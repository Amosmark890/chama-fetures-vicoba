package com.ekenya.chamakyc.dao.error;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;

/**
 * @author Alex Maina
 * @created 12/01/2022
 */
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class FailedOperations extends Auditable {
    private String operation;
    private long groupId;
    private String stage;
    private String json_data;
    private String message;
}
