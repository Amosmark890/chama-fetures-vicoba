package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.service.SubscribeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscribeServiceImpl implements SubscribeService {

    private final AccountingService accountingService;

    @Bean
    public Consumer<String> createMemberWallet() {
        return walletInfo -> log.info("Wallet information... {}", walletInfo);
    }

    @Bean
    public Consumer<String> createGroupContribution() {
        return accountingService::createGroupContribution;
    }

    @Bean
    public Consumer<String> createGroupAccount() {
        return accountingService::createGroupAccount;
    }

    @Bean
    @Override
    public Consumer<String> enableGroupContributions() {
        return accountingService::enableGroupContributions;
    }

    @Bean
    @Override
    public Consumer<String> disableGroupContributions() {
        return accountingService::disableGroupContributions;
    }

    @Bean
    @Override
    public Consumer<String> writeOffLoansAndPenalties() {
        return accountingService::writeOffLoansAndPenalties;
    }

    @Bean
    @Override
    public Consumer<String> editContributionName() {
        return accountingService::editContributionName;
    }

    @Bean
    @Override
    public Consumer<String> updateGroupCoreAccount() {
        return accountingService::updateGroupCoreAccount;
    }
}
