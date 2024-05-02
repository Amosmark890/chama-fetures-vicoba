package com.ekenya.apigateway.config;

import com.ekenya.apigateway.utils.MaskUtils;
import com.google.gson.Gson;
//import io.eclectics.rnd.maskutility.util.MaskUtil;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Configuration
public class GatewayConfig {

    private static final Duration TIMEOUT = Duration.ofMinutes(5);

    private static final String DEFAULT_FALLBACK = "/defaultFallback";
    public static final String REQUEST_RESPONSE_LOG_TOPIC = "request-response-log";

//    private final MaskUtil maskUtil;

    private final StreamBridge streamBridge;

    private final Gson gson = new Gson();

    public GatewayConfig( StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Bean
    public RouteLocator myRoutes(RouteLocatorBuilder routeLocatorBuilder) {
        return routeLocatorBuilder.routes()
                .route(p -> p.path("/oauth/**")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-authServer/"))
                .route(p -> p.path("/api/v2/kyc/**")
                        .filters(this::logRequestAndResponse)
                        .uri("lb://chama-kyc/"))
                .route(p -> p.path("/api/v2/payment/**")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-payments/"))
                .route(p -> p.path("/portal/payments/**")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-payments/"))
                .route(p -> p.path("/api/v2/poll/**")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-polls/"))
                .route(p -> p.path("/portal/kyc/**")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-kyc/"))
                .route(p -> p.path("/kyc/swagger-ui")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-kyc/swagger-ui/"))
                .route(p -> p.path("/payments/swagger-ui/")
                        .filters(f -> {
                            f.circuitBreaker(c -> c.setFallbackUri(DEFAULT_FALLBACK));
                            return f;
                        })
                        .uri("lb://chama-payments/swagger-ui/"))
                .build();
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .slidingWindowSize(20)
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .failureRateThreshold(50)
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(TIMEOUT)
                        .build())
                .build()
        );
    }

    private UriSpec logRequestAndResponse(GatewayFilterSpec f) {
        AtomicReference<Map<String, String>> requestAndResponse = new AtomicReference<>(new HashMap<>());

        return f.modifyRequestBody(String.class, String.class, MediaType.APPLICATION_JSON_VALUE, (exchange, body) -> logRequestBody(requestAndResponse, body))
                .modifyResponseBody(String.class, String.class, MediaType.TEXT_PLAIN_VALUE, (exchange, body) -> logResponseBody(requestAndResponse, exchange, body));
    }

    private Mono<String> logResponseBody(AtomicReference<Map<String, String>> requestAndResponse, ServerWebExchange exchange, String responseBody) {
        Map<String, String> hashMap = requestAndResponse.get();
        if (!exchange.getResponse().getStatusCode().is2xxSuccessful()) {
            asyncLog(hashMap);
            return Mono.justOrEmpty(responseBody);
        }

        log.info("Logging::: {}", responseBody);
//        hashMap.put("response", MaskUtils.maskObject(responseBody));
        hashMap.put("response", responseBody);
        asyncLog(hashMap);
        return Mono.justOrEmpty(responseBody);
    }

    private Mono<String> logRequestBody(AtomicReference<Map<String, String>> requestAndResponseBody, String requestBody) {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("request", requestBody);
//        hashMap.put("request", MaskUtils.maskObject(requestBody));
        requestAndResponseBody.set(hashMap);
        return Mono.justOrEmpty(requestBody);
    }

    @Async
    public void asyncLog(Map<String, String> logBody) {
        streamBridge.send(REQUEST_RESPONSE_LOG_TOPIC, gson.toJson(logBody));
    }
}
