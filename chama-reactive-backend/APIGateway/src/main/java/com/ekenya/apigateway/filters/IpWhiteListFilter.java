package com.ekenya.apigateway.filters;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
//@Configuration
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IpWhiteListFilter implements GlobalFilter {
    @Value("${ip.whitelist}")
    private List<String> ipWhiteListRange;
    private final Gson gson;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String IP_RANGE = exchange.getRequest().getURI().getScheme() + "://" + exchange.getRequest().getURI().getHost();
        log.info("IP RANGE {}", IP_RANGE);
        if (!ipWhiteListRange.contains(IP_RANGE)) {
            log.info("Un-whitelisted ip address {}", IP_RANGE);
            Map<String, Object> response = new HashMap<>();
            response.put("status", 400);
            response.put("message", "Unauthorized Host Access");
            String gsonString = gson.toJson(response);
            DataBuffer bodyDataBuffer = exchange.getResponse().bufferFactory().wrap(gsonString.getBytes());
            exchange.getResponse().setStatusCode(HttpStatus.OK);
            exchange.getResponse().writeWith(Mono.just(bodyDataBuffer))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
