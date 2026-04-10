package com.gateway.gateway.config;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import feign.Request;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ForwardRequestFactory {

	private static final List<String> EXCLUDED_REQUEST_HEADERS = List.of("host", "content-length");

	public Request create(HttpServletRequest request, URI targetUri, byte[] body) {
		return Request.create(
			Request.HttpMethod.valueOf(request.getMethod()),
			targetUri.toString(),
			extractHeaders(request),
			body == null || body.length == 0 ? null : body,
			StandardCharsets.UTF_8,
			null
		);
	}

	private Map<String, Collection<String>> extractHeaders(HttpServletRequest request) {
		Map<String, Collection<String>> headers = new LinkedHashMap<>();
		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames != null && headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if (shouldSkipHeader(headerName)) {
				continue;
			}

			headers.put(headerName, Collections.list(request.getHeaders(headerName)));
		}

		return headers;
	}

	private boolean shouldSkipHeader(String headerName) {
		return EXCLUDED_REQUEST_HEADERS.stream().anyMatch(excluded -> excluded.equalsIgnoreCase(headerName));
	}
}
