package com.ekenya.apigateway.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.netty.util.internal.StringUtil;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.ekenya.apigateway.utils.CommonFields.*;


@UtilityClass
public class MaskUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaskUtils.class.getName());

    private static final String[] maskFields = new String[]{CHANNEL_KEY, CREDIT_ACCOUNT_FIELD, DEBIT_ACCOUNT_FIELD, EMAIL_FIELD, PHONE_NUMBER_FIELD, PHONE_NUMBER, BILLER_REF, DEBIT_ACCOUNT_FIELD_CAMEL, CREDIT_ACCOUNT_FIELD_CAMEL, TRX_PIN_FIELD, "otp_code", "text_message", "pin", "password", "dob", "credentials", "receiverPhone"};

    public static String maskString(String s, int begin, int end) {
        StringBuilder builder = new StringBuilder(DEFAULT_BUFFER_SIZE);
        try {
            if (end > 15) {
                begin = -5;
            }
            if (StringUtil.isNullOrEmpty(s)) {
                builder.append(repeat("*", 10));
            } else if (begin < 0) {
                builder.append(repeat("*", Math.min(s.length(), 15)));
            } else if (s.length() > (begin + end)) {
                builder.append(s, 0, begin);
                builder.append(repeat("*", s.length() - begin - end));
                builder.append(s.substring(s.length() - end));
            } else {
                builder.append(repeat("*", s.length()));
            }
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage());
        }
        return builder.toString();
    }

    /**
     * Repeat string N times
     *
     * @param str   String to repeat
     * @param times Number of times to repeat
     * @return <code>str</code> repeated <code>times</cod>
     */
    public static String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    /**
     * Mask JSON object before printing it on the screen
     *
     * @param object {@link JsonObject}
     * @param fields Extra fields to mask
     * @return Masked field
     */
    public static String maskObject(String object, String... fields) {
        if (object == null) {
            return new JsonObject().getAsString();
        }
        Gson gson = new Gson();

        JsonObject jsonObject = gson.fromJson(object, JsonObject.class);
        JsonObject n = jsonObject.deepCopy();
        List<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(fields));
        list.addAll(Arrays.asList(maskFields));
        try {

            jsonObject.keySet()
                    .forEach(key -> {
                        Object o = jsonObject.get(key);
                        if (o != null && !(o instanceof JsonObject || o instanceof JsonArray)) {
                            String value = String.valueOf(o);
                            if (key.contains("account") || key.contains("balance") || key.contains("transactionRef")) {
                                n.addProperty(key, maskString(value, 0, value.length() / 2));
                            } else if (key.contains("user") || key.contains("phone") || key.contains("mobile")) {
                                n.addProperty(key, maskString(value, 0, value.length() / 2));
                            } else if (list.contains(key)) {
                                n.addProperty(key, maskString(value, 0, value.length()));
                            } else if (key.contains("card") || key.contains("key")) {
                                n.addProperty(key, repeat("C", 10));
                            } else {
                                n.add(key, jsonObject.get(key));
                            }
                        } else if (o instanceof JsonObject) {
                            n.add(key, gson.fromJson(gson.toJson(o), JsonObject.class));
//                            n.add(key, gson.fromJson(maskObject(gson.toJson(o)), JsonObject.class));
                        }
                    });
        } catch (Exception ex) {
            LOGGER.warn("Failed to mask JSON object: ", ex);
        }

        return gson.toJson(n);
    }
}
