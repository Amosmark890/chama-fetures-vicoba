package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import com.ekenya.chamakyc.dao.user.Users;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Table(name = "groups_documents")
@Entity
@Builder
public class GroupDocuments extends Auditable {
    private String name;
    private String fileName;
    private String path;
    @ManyToOne
    private Group group;
    @ManyToOne
    private Users uploadedBy ;




}
