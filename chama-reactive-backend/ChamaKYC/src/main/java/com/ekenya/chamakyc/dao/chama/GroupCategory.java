package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * @author Alex Maina
 * @created 05/01/2022
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Entity
@Table(name = "groupCategory")
public class GroupCategory extends Auditable {
    private String name;
    private long number;
    private String url;
    private String config;
}
