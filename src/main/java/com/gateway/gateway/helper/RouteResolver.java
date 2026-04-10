package com.gateway.gateway.helper;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.gateway.gateway.config.GatewayRoutingProperties;

@Component
public class RouteResolver {

	private final GatewayRoutingProperties routingProperties;

	public RouteResolver(GatewayRoutingProperties routingProperties) {
		this.routingProperties = routingProperties;
	}

	public ResolvedRoute resolve(HttpServletRequest request) {
		GatewayRoutingProperties.Route route = routingProperties.getRoutes().stream()
			.filter(candidate -> matchesPrefix(candidate.getPathPrefix(), request.getRequestURI()))
			.sorted((left, right) -> Integer.compare(
				normalizePrefix(right.getPathPrefix()).length(),
				normalizePrefix(left.getPathPrefix()).length()
			))
			.findFirst()
			.orElseGet(this::defaultRoute);

		return new ResolvedRoute(
			normalizePrefix(route.getPathPrefix()),
			route.getTargetBaseUrl(),
			buildTargetUri(route.getTargetBaseUrl(), request.getRequestURI(), request.getQueryString())
		);
	}

	private GatewayRoutingProperties.Route defaultRoute() {
		if (routingProperties.getDefaultTargetBaseUrl() == null || routingProperties.getDefaultTargetBaseUrl().isBlank()) {
			throw new ResponseStatusException(
				HttpStatus.NOT_FOUND,
				"No gateway route configured and no default target defined"
			);
		}

		GatewayRoutingProperties.Route route = new GatewayRoutingProperties.Route();
		route.setPathPrefix("/");
		route.setTargetBaseUrl(routingProperties.getDefaultTargetBaseUrl());
		return route;
	}

	private URI buildTargetUri(String targetBaseUrl, String requestPath, String queryString) {
		UriComponentsBuilder builder = UriComponentsBuilder
			.fromUriString(targetBaseUrl)
			.path(requestPath);

		if (queryString != null && !queryString.isBlank()) {
			builder.query(queryString);
		}

		return builder.build(true).toUri();
	}

	private boolean matchesPrefix(String configuredPrefix, String requestPath) {
		String prefix = normalizePrefix(configuredPrefix);
		return requestPath.equals(prefix) || requestPath.startsWith(prefix + "/");
	}

	private String normalizePrefix(String prefix) {
		if (prefix == null || prefix.isBlank()) {
			return "/";
		}
		String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
		return normalized.endsWith("/") && normalized.length() > 1
			? normalized.substring(0, normalized.length() - 1)
			: normalized;
	}
}
