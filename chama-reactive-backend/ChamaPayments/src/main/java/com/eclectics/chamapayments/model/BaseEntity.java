package com.eclectics.chamapayments.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 *
 * @version 2.0
 */
@MappedSuperclass
@Getter
@Setter
public class BaseEntity implements Serializable {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(name = "created_on",updatable = false)
    @CreationTimestamp  @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    private Date createdOn;
    @Column(name = "softDelete", columnDefinition = "boolean default false")
    private boolean softDelete;

    @PrePersist
    public void addData() {
        this.createdOn = new Date();
        this.softDelete = false;
    }
}
