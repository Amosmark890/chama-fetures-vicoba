package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.MemberTransactionStatusService;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MemberAOP {

    private final ChamaKycService chamaKycService;

    private final ResourceBundleMessageSource source;
    private final MemberTransactionStatusService memberTransactionStatusService;

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    @Around("@annotation(com.eclectics.chamapayments.service.enums.CanTransact)")
    public Mono<?> checkIfMemberCanTransact(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] objects = joinPoint.getArgs();

        String username = (String) objects[1];

        Optional<MemberWrapper> memberWrapper = chamaKycService.searchMemberByPhoneNumber(username);

        if (memberWrapper.isEmpty()) {
            throw new IllegalArgumentException("Member not found");
        }

        MemberWrapper member = memberWrapper.get();

        boolean canTransact = memberTransactionStatusService.canTransact(member.getEsbwalletaccount());

        if (!canTransact)
            throw new IllegalArgumentException(getResponseMessage("thereIsAnExistingTrx"));

        var response = joinPoint.proceed(objects);

        if (response instanceof Mono<?>) {
            return (Mono<?>) response;
        }

        log.error("Request failed after point cut. The returned response is not of type Mono<?>");
        return Mono.just(ResponseEntity.ok().body(UniversalResponse.builder()
                .status("Fail")
                .message("serviceNotAvailable")
                .build()));
    }
}
