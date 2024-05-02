package com.ekenya.chamakyc.configs;

import io.micrometer.core.lang.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.adapter.HttpWebHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;

import java.util.List;
import java.util.Locale;

@Slf4j
@Configuration
public class LocaleConfig extends AcceptHeaderLocaleContextResolver {

    /**
     * Updates the locale to the one provided in the header.
     *
     * @param applicationContext this is provided by Spring
     * @return the HttpHandler
     */
    @Bean
    HttpHandler acceptLanguageHeaderHttpHandler(ApplicationContext applicationContext) {
        HttpHandler delegate = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
        return new HttpWebHandlerAdapter((HttpWebHandlerAdapter) delegate) {
            @Override
            @NonNull
            protected ServerWebExchange createExchange(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
                ServerWebExchange serverWebExchange = super.createExchange(request, response);
                List<String> languages = request.getHeaders().get(HttpHeaders.ACCEPT_LANGUAGE);
                if (languages != null && languages.stream().anyMatch(locale -> locale.equals("sw"))) {
                    LocaleContextHolder.setDefaultLocale(new Locale("sw"));
                } else {
                    LocaleContextHolder.setDefaultLocale(Locale.ENGLISH);
                }
                return serverWebExchange;
            }
        };
    }

    /**
     * Provide the messages binding base name.
     * Uses .properties, Yaml not supported.
     *
     * @return the resource bundle message source
     */
    @Bean
    public ResourceBundleMessageSource messageSource() {
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setDefaultLocale(Locale.ENGLISH);
        return source;
    }

}
