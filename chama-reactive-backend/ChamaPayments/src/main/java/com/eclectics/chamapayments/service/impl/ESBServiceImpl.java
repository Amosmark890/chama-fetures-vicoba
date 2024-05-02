package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.ESBService;
import com.eclectics.chamapayments.util.TransactionIdGenerator;
import com.eclectics.chamapayments.wrappers.response.BalanceInquiry;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.eclectics.chamapayments.util.RequestConstructor.getBalanceInquiryReq;

@Service
@RequiredArgsConstructor
public class ESBServiceImpl implements ESBService {

    @Value("${vicoba.url}")
    private String esbURL;
    private final Gson gson;
    private WebClient webClient;
    private final ResourceBundleMessageSource source;

    @PostConstruct
    private void init() {
        webClient = WebClient
                .builder()
                .baseUrl(esbURL)
                .build();
    }

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    @Override
    public Mono<UniversalResponse> balanceInquiry(String account) {
        Map<String, String> balanceInquiryReq = getBalanceInquiryReq(account);
//        TransactionIdGenerator.saveEsbRequestLog(balanceInquiryReq);

        return webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(balanceInquiryReq))
                .retrieve()
                .bodyToMono(String.class)
                .map(jsonString -> {
                    JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

                    if (jsonObject.get("48").getAsString().equals("fail"))
                        return new UniversalResponse("fail", jsonObject.get("54").getAsString());

                    BalanceInquiry balanceInquiry = gson.fromJson(jsonObject.get("54").getAsJsonObject(), BalanceInquiry.class);

                    return new UniversalResponse("success", getResponseMessage("balanceInquirySuccessful"), balanceInquiry);
                }).onErrorReturn(new UniversalResponse("fail", getResponseMessage("serviceNotAvailable")));
    }
}
