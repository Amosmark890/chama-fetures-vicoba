package com.ekenya.chamakyc.wrappers.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponse {
    private String access_token;
    private String token_type;
    private String refresh_token;
    private long expires_in;
    private String scope;
    private String jti;
    private boolean firstTimeLogin;
    private String language;
    private String message;
    private int status;
}
