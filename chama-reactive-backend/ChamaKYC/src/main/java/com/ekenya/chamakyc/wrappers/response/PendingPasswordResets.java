package com.ekenya.chamakyc.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Class name: PendingPasswordResets
 * Creater: wgicheru
 * Date:2/17/2020
 */
@Getter
@Setter
public class PendingPasswordResets {
    String email;
    String requestedby;
    @JsonFormat(pattern="dd-MM-yyyy HH:mm:ss")
    Date requestedon;
    boolean done;
}
