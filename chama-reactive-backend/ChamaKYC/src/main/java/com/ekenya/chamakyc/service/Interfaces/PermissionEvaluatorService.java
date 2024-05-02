package com.ekenya.chamakyc.service.Interfaces;

import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 * @author Alex Maina
 * @created 07/01/2022
 */
public interface PermissionEvaluatorService {
    boolean hasPermission(Authentication auth, Serializable targetId, String scope, Object objectaction);
}
