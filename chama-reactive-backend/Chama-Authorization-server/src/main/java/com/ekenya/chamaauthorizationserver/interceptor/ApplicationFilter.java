package com.ekenya.chamaauthorizationserver.interceptor;

import com.ekenya.chamaauthorizationserver.entity.Users;
import com.ekenya.chamaauthorizationserver.services.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationFilter extends OncePerRequestFilter {
    private final ObjectMapper mapper;
    private final Gson gson;
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String upd = httpServletRequest.getHeader("authorization");
        String pair = new String(Base64.decodeBase64(upd.substring(6)));
        String userName = pair.split(":")[0];
        HttpServletResponseCopier responseCopier = new HttpServletResponseCopier(httpServletResponse);
        filterChain.doFilter(httpServletRequest, responseCopier);
        byte[] copy = responseCopier.getCopy();
        String tokenInformation = new String(copy, httpServletResponse.getCharacterEncoding());
        JsonElement jsonElement = new JsonParser().parse(tokenInformation);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        if (httpServletRequest.getRequestURI().equalsIgnoreCase("/oauth/token")) {
            //check the logged user channel;
            String channel = "";
            if (userName.equals("chama_clientid")) {
                channel = "app";
            } else if (userName.equals("chama_portal")) {
                channel = "portal";
            } else {
                channel = "ussd";
            }
            AtomicReference<String> atomicUsername = new AtomicReference<>();

            httpServletRequest.getParameterMap().forEach((s, strings) -> {
                if (s.equals("username")) atomicUsername.set(strings[0]);
            });

            tokenInformation = new String(copy, httpServletResponse.getCharacterEncoding());
            jsonElement = new JsonParser().parse(tokenInformation);
            jsonObject = jsonElement.getAsJsonObject();

            Users user = userService.searchUserByUsernameAndChannel(atomicUsername.get(), channel);
            int status = httpServletResponse.getStatus();

            if (user == null) {
                AccessTokenWrapper accessTokenWrapper = new AccessTokenWrapper();
                accessTokenWrapper.setStatus(400);
                accessTokenWrapper.setMessage("Phone number or email not found");

                errorLoginResponse(responseCopier, accessTokenWrapper);

                return;
            }

            if (status != 200) {
                if (user.isBlocked()) {
                    AccessTokenWrapper accessTokenWrapper = new AccessTokenWrapper();
                    accessTokenWrapper.setStatus(400);
                    accessTokenWrapper.setMessage("User Account is blocked");

                    errorLoginResponse(responseCopier, accessTokenWrapper);
                    return;
                }

                int loginAttempts = user.getLoginAttempts();
                log.info("Login attempts... {}", loginAttempts);
                loginAttempts = loginAttempts + 1;
                if (loginAttempts == 3) {
                    user.setBlocked(true);
                    user.setLoginAttempts(0);
                    userService.deactivateUser(user);
                }

                user.setLoginAttempts(loginAttempts);

                AccessTokenWrapper accessTokenWrapper = new AccessTokenWrapper();
                accessTokenWrapper.setStatus(400);
                accessTokenWrapper.setMessage("You have" + " " + (3 - loginAttempts) + " attempts left");

                errorLoginResponse(responseCopier, accessTokenWrapper);

                userService.saveUser(user);
                return;
            }

            jsonObject.addProperty("isFirstTimeLogin", user.isFirstTimeLogin());

            String lang;
            if (user.getLanguage() == null) {
                lang = "en";
            } else {
                lang = user.getLanguage().toLowerCase().startsWith("en") ? "en" : "swa";
            }
            jsonObject.addProperty("language", lang);
            AccessTokenWrapper accessTokenWrapper = gson.fromJson(jsonObject, AccessTokenWrapper.class);
            accessTokenWrapper.setMessage("Logged in successfully");
            accessTokenWrapper.setStatus(200);
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpServletResponse.resetBuffer();
            mapper.writeValue(httpServletResponse.getOutputStream(), accessTokenWrapper);
            userService.resetLoginAttempts(user);
        } else {
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpServletResponse.resetBuffer();

            AccessTokenWrapper accessTokenWrapper = gson.fromJson(jsonObject, AccessTokenWrapper.class);
            mapper.writeValue(httpServletResponse.getOutputStream(), accessTokenWrapper);
        }
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private void errorLoginResponse(HttpServletResponseCopier responseCopier, AccessTokenWrapper errorLogin) throws IOException {
        responseCopier.setContentType(MediaType.APPLICATION_JSON_VALUE);
        responseCopier.setStatus(200);
        responseCopier.resetBuffer();
        mapper.writeValue(responseCopier.getOutputStream(), errorLogin);
    }
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class AccessTokenWrapper {
    private int status;
    private String message;
    private String access_token;
    private String token_type;
    private String refresh_token;
    private Integer expires_in;
    private String scope;
    private String jti;
    private boolean isFirstTimeLogin;
    private String language;
}

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class ErrorLogin {
    private String error;
    private String error_description;
}
