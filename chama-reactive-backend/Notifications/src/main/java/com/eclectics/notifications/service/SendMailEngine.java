package com.eclectics.notifications.service;

public interface SendMailEngine {

    void sendMail(String subject, String body, String to);

}
