package com.ekenya.chamakyc.wrappers.request;

import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvitesWrapper {
    @NotNull(message = "groupid cannot be null")
    long groupid;
    String phoneNumber;
    List<MemberRoles> phonenumbersandrole;
}
