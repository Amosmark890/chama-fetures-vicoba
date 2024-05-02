package com.eclectics.notifications.service;

import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 13/12/2021
 */
public interface SubscriberService {

    Consumer<String> sendText();

    Consumer<String> sendVicobaText();

    Consumer<String> sendEmail();
}
