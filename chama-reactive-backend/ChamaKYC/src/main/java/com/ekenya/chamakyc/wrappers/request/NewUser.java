package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

import static com.ekenya.chamakyc.util.StringConstantsUtil.*;

/**
 * Class name: NewUser
 * Creater: wgicheru
 * Date:1/29/2020
 */
@NoArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewUser {
    @NotNull(message = "firstname cannot be null")
    @NotEmpty(message = "firstname cannot be empty")
    @Size(max = 25, message = "First name should be of length 25 or less")
    @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "First Name should not contain special characters and digits")
    private String firstname;

    @Size(max = 25, message = "Name cannot be of length greater than 25 characters")
    @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Other names should not contain special characters and digits")
    private String othernames;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private Date dateofbirth;
    @NotNull(message = "phonenumber cannot be null")
    @NotEmpty(message = "phonenumber cannot be empty")
    private String phonenumber;

    @Pattern(regexp = EMAIL_REGEX, message = "Provide a valid email address")
    private String email;

    @Size(max = 25, message = "Identification cannot be of length greater than 25 characters")
    @Pattern(regexp = EMPTY_OR_WORD, message = "Identification should not contain special characters and digits")
    private String identification;

    @Size(max = 25, message = "Nationality cannot be of length greater than 25 characters")
    @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Nationality should not contain special characters and digits")
    private String nationality;

    @Size(max = 10, message = "Language length not valid. At most 10 characters expected")
    @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Language should not contain digits or special characters")
    private String language = "English";
    @Size(max = 10, message = "Gender size is not valid")
    @Pattern(regexp = "(male|female/Male/Female)", message = "Only male/Male or female/Female allowed")
    private String gender = "Prefer Not To Say";
    private String userDeviceId;
    private String oldPassword;
    private String newPassword;
    private String linkedAccounts;
}
