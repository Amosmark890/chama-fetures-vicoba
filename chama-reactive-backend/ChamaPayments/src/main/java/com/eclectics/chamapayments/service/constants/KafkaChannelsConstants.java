package com.eclectics.chamapayments.service.constants;

/**
 * Define kafka topics to be used by the Stream Bridge for
 * publishing.
 * The names conform to the Consumers in the respective services.
 *
 * @author wnganga
 * @created 15/04/2022
 */
public class KafkaChannelsConstants {

    public static final String SEND_EMAIL_TOPIC = "sendEmail-in-0";
    public static final String SEND_TEXT_TOPIC = "sendVicobaText-in-0";
    public static final String CALLBACK_TOPIC = "callback-topic";

}
