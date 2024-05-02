package com.ekenya.chamakyc.dao.user;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;

@Table(name="rolesconfig")
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Roles extends Auditable implements Serializable {
    private String name;
    private String rules;
    private String resourceid;
}

