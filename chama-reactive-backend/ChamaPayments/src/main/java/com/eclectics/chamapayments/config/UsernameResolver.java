package com.eclectics.chamapayments.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@Slf4j
@ControllerAdvice
public class UsernameResolver {

    @ModelAttribute
    private String getAuthenticationUsername(Authentication authentication) {
        if (authentication == null) return null;

        if (authentication.getPrincipal() instanceof User) {
            log.info("Currently logged in user => {}", ((User) authentication.getPrincipal()).getUsername());
            return ((User) authentication.getPrincipal()).getUsername();
        }

        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            return jwt.getClaimAsString("user_name");
        }

        return (String) authentication.getPrincipal();
    }

}
