package com.ekenya.chamakyc.dao.user;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.util.Date;

@Table(name="users")
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Users extends Auditable {
    private String firstName;
    private String lastName;
    private String email;
    @Column(unique = true)
    private String username;
    private Date dateOfBirth;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private String gender;
    private String countryCode;
    private String phoneNumber;
    private String language;
    private String nationalId;
    private String nationality;
    private Date lastLogin;
    private int loginAttempts;
    private String profilePicUrl;
    private String resourceId;
    private String channel;
    @Column(columnDefinition = "boolean default true")
    private boolean firstTimeLogin;
    @Column(columnDefinition = "boolean default true")
    private boolean active;
    @Column(columnDefinition = "boolean default true")
    private boolean blocked = false;
    @ManyToOne
    @JoinColumn(columnDefinition = "deactivated_by")
    private Users deactivatedBy;
    @ManyToOne
    @JoinColumn(columnDefinition = "roles_id")
    private Roles roles;
}
