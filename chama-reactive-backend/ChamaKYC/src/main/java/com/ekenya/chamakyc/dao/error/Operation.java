package com.ekenya.chamakyc.dao.error;

/**
 * @author Alex Maina
 * @created 12/01/2022
 */
public enum Operation {
    OPERATION_APPLY_POLL_RESULTS("Apply poll results "),
    OPERATION_ESB_REGISTRATION_RESULTS_UPDATE("Update wallet registration");

    String name;

    Operation(String name){
        this.name= name;
    }
}
