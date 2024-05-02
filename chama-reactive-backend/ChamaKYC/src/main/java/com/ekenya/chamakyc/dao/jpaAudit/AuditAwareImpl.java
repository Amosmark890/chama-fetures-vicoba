package com.ekenya.chamakyc.dao.jpaAudit;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class AuditAwareImpl implements AuditorAware<String> {
    @Override
    public Optional<String> getCurrentAuditor() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return Optional.of("USER");
        return Optional.of(SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
