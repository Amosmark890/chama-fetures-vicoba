package com.eclectics.commons.wrappers.Request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * Class name: NewUser
 * Creater: wgicheru
 * Date:1/29/2020
 */
@NoArgsConstructor
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewUser {
    @NotNull(message = "firstname cannot be null") @NotEmpty(message = "firstname cannot be empty")
    private String firstname;
    private String othernames;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date dateofbirth;
    @NotNull(message = "phonenumber cannot be null") @NotEmpty(message = "phonenumber cannot be empty")
    private String phonenumber;
    private String email;
    private String identification;
    private String nationality;
    private String gender;
    private String userDeviceId;
    private String oldPassword;
    private String newPassword;
}
