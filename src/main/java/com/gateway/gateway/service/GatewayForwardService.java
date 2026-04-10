package com.gateway.gateway.service;

import java.io.IOException;

import com.gateway.gateway.config.ForwardRequestFactory;
import com.gateway.gateway.helper.ForwardLoggingHelper;
import com.gateway.gateway.helper.ResolvedRoute;
import com.gateway.gateway.helper.RouteResolver;
import com.gateway.gateway.mapper.ForwardResponseMapper;
import feign.Client;
import feign.Request;
import feign.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class GatewayForwardService {

	private final Client feignClient;
	private final RouteResolver routeResolver;
	private final ForwardRequestFactory forwardRequestFactory;
	private final ForwardResponseMapper forwardResponseMapper;
	private final ForwardLoggingHelper forwardLoggingHelper;

	public GatewayForwardService(
		Client feignClient,
		RouteResolver routeResolver,
		ForwardRequestFactory forwardRequestFactory,
		ForwardResponseMapper forwardResponseMapper,
		ForwardLoggingHelper forwardLoggingHelper
	) {
		this.feignClient = feignClient;
		this.routeResolver = routeResolver;
		this.forwardRequestFactory = forwardRequestFactory;
		this.forwardResponseMapper = forwardResponseMapper;
		this.forwardLoggingHelper = forwardLoggingHelper;
	}

	public ResponseEntity<String> forward(HttpServletRequest request, byte[] body) {
		ResolvedRoute route = routeResolver.resolve(request);
		logForwardingRequest(request, body, route);

		Request feignRequest = forwardRequestFactory.create(request, route.targetUri(), body);

		try (Response response = feignClient.execute(feignRequest, new Request.Options())) {
			String responseBody = forwardResponseMapper.readBody(response);
			logForwardingResponse(response, route, responseBody);
			return forwardResponseMapper.toResponseEntity(response, responseBody);
		} catch (IOException ex) {
			log.error("Failed forwarding request to target={}", route.targetUri(), ex);
			throw new ResponseStatusException(
				HttpStatus.BAD_GATEWAY,
				"Failed to forward request to " + route.targetBaseUrl(),
				ex
			);
		}
	}

	private void logForwardingRequest(HttpServletRequest request, byte[] body, ResolvedRoute route) {
		log.info(
			"Forwarding request method={} path={} query={} matchedPrefix={} target={} bodyBytes={}",
			request.getMethod(),
			request.getRequestURI(),
			request.getQueryString(),
			route.pathPrefix(),
			route.targetUri(),
			body == null ? 0 : body.length
		);
		log.info("Forwarding request curl={}", forwardLoggingHelper.toCurlCommand(request, route.targetUri(), body));
	}

	private void logForwardingResponse(Response response, ResolvedRoute route, String responseBody) {
		log.info(
			"Received response status={} target={} bodyLength={} responseJson={}",
			response.status(),
			route.targetUri(),
			responseBody.length(),
			responseBody
		);
	}
}
