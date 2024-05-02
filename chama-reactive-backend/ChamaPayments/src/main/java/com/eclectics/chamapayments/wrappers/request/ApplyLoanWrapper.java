package com.eclectics.chamapayments.wrappers.request;

import com.eclectics.chamapayments.model.Guarantors;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class name: ApplyLoanWrapper
 * Creater: wgicheru
 * Date:4/22/2020
 */
@Getter
@Setter
public class ApplyLoanWrapper {
    @NotNull(message = "loanproduct cannot be null")
    long loanproduct;
    @NotNull(message = "amount cannot be null")
    double amount;
    String coreAccount = "";
    Map<String, Object> reminder = Collections.emptyMap();
    List<Guarantors> guarantors;
}
