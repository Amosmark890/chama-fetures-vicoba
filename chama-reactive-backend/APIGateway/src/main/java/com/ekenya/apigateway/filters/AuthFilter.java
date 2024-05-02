package com.ekenya.apigateway.filters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class AuthFilter implements WebFilter {

    private WebClient webClient;

    @Value("${auth.server.url}")
    private String authServerBaseUrl;

    @Value("${auth.server.basic}")
    private String authServerCredentials;

    private static final String BEARER = "Bearer ";

    private final Gson gson;

    private static final Predicate<String> matchBearerLength = authValue -> authValue.equals(BEARER);

    private static final UnaryOperator<String> isolateBearerValue = authValue -> authValue.substring(BEARER.length());

    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(authServerBaseUrl)
                .defaultHeaders(httpHeaders -> httpHeaders.add(HttpHeaders.AUTHORIZATION, authServerCredentials))
                .build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.info("********************** Trying to validate token **********************");
        String bearerToken = getTokenFromRequest(exchange);

        if (bearerToken.isBlank() || !matchBearerLength.test(bearerToken.substring(0, 7))) {
            log.info("********************** No Bearer Token Found => Unsecured Endpoint **********************");
            return chain.filter(exchange);
        }

        String token = isolateBearerValue.apply(bearerToken);

        return webClient.post()
                .uri("/oauth/validate?token=" + token)
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> gson.fromJson(json, JsonObject.class))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(jsonObject -> {
                    log.info("********************** Token Validation Response::: {}", gson.toJson(jsonObject));

                    if (jsonObject.get("status").getAsInt() != 200) {
                        Map<String, Object> response = Map.of("status", 401, "message", "Session expired");
                        String gsonString = gson.toJson(response);
                        DataBuffer bodyDataBuffer = exchange.getResponse().bufferFactory().wrap(gsonString.getBytes());
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        exchange.getResponse()
                                .writeWith(Mono.just(bodyDataBuffer))
                                .subscribe();
                        return exchange.getResponse().setComplete();
                    }

                    log.info("********************** Request is validated successfully **********************");
                    return chain.filter(exchange);
                });
    }

    public static String getTokenFromRequest(ServerWebExchange webExchange) {
        String token = webExchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        return StringUtils.hasLength(token) ? token : "";
    }
}
