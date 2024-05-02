package com.ekenya.chamakyc.configs;

import com.ekenya.chamakyc.dao.config.AuditLog;
import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AuditTrailInterceptor implements WebFilter {

    private final Gson gson;
    private final PublishingService publishingService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(exchange)
                .flatMap(ex -> {
                    LocalDateTime requestTime = LocalDateTime.now();
                    List<String> agentHeaders = exchange.getRequest().getHeaders().get("user-agent");
                    String queryParams = !exchange.getRequest().getQueryParams().isEmpty() ? gson.toJson(ex.getRequest().getQueryParams()) : null;
                    AuditLog auditLog = new AuditLog();

                    auditLog.setRequestTime(requestTime);
                    auditLog.setRemoteAddress(ex.getRequest().getRemoteAddress().toString());
                    auditLog.setMethodType(ex.getRequest().getMethod().name());
                    auditLog.setUserAgent(agentHeaders != null ? agentHeaders.get(0) : null);
                    auditLog.setQueryParams(queryParams);

                    ServerHttpRequestDecorator loggingServerHttpRequestDecorator = new ServerHttpRequestDecorator(ex.getRequest()) {
                        String requestBody = "";

                        @Override
                        public Flux<DataBuffer> getBody() {
                            return super.getBody().doOnNext(dataBuffer -> {
                                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                                    Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                                    requestBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                                    auditLog.setUri(exchange.getRequest().getURI().toURL().toString());
                                    auditLog.setRequestBody(requestBody);
                                } catch (IOException e) {
                                    log.error(e.getLocalizedMessage(), e);
                                }
                            });
                        }
                    };
                    ServerHttpResponseDecorator loggingServerHttpResponseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
                        String responseBody = "";

                        @Override
                        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                            Mono<DataBuffer> buffer = Mono.from(body);
                            return super.writeWith(buffer.publishOn(Schedulers.boundedElastic()).doOnNext(dataBuffer -> {
                                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                                    Channels.newChannel(byteArrayOutputStream).write(dataBuffer.asByteBuffer().asReadOnlyBuffer());
                                    responseBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
                                    auditLog.setResponseBody(responseBody);

                                    if (auditLog.getUri() != null && !auditLog.getUri().contains("auth"))
                                        publishingService.publishAuditLog(auditLog);

                                } catch (Exception ignored) {
                                    log.error(ignored.getLocalizedMessage(), ignored);
                                }
                            }));
                        }
                    };
                    return chain.filter(exchange.mutate().request(loggingServerHttpRequestDecorator).response(loggingServerHttpResponseDecorator).build());
                });
    }
}