package com.eclectics.notifications.service.impl;

import com.eclectics.notifications.service.SendMailEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

/**
 * Class name: SendmailEngine
 * Creater: wgicheru
 * Date:1/27/2020
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SendMailEngineImpl implements SendMailEngine {

    @Value("${email.send-from}")
    private String sendFrom;

    private final JavaMailSender javaMailSender;

    /**
     * Send email.
     *
     * @param subject the subject
     * @param body    the body
     * @param sendto  the sendto
     */
    @Override
    public void sendMail(String subject, String body, String sendto) {

        MimeMessage message = javaMailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(sendFrom);
            helper.setTo(sendto);
            helper.setSubject(subject);
            helper.setText(body);

            log.info("Sending mail... To => {} Subject => {}  Body => {} ", sendto, subject, body);
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.info("email sent error {}", ex.getLocalizedMessage());
        }
    }
}
