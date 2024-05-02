package com.eclectics.chamapayments.wrappers.esbWrappers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Alex Maina
 * @created 24/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class TransactionDetails {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String prefix;
    private String phone_number;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String customer_names;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String req_type;
    @JsonInclude(JsonInclude.Include.NON_NULL) @JsonProperty("is_management")
    private boolean is_management;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transaction_type;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private double amount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String host_code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String direction;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String debit_account;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String transaction_code;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String currency;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String credit_account;

    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error(e.getMessage());
        }
        return null;
    }

}
