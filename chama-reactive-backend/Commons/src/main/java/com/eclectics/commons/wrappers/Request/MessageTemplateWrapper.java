package com.eclectics.commons.wrappers.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: MessageTemplateWrapper
 * Creater: wgicheru
 * Date:3/2/2020
 */
@Data
public class MessageTemplateWrapper {
    /**
     * The Templateid.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    long templateid;
    /**
     * The Language.
     */
    @NotNull(message = "language cannot be null") @NotEmpty(message = "language cannot be empty")
    String language;
    /**
     * The Template.
     */
    @NotNull(message = "template cannot be null") @NotEmpty(message = "template cannot be empty")
    String template;
    /**
     * The Type.
     */
    @NotNull(message = "type cannot be null") @NotEmpty(message = "type cannot be empty")
    String type;
}
