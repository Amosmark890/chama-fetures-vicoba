package com.eclectics.chamapayments.service;

public interface PublishingService {
    void sendEmailNotification(String title, String message, String email);
    void sendText(String message, String phoneNumber);
}
