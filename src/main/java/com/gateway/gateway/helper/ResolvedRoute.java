package com.gateway.gateway.helper;

import java.net.URI;

public record ResolvedRoute(String pathPrefix, String targetBaseUrl, URI targetUri) {
}
