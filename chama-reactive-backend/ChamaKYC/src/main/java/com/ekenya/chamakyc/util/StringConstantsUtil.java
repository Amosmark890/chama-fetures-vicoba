package com.ekenya.chamakyc.util;

public class StringConstantsUtil {

    private StringConstantsUtil() {
    }

    public static final String ACTIVATION_URL = "http://192.168.20.42:8000/#/auth/set-password?token=%s";

    public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    public static final String EMPTY_OR_EMAIL_REGEX = "(^$|^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$)";

    public static final String UPPER_AND_LOWER_CASE_MATCH = "^[[a-zA-z][\\s+]]*$";
    public static final String EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH = "^(^$|[[a-zA-z][\\s+]]*$)";
    public static final String EMPTY_OR_WORD = "^(^$|[[a-zA-z0-9][\\s+]]*$)";

    public static final String LOWER_CASE_MATCH = "^[[a-z][\\s+]]*$";
    public static final String  EMPTY_OR_LOWER_CASE_MATCH = "^(^$|[[a-zA-z][\\s+]]*$)";

    public static final String PHONE_NUMBER_MATCH = "^(25[54])\\d{9}$";

    public static final String EMPTY_OR_PHONE_NUMBER_MATCH = "^(^$|(25[54])\\d{9})$";

    public static final String CBS_ACCOUNT_MATCH = "(^$|^[0-9]{15,16}$)";
}
