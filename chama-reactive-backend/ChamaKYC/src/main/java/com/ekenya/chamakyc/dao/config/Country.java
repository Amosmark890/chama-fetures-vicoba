package com.ekenya.chamakyc.dao.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "countries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Country {
    @Id
    @Column(name = "code", nullable = false, length = 5)
    private String id;
    @Column(name = "nationality", nullable = false, length = 50)
    private String nationality;
    @Column(name = "dial_code", nullable = false)
    private Integer dialCode;
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    @Column(name = "language", nullable = false, length = 20)
    private String language;

}