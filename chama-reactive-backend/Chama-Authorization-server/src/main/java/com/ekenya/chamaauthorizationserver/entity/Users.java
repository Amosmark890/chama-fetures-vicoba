package com.ekenya.chamaauthorizationserver.entity;

import com.ekenya.chamaauthorizationserver.entity.jpaAudit.Auditable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;

@Table(name="users")
@Entity
@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Users extends Auditable {
    
    private String firstName;
    
    private String lastName;
    
    private String email;
    
    private String username;
    
    private Date dateOfBirth;
    
    private String password;
    
    private String gender;
    
    private String countryCode;
    
    private String phoneNumber;
    
    private String language;
    
    private String nationalId;
    
    private String nationality;
    
    private Date lastLogin;
    
    private String profilePicUrl;
    
    private String resourceId;

    private boolean firstTimeLogin;

    @Column(columnDefinition = "boolean default false")
    private boolean active = false;

    @Column(columnDefinition = "boolean default false")
    private boolean blocked = false;

    private boolean softDelete;

    private int loginAttempts;
    
    private String channel;
    @ManyToOne
    @JoinColumn(columnDefinition = "deactivated_by")
    private Users deactivatedBy;
    
    @ManyToOne
    private Roles roles;

}
