package com.gateway.gateway.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.gateway.gateway.config.ForwardRequestFactory;
import com.gateway.gateway.helper.ForwardLoggingHelper;
import com.gateway.gateway.helper.RouteResolver;
import com.gateway.gateway.mapper.ForwardResponseMapper;
import feign.Client;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.gateway.gateway.config.GatewayRoutingProperties;

class GatewayForwardServiceTest {

	@Test
	void forwardsPathQueryBodyAndAuthorizationHeader() throws Exception {
		Client feignClient = mock(Client.class);
		ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
		when(feignClient.execute(requestCaptor.capture(), any(Request.Options.class)))
			.thenReturn(
				Response.builder()
					.status(200)
					.reason("OK")
					.request(Request.create(
						Request.HttpMethod.POST,
						"http://190.180.0.111:9099/limit-validator/mfund?id=1",
						Map.of(),
						null,
						StandardCharsets.UTF_8,
						null
					))
					.headers(Map.of("Content-Type", List.of("application/json")))
					.body("{\"code\":\"00\"}", StandardCharsets.UTF_8)
					.build()
			);

		GatewayForwardService service = newService(feignClient, routingProperties(
			route("/limit-validator", "http://localhost:8080"),
			route("/limit-validator/mfund", "http://190.180.0.111:9099")
		));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/limit-validator/mfund");
		request.setQueryString("id=1");
		request.addHeader("Authorization", "Bearer abc");
		request.addHeader("Content-Type", "application/json");
		request.addHeader("X-Correlation-Id", "req-123");
		byte[] body = "{\"name\":\"john\"}".getBytes(StandardCharsets.UTF_8);

		ResponseEntity<String> response = service.forward(request, body);
		Request forwardedRequest = requestCaptor.getValue();

		assertEquals("http://190.180.0.111:9099/limit-validator/mfund?id=1", forwardedRequest.url());
		assertEquals(List.of("Bearer abc"), forwardedRequest.headers().get("Authorization").stream().toList());
		assertEquals(List.of("application/json"), forwardedRequest.headers().get("Content-Type").stream().toList());
		assertEquals(List.of("req-123"), forwardedRequest.headers().get("X-Correlation-Id").stream().toList());
		assertFalse(forwardedRequest.headers().containsKey("Host"));
		assertFalse(forwardedRequest.headers().containsKey("Content-Length"));
		assertArrayEquals(body, forwardedRequest.body());
		assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
		assertEquals("{\"code\":\"00\"}", response.getBody());
		assertEquals("application/json", response.getHeaders().getFirst("Content-Type"));
	}

	@Test
	void forwardsRequestToLessSpecificRouteWhenNoMoreSpecificMatchExists() throws Exception {
		Client feignClient = mock(Client.class);
		ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
		when(feignClient.execute(requestCaptor.capture(), any(Request.Options.class)))
			.thenReturn(
				Response.builder()
					.status(200)
					.reason("OK")
					.request(Request.create(
						Request.HttpMethod.GET,
						"http://localhost:8080/limit-validator/check",
						Map.of(),
						null,
						StandardCharsets.UTF_8,
						null
					))
					.headers(Map.of())
					.body("ok", StandardCharsets.UTF_8)
					.build()
			);

		GatewayForwardService service = newService(feignClient, routingProperties(
			route("/limit-validator", "http://localhost:8080"),
			route("/master-data-hub", "http://190.180.0.111:9099")
		));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/limit-validator/check");
		request.addHeader("Authorization", "Bearer def");

		ResponseEntity<String> response = service.forward(request, null);

		assertEquals("http://localhost:8080/limit-validator/check", requestCaptor.getValue().url());
		assertTrue(requestCaptor.getValue().headers().containsKey("Authorization"));
		assertEquals("ok", response.getBody());
	}

	@Test
	void forwardsToDefaultRouteWhenPathIsNotConfigured() throws Exception {
		Client feignClient = mock(Client.class);
		ArgumentCaptor<Request> requestCaptor = ArgumentCaptor.forClass(Request.class);
		when(feignClient.execute(requestCaptor.capture(), any(Request.Options.class)))
			.thenReturn(
				Response.builder()
					.status(200)
					.reason("OK")
					.request(Request.create(
						Request.HttpMethod.GET,
						"http://190.180.0.111:9099/unregistered/path?x=1",
						Map.of(),
						null,
						StandardCharsets.UTF_8,
						null
					))
					.headers(Map.of())
					.body("fallback", StandardCharsets.UTF_8)
					.build()
			);

		GatewayForwardService service = newService(feignClient, routingProperties(
			"http://190.180.0.111:9099",
			route("/limit-validator", "http://localhost:8080")
		));

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unregistered/path");
		request.setQueryString("x=1");
		request.addHeader("Authorization", "Bearer xyz");

		ResponseEntity<String> response = service.forward(request, null);

		assertEquals("http://190.180.0.111:9099/unregistered/path?x=1", requestCaptor.getValue().url());
		assertEquals(List.of("Bearer xyz"), requestCaptor.getValue().headers().get("Authorization").stream().toList());
		assertEquals("fallback", response.getBody());
	}

	private GatewayRoutingProperties routingProperties(GatewayRoutingProperties.Route... routes) {
		return routingProperties(null, routes);
	}

	private GatewayRoutingProperties routingProperties(
		String defaultTargetBaseUrl,
		GatewayRoutingProperties.Route... routes
	) {
		GatewayRoutingProperties properties = new GatewayRoutingProperties();
		properties.setDefaultTargetBaseUrl(defaultTargetBaseUrl);
		properties.setRoutes(List.of(routes));
		return properties;
	}

	private GatewayRoutingProperties.Route route(String pathPrefix, String targetBaseUrl) {
		GatewayRoutingProperties.Route route = new GatewayRoutingProperties.Route();
		route.setPathPrefix(pathPrefix);
		route.setTargetBaseUrl(targetBaseUrl);
		return route;
	}

	private GatewayForwardService newService(Client feignClient, GatewayRoutingProperties routingProperties) {
		return new GatewayForwardService(
			feignClient,
			new RouteResolver(routingProperties),
			new ForwardRequestFactory(),
			new ForwardResponseMapper(),
			new ForwardLoggingHelper()
		);
	}
}
