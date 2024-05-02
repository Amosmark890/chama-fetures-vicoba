package com.eclectics.chamapayments.service.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class StringConstantsUtil {

    public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    public static final String EMPTY_OR_EMAIL_REGEX = "(^$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$)";
    public static final String UPPER_AND_LOWER_CASE_MATCH = "^[[a-zA-z][\\s+]]*$";
    public static final String EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH = "^(^$|[[a-zA-z][\\s+]]*)$";
    public static final String LOWER_CASE_MATCH = "^[[a-z][\\s+]]*$";
    public static final String  EMPTY_OR_LOWER_CASE_MATCH = "^(^$|[[a-zA-z][\\s+]]*)$";

    public static final String PHONE_NUMBER_MATCH = "^(25[54])([71])\\d{8}$";

    public static final String EMPTY_OR_PHONE_NUMBER_MATCH = "^(^$|(25[54])([71])\\d{8})$";

    public static final String CBS_ACCOUNT_MATCH = "^(?=.{15,16}$)[0-9]*(?:\\s[0-9]*)*$";
}
