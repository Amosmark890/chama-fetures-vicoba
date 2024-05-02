package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class GroupDetailsWrapper {
    private String cbsAccountName;
    private String availableBalance = "0.0";
    @Size(max = 15, message = "Length cannot be more than 15")
    private String cbsAccount;
    @Size(max = 25, message = "Group name cannot be of length greater than 25")
    @NotNull(message = "Group name is required")
    private String groupname = "";
    @Size(max = 200, message = "Description cannot be of length greater than 200")
    private String description;
    @Size(max = 100, message = "Purpose cannot be of length greater than 100")
    private String purpose;
    private Long categoryId = 2L;
    @NotNull(message = "Please provide the secretary phone number")
    @Size(max = 12, message = "Secretary Phone number number cannot be of length greater than 12")
    private String secretaryPhoneNumber;
    @NotNull(message = "Please provide the treasurer phone number")
    @Size(max = 12, message = "Treasurer Phone number number cannot be of length greater than 12")
    private String treasurerPhoneNumber;
}
