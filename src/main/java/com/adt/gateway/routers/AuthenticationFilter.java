package com.adt.gateway.routers;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@RefreshScope
@Component
public class AuthenticationFilter implements GatewayFilter {

	@Autowired
	private RouterValidator routerValidator;

	@Autowired
	private UtilityHandler utilClass;
	
	private static final Logger LOGGER = LogManager.getLogger(AuthenticationFilter.class);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		LOGGER.info("Enter in filer method");
		ServerHttpRequest request = exchange.getRequest();
		if (routerValidator.isSecured.test((ServerHttpRequest) request)) {
			if (this.isAuthMissing(request))
				return this.onError(exchange, "Authorization header is missing in request", HttpStatus.UNAUTHORIZED);
			final String token = this.getAuthHeader(request);
			if (utilClass.isInvalid(token))
				return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
			String uri = request.getPath().toString();
			String methodType = request.getMethod().toString();
			if(utilClass.isApiValid(uri,methodType,token)){
				return this.onError(exchange, "Authorization header is invalid", HttpStatus.UNAUTHORIZED);
			}
			this.populateRequestWithHeaders(exchange, token);
		}
		return chain.filter(exchange);
	}

	/* PRIVATE */
	private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(httpStatus);
		return response.setComplete();
	}

	private String getAuthHeader(ServerHttpRequest request) {
		return request.getHeaders().getOrEmpty("Authorization").get(0);
	}

	private boolean isAuthMissing(ServerHttpRequest request) {
		return !request.getHeaders().containsKey("Authorization");
	}

	private void populateRequestWithHeaders(ServerWebExchange exchange, String token) {
		LOGGER.info("Token verify successfully");
		exchange.getRequest().mutate().build();
	}

}
