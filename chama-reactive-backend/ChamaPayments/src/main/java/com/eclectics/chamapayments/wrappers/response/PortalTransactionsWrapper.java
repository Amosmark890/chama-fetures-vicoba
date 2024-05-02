package com.eclectics.chamapayments.wrappers.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Class name: PortalTransactionsWrapper
 * Creater: wgicheru
 * Date:4/3/2020
 */
@Getter
@Setter
public class PortalTransactionsWrapper {
    long paymentscount;
    double paymentsvalue;
    double paymentsaverage;

    long withdrawalscount;
    double withdrawalvalue;
    double withdrawalaverage;

}
