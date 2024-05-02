package com.ekenya.apigateway.filters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.bucket4j.*;
import io.github.bucket4j.grid.GridBucketState;
import io.github.bucket4j.grid.ProxyManager;
import io.github.bucket4j.grid.jcache.JCache;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import javax.cache.Cache;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class IpThrottlingFilter implements WebFilter {
    // cache for storing token buckets
    private final Cache<String, GridBucketState> cache;
    private final ProxyManager<String> buckets;

    public IpThrottlingFilter(Cache<String, GridBucketState> cache) {
        this.cache = cache;
        // init bucket registry
        buckets = Bucket4j.extension(JCache.class).proxyManagerForCache(this.cache);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        BucketConfiguration bucketConfiguration = Bucket4j.configurationBuilder()
                .addLimit(Bandwidth.simple(12, Duration.ofMinutes(1))) // sets a bucket of 10 tokens for a duration of 1
                // minute
                .build();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Configuration that will be called on first interaction with proxy if bucket
        // was not saved previously
        Supplier<BucketConfiguration> configurationLazySupplier = () -> bucketConfiguration;

        // acquire cheap proxy to bucket
        Bucket bucket = buckets.getProxy(request.getRemoteAddress().getHostString(), configurationLazySupplier);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // the limit is not consumed
            response.getHeaders().add("X-Rate-Limit-Remaining", " " + probe.getRemainingTokens());
            exchange.mutate().response(response).build();
            chain.filter(exchange);
        } else {
            Gson gson = new Gson();
            Map<String, String> responseData = new LinkedHashMap<>();
            responseData.put("error", "too many requests");
            responseData.put("error description", "please wait");
            Type gsonType = new TypeToken<LinkedHashMap<String, String>>() {
            }.getType();
            String gsonString = gson.toJson(responseData, gsonType);
            DataBuffer bodyDataBuffer = response.bufferFactory().wrap(gsonString.getBytes());
            response.setStatusCode(HttpStatus.OK);
            response.writeWith(Mono.just(bodyDataBuffer));
            exchange.mutate().response(response).build();

        }
        return chain.filter(exchange);
    }
}
