package com.eclectics.notifications.model;



import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequestBody {
    String to;
    String message;
    String from;
    String transactionID;
    String username;
    String password;
    String clientid;
}
