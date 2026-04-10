package com.gateway.gateway.mapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import feign.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ForwardResponseMapper {

	private static final List<String> EXCLUDED_RESPONSE_HEADERS = List.of("content-length", "transfer-encoding");

	public ResponseEntity<String> toResponseEntity(Response response, String responseBody) {
		return ResponseEntity
			.status(response.status())
			.headers(extractHeaders(response))
			.body(responseBody);
	}

	public String readBody(Response response) throws IOException {
		if (response.body() == null) {
			return "";
		}
		return new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}

	private HttpHeaders extractHeaders(Response response) {
		HttpHeaders headers = new HttpHeaders();
		response.headers().forEach((name, values) -> {
			if (!shouldSkipHeader(name)) {
				headers.put(name, values.stream().toList());
			}
		});
		return headers;
	}

	private boolean shouldSkipHeader(String headerName) {
		return EXCLUDED_RESPONSE_HEADERS.stream().anyMatch(excluded -> excluded.equalsIgnoreCase(headerName));
	}
}
