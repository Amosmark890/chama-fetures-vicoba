package com.ekenya.chamakyc.dao.chama;

import com.ekenya.chamakyc.dao.jpaAudit.Auditable;
import com.ekenya.chamakyc.dao.user.Users;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Otp extends Auditable {
    private String otpValue;
    private boolean expired;
    private String otpType;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonMerge
    @JsonProperty("user")
    private Users user;

}
