package com.ekenya.chamaauthorizationserver.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.io.Serializable;

@Table(name="rolesconfig")
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Roles implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    //@Transient
    private String name;
    //@Transient
    @Column(columnDefinition = "longtext")
    private String rules;
    //@Transient
    @Column(columnDefinition = "longtext")
    private String resourceid;
}

