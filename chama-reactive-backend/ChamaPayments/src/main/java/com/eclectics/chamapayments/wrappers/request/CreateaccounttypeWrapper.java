package com.eclectics.chamapayments.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Class name: CreateaccounttypeWrapper
 * Creater: wgicheru
 * Date:3/23/2020
 */
@Getter
@Setter
public class CreateaccounttypeWrapper {
    @NotNull(message = "field cannot be null") @NotEmpty(message = "field cannot be empty")
    private String name, prefix;
    @NotNull(message = "requiredfields cannot be null")
    List<String> requiredfields;
}
