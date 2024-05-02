package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Class name: AccounttypeWrapper
 * Creater: wgicheru
 * Date:3/9/2020
 */
@Getter
@Setter
public class AccounttypeWrapper {
    private String name;
    private String prefix;
    private List<String> requiredfields;
    private long id;
}
