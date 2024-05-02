package com.ekenya.chamakyc.wrappers.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.ekenya.chamakyc.util.StringConstantsUtil.UPPER_AND_LOWER_CASE_MATCH;

@Getter
@Setter
@NoArgsConstructor
public class MessageTemplateWrapper {
    Long id;
    String template;
    @Size(max = 15, message = "Message type can only have a max of 15 digits")
    @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Account number cannot contain letters or special characters")
    String type;
    String language;
}
