package com.gateway.gateway.helper;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class ForwardLoggingHelper {

	public String toCurlCommand(HttpServletRequest request, URI targetUri, byte[] body) {
		StringBuilder curl = new StringBuilder();
		curl.append("curl -X ").append(request.getMethod());
		curl.append(" \"").append(targetUri).append("\"");

		if (body != null && body.length > 0) {
			curl.append(" --data '")
				.append(escapeForSingleQuotes(new String(body, StandardCharsets.UTF_8)))
				.append("'");
		}

		return curl.toString();
	}

	private String escapeForSingleQuotes(String value) {
		return value.replace("'", "'\"'\"'");
	}
}
