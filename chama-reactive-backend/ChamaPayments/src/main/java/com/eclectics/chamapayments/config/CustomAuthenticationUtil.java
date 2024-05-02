package com.eclectics.chamapayments.config;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

@UtilityClass
public class CustomAuthenticationUtil {

    public Mono<String> getUsername() {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(sc -> Mono.just((String) ((JwtAuthenticationToken) sc.getAuthentication()).getTokenAttributes().get("user_name")));
    }

}
