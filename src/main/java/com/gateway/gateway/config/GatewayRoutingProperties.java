package com.gateway.gateway.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway")
public class GatewayRoutingProperties {

	private String defaultTargetBaseUrl;
	private List<Route> routes = new ArrayList<>();

	public String getDefaultTargetBaseUrl() {
		return defaultTargetBaseUrl;
	}

	public void setDefaultTargetBaseUrl(String defaultTargetBaseUrl) {
		this.defaultTargetBaseUrl = defaultTargetBaseUrl;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	public void setRoutes(List<Route> routes) {
		this.routes = routes;
	}

	public static class Route {

		private String pathPrefix;
		private String targetBaseUrl;

		public String getPathPrefix() {
			return pathPrefix;
		}

		public void setPathPrefix(String pathPrefix) {
			this.pathPrefix = pathPrefix;
		}

		public String getTargetBaseUrl() {
			return targetBaseUrl;
		}

		public void setTargetBaseUrl(String targetBaseUrl) {
			this.targetBaseUrl = targetBaseUrl;
		}
	}
}
