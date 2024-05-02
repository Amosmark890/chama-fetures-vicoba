package com.eclectics.chamapayments.model;

import com.eclectics.chamapayments.model.jpaAudit.Auditable;
import com.google.gson.JsonObject;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Map;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "esb_request_logs")
public class EsbRequestLog extends Auditable {
    @Column(columnDefinition = "boolean default false")
    private boolean callbackReceived;
    private String field0;
    private String field2;
    private String field3;
    private String field4;
    private String field24;
    private String field25;
    private String field32;
    private String field37;
    private String field48;
    private String field61;
    private String field65;
    private String field66;
    private String field68;
    private String field102;
    private String field103;


}
