package com.ekenya.chamakyc.dao.config;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    private String id;
    private String uri;
    private String queryParams;
    private String requestBody;
    private String responseBody;
    private String userAgent;
    private String methodType;
    private String remoteAddress;
    private LocalDateTime requestTime;
    private LocalDateTime responseTime;
    private int responseStatus;
}
