package com.ekenya.chamakyc.wrappers.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * Class name: CreateconfigWrapper
 * Creater: wgicheru
 * Date:3/23/2020
 */
@Getter
@Setter
public class CreateconfigWrapper {
    @NotNull(message = "field cannot be null") @NotEmpty(message = "field cannot be empty")
    String configname;
}
