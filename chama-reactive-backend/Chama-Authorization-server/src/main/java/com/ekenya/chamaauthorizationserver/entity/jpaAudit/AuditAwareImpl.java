package com.ekenya.chamaauthorizationserver.entity.jpaAudit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String name;
        if (authentication == null) {
            name = "USER";
        } else {
            name = authentication.getName();
        }
        return Optional.of(name);
    }
}
