package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
@Entity
@Table(name = "group_membership_tbl")
public class GroupMembership extends Auditable {
    private boolean activemembership = true;
    private boolean isrequesttoleaveactedon = false;
    private boolean requesttoleavegroup = false;
    private String deactivationreason;
    private String title;
    @Column(columnDefinition = "text")
    private String permissions;
    @JsonManagedReference
    @ManyToOne
    private Member members;
    @ManyToOne
    private Group group;

}
