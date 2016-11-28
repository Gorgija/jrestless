package com.jrestless.aws.service.handler;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.lambda.runtime.Context;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.jrestless.aws.service.io.DefaultServiceRequest;
import com.jrestless.aws.service.io.DefaultServiceResponse;
import com.jrestless.aws.service.io.ServiceRequest;
import com.jrestless.core.container.JRestlessHandlerContainer;
import com.jrestless.core.container.handler.SimpleRequestHandler.SimpleResponseWriter;
import com.jrestless.core.container.io.JRestlessContainerRequest;

public class ServiceRequestHandlerTest {


	private JRestlessHandlerContainer<JRestlessContainerRequest> container;
	private ServiceRequestHandler gatewayHandler;

	@SuppressWarnings("unchecked")
	@Before
	public void setup() {
		container = mock(JRestlessHandlerContainer.class);
		gatewayHandler = spy(new ServiceRequestHandlerImpl());
		gatewayHandler.init(container);
		gatewayHandler.start();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void delegateRequest_ValidRequestGiven_ShouldRegisterContextsOnRequest() {
		Context context = mock(Context.class);
		DefaultServiceRequest request = new DefaultServiceRequest(null, new HashMap<>(), URI.create("/"), "GET");
		ServiceRequestAndLambdaContext reqAndContext = new ServiceRequestAndLambdaContext(request, context);
		DefaultServiceResponse response = new DefaultServiceResponse("testBody", new HashMap<>(), 200, "");
		SimpleResponseWriter<DefaultServiceResponse> responseWriter = mock(SimpleResponseWriter.class);
		when(responseWriter.getResponse()).thenReturn(response);
		doReturn(responseWriter).when(gatewayHandler).createResponseWriter();
		ArgumentCaptor<Consumer> containerEnhancerCapure = ArgumentCaptor.forClass(Consumer.class);
		gatewayHandler.delegateRequest(reqAndContext);
		verify(container).handleRequest(any(), eq(responseWriter), any(), containerEnhancerCapure.capture());

		ContainerRequest containerRequest = mock(ContainerRequest.class);
		containerEnhancerCapure.getValue().accept(containerRequest);
		verify(containerRequest).setProperty("awsLambdaContext", context);
		verify(containerRequest).setProperty("awsServiceRequest", request);
	}

	@Test
	public void createContainerRequest_NoBodyGiven_ShouldUseEmptyBaos() {
		ServiceRequestAndLambdaContext request = createMinimalRequest();
		JRestlessContainerRequest containerRequest = gatewayHandler.createContainerRequest(request);
		InputStream is = containerRequest.getEntityStream();
		assertEquals(ByteArrayInputStream.class, is.getClass());
		assertArrayEquals(new byte[0], toBytes((ByteArrayInputStream) is));
	}

	@Test
	public void createContainerRequest_BodyGiven_ShouldUseBody() {
		ServiceRequestAndLambdaContext request = createMinimalRequest();
		((DefaultServiceRequest) request.getServiceRequest()).setBody("abc");
		JRestlessContainerRequest containerRequest = gatewayHandler.createContainerRequest(request);
		InputStream is = containerRequest.getEntityStream();
		assertEquals(ByteArrayInputStream.class, is.getClass());
		assertArrayEquals("abc".getBytes(), toBytes((ByteArrayInputStream) is));
	}

	@Test
	public void createContainerRequest_HttpMethodGiven_ShouldUseHttpMethod() {
		ServiceRequestAndLambdaContext request = createMinimalRequest();
		((DefaultServiceRequest) request.getServiceRequest()).setHttpMethod("X");
		JRestlessContainerRequest containerRequest = gatewayHandler.createContainerRequest(request);
		assertEquals("X", containerRequest.getHttpMethod());
	}

	@Test
	public void createContainerRequest_PathGiven_ShouldUsePath() {
		ServiceRequestAndLambdaContext request = createMinimalRequest();
		((DefaultServiceRequest) request.getServiceRequest()).setRequestUri(URI.create("/a?b=c&d=e"));
		JRestlessContainerRequest containerRequest = gatewayHandler.createContainerRequest(request);
		assertEquals(URI.create("/a?b=c&d=e"), containerRequest.getRequestUri());
	}

	@Test
	public void createContainerRequest_HeadersGiven_ShouldUseHeaders() {
		ServiceRequestAndLambdaContext request = createMinimalRequest();
		Map<String, List<String>> headers = ImmutableMap.of("0", emptyList(), "1", singletonList("a"), "2", ImmutableList.of("b", "c"));
		((DefaultServiceRequest) request.getServiceRequest()).setHeaders(headers);
		JRestlessContainerRequest containerRequest = gatewayHandler.createContainerRequest(request);
		assertEquals(headers, containerRequest.getHeaders());
	}

	private ServiceRequestAndLambdaContext createMinimalRequest() {
		ServiceRequest request = new DefaultServiceRequest(null, new HashMap<>(), URI.create("/"), "GET");
		return new ServiceRequestAndLambdaContext(request, null);
	}

	public static byte[] toBytes(ByteArrayInputStream bais) {
		byte[] array = new byte[bais.available()];
		try {
			bais.read(array);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return array;
	}
	private static class ServiceRequestHandlerImpl extends ServiceRequestHandler {
	}
}
