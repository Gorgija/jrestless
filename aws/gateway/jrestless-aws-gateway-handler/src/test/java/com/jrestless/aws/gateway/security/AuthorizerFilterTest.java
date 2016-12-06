package com.jrestless.aws.gateway.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.jrestless.aws.gateway.dpi.GatewayRequestContextFactory;
import com.jrestless.aws.gateway.io.GatewayRequest;
import com.jrestless.aws.gateway.io.GatewayRequestContext;

public abstract class AuthorizerFilterTest {

	abstract AuthorizerFilter createCognitoAuthorizerFilter();

	protected final ContainerRequestContext createRequest(Map<String, Object> authorizerData) {
		GatewayRequestContext gatewayRequestContext = mock(GatewayRequestContext.class);
		when(gatewayRequestContext.getAuthorizer()).thenReturn(authorizerData);

		GatewayRequest gatewayRequest = mock(GatewayRequest.class);
		when(gatewayRequest.getRequestContext()).thenReturn(gatewayRequestContext);

		ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
		when(containerRequestContext.getProperty(GatewayRequestContextFactory.PROPERTY_NAME))
				.thenReturn(gatewayRequest);

		return containerRequestContext;
	}

	@Test
	public void noGatewayRequestSet_ShouldNotSetSecurityContext() {
		ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
		filterAndVerifyNoSecurityContextSet(containerRequestContext);
	}

	@Test
	public void noGatewayRequestContextSet_ShouldNotSetSecurityContext() {
		GatewayRequest gatewayRequest = mock(GatewayRequest.class);

		ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
		when(containerRequestContext.getProperty(GatewayRequestContextFactory.PROPERTY_NAME))
				.thenReturn(gatewayRequest);

		filterAndVerifyNoSecurityContextSet(containerRequestContext);
	}

	@Test
	public void noAuthorizerDataSet_ShouldNotSetSecurityContext() {
		GatewayRequestContext gatewayRequestContext = mock(GatewayRequestContext.class);
		when(gatewayRequestContext.getAuthorizer()).thenReturn(null);

		GatewayRequest gatewayRequest = mock(GatewayRequest.class);
		when(gatewayRequest.getRequestContext()).thenReturn(gatewayRequestContext);

		ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
		when(containerRequestContext.getProperty(GatewayRequestContextFactory.PROPERTY_NAME))
				.thenReturn(gatewayRequest);

		filterAndVerifyNoSecurityContextSet(containerRequestContext);
	}

	@Test
	public void emptyAuthorizerDataSet_ShouldNotSetSecurityContext() {
		GatewayRequestContext gatewayRequestContext = mock(GatewayRequestContext.class);
		when(gatewayRequestContext.getAuthorizer()).thenReturn(Collections.emptyMap());

		GatewayRequest gatewayRequest = mock(GatewayRequest.class);
		when(gatewayRequest.getRequestContext()).thenReturn(gatewayRequestContext);

		ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
		when(containerRequestContext.getProperty(GatewayRequestContextFactory.PROPERTY_NAME))
				.thenReturn(gatewayRequest);

		filterAndVerifyNoSecurityContextSet(containerRequestContext);
	}

	protected final SecurityContext filterAndReturnSetSecurityContext(Map<String, Object> authorizerData) {
		return filterAndReturnSetSecurityContext(createRequest(authorizerData));
	}

	protected final SecurityContext filterAndReturnSetSecurityContext(ContainerRequestContext containerRequestContext) {
		AuthorizerFilter filter = createCognitoAuthorizerFilter();
		ArgumentCaptor<SecurityContext> securityContextCapture = ArgumentCaptor.forClass(SecurityContext.class);
		try {
			filter.filter(containerRequestContext);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		verify(containerRequestContext).setSecurityContext(securityContextCapture.capture());
		return securityContextCapture.getValue();
	}

	protected final void filterAndVerifyNoSecurityContextSet(Map<String, Object> authorizerData) {
		filterAndVerifyNoSecurityContextSet(createRequest(authorizerData));
	}

	protected final void filterAndVerifyNoSecurityContextSet(ContainerRequestContext containerRequestContext) {
		AuthorizerFilter filter = createCognitoAuthorizerFilter();
		try {
			filter.filter(containerRequestContext);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		verify(containerRequestContext, times(0)).setSecurityContext(any());
	}
}
