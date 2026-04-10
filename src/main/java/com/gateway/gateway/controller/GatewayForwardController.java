package com.gateway.gateway.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.gateway.service.GatewayForwardService;

@RestController
@Slf4j
public class GatewayForwardController {

	private final GatewayForwardService gatewayForwardService;

	public GatewayForwardController(GatewayForwardService gatewayForwardService) {
		this.gatewayForwardService = gatewayForwardService;
	}

	@RequestMapping(
		value = {"/", "/{*path}"},
		method = {
			RequestMethod.GET,
			RequestMethod.POST,
			RequestMethod.PUT,
			RequestMethod.PATCH,
			RequestMethod.DELETE,
			RequestMethod.HEAD,
			RequestMethod.OPTIONS,
			RequestMethod.TRACE
		}
	)
	public ResponseEntity<String> forward(
		HttpServletRequest request,
		@RequestBody(required = false) byte[] body
	) {
		log.info(
			"Incoming request method={} path={} query={} contentType={} bodyBytes={}",
			request.getMethod(),
			request.getRequestURI(),
			request.getQueryString(),
			request.getContentType(),
			body == null ? 0 : body.length
		);
		return gatewayForwardService.forward(request, body);
	}
}
