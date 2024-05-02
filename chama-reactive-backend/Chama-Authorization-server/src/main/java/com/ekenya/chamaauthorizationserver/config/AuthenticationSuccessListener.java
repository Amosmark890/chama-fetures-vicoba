package com.ekenya.chamaauthorizationserver.config;

import com.ekenya.chamaauthorizationserver.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
    @Autowired
    private UserService userService;
    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent authenticationSuccessEvent) {
        String loggedInUser= authenticationSuccessEvent.getAuthentication().getName();
        userService.updateLastLogin(loggedInUser);
    }
}
