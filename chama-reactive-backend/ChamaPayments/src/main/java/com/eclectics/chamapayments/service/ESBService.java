package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import reactor.core.publisher.Mono;

public interface ESBService {

    Mono<UniversalResponse> balanceInquiry(String account);

}
