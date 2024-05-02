package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.repository.AccountsRepository;
import com.eclectics.chamapayments.repository.ContributionLoanRepository;
import com.eclectics.chamapayments.repository.ContributionsPaymentRepository;
import com.eclectics.chamapayments.repository.WithdrawallogsRepo;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.NotificationService;
import com.eclectics.chamapayments.service.PaymentUtil;
import com.eclectics.chamapayments.service.RouterService;
import com.eclectics.chamapayments.service.enums.B2CTransactionStatus;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Alex Maina
 * @created 23/12/2021
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RouterServiceImpl implements RouterService {
    private final ContributionLoanRepository contributionLoanRepository;
    private final ContributionsPaymentRepository contributionsPaymentRepository;
    private final WithdrawallogsRepo withdrawallogsRepo;
    private final NotificationService notificationService;
    private final WebClient webClient;
    private final ChamaKycService chamaKycService;
    @Lazy
    @Autowired
    private PaymentUtil paymentUtil;
    @Lazy
    @Autowired
    private LoanServiceImpl loanService;
    private final AccountsRepository accountsRepository;

    @Value("${app-configs.online-checkout}")
    public String onlineCheckoutURL;
    @Value("${app-configs.online-query}")
    public String mpesaQueryURL;
    @Value("${app-configs.b2c-url}")
    public String B2CURL;
    @Value("${app-configs.b2c-online-query}")
    public String B2CURLQueryTransactionStatus;
    @Value("${app-configs.pg-client-username}")
    public String pgUsername;
    @Value("${app-configs.pg-client-id}")
    public String pgClientId;
    @Value("${app-configs.pg-client-name}")
    public String pgClientName;
    @Value("${app-configs.pg-client-password}")
    public String pgClientPassword;
    @Value("${app-configs.pg-service-id}")
    public String pgServiceId;
    @Value("${app-configs.pg-service-b2c-id}")
    public String pgB2cServiceId;

    @Override
    public Mono<Boolean> makeB2Crequest(String phoneNumber, double amount, Long contributionLoanId) {
        return Mono.fromCallable(() -> {
                    String transactionId = UUID.randomUUID().toString();
                    JsonObject jsonObject = getRequestJsonObject(phoneNumber, amount, transactionId);
                    Optional<Loan> optionalLoan = contributionLoanRepository.findById(contributionLoanId);
                    if (optionalLoan.isEmpty()) return Mono.just(false);
                    Loan loan = optionalLoan.get();
                    loan.setTransactionRef(transactionId);
                    contributionLoanRepository.save(loan);

                    return webClient.post()
                            .uri(B2CURL)
                            .body(jsonObject.toString(), String.class)
                            .retrieve()
                            .bodyToMono(String.class)
                            .retryWhen(Retry.backoff(4, Duration.ofSeconds(2)))
                            .flatMap(response -> {
                                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                                loan.setTransactionDescription(json.get("statusDescription").getAsString());
                                log.info("===B2C RESPONSE" + json.getAsString());
                                if (json.get("status").getAsString().equals("00")) {
                                    loan.setTransactionStatus(B2CTransactionStatus.TRANSACTION_IN_PROGRESS.name());
                                } else {
                                    loan.setTransactionStatus(B2CTransactionStatus.FAILED.name());
                                }
                                contributionLoanRepository.save(loan);
                                boolean success = json.get("status").getAsString().equals("00");
                                return Mono.just(success);
                            });
                })
                .flatMap(res -> res)
                .publishOn(Schedulers.boundedElastic());
    }

    private JsonObject getRequestJsonObject(String phoneNumber, double amount, String transactionId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", pgUsername);
        jsonObject.addProperty("password", pgClientPassword);
        jsonObject.addProperty("amount", (int) amount);
        jsonObject.addProperty("clientid", pgClientId);
        jsonObject.addProperty("accountno", phoneNumber);
        jsonObject.addProperty("currencycode", "KES");
        jsonObject.addProperty("serviceid", pgB2cServiceId);
        jsonObject.addProperty("msisdn", phoneNumber);
        jsonObject.addProperty("transactionid", transactionId);
        jsonObject.addProperty("timestamp", new Date().toString());
        return jsonObject;
    }

    @Override
    @Async
    public void makeB2CwithdrawalRequest(String phoneNumber, double amount, String uniqueId) {
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);

        JsonObject jsonObject = getRequestJsonObject(phoneNumber, amount, uniqueId);
        webClient.post()
                .uri(B2CURL)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(4, Duration.ofSeconds(2)))
                .subscribe(resp -> {
                    JsonObject json = JsonParser.parseString(resp).getAsJsonObject();
                    WithdrawalLogs withdrawalLogs = withdrawallogsRepo.getByUniqueTransactionId(uniqueId);
                    if (withdrawalLogs == null) {
                        return;
                    }
                    if (json.get("status").getAsString().equals("00")) {
                        withdrawalLogs.setTransferToUserStatus(B2CTransactionStatus.TRANSACTION_IN_PROGRESS.name());
                    } else {
                        withdrawalLogs.setTransferToUserStatus(B2CTransactionStatus.FAILED.name());

                        notificationService.sendB2cError(phoneNumber, memberWrapper.getFirstname(), amount, memberWrapper.getLanguage());
                    }

                    withdrawallogsRepo.save(withdrawalLogs);
                });
    }

    @Override
    public String makeStkPushRequest(JsonObject jsonObject) {
        return webClient.post()
                .uri(onlineCheckoutURL)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(4, Duration.ofSeconds(2)))
                .block();
    }

    @Async
    @Override
    public void queryMpesaTransactionStatus(String mpesaCheckoutId) {
        log.info("querying Mpesa status for " + mpesaCheckoutId);
        JsonObject jsonObject = getRequestObject(mpesaCheckoutId);
        webClient.post()
                .uri(mpesaQueryURL)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                .subscribe(response -> {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    Optional<ContributionPayment> contributionPaymentOptional = contributionsPaymentRepository.findContributionByMpesaId(mpesaCheckoutId);
                    contributionPaymentOptional.ifPresent(contributionPayment -> paymentUtil.extractMpesaResponse(json, contributionPayment));
                });
    }

    private JsonObject getRequestObject(String mpesaCheckoutId) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("transactionid", mpesaCheckoutId);
        jsonObject.addProperty("serviceid", pgServiceId);
        jsonObject.addProperty("clientid", pgClientId);
        jsonObject.addProperty("password", pgClientPassword);
        jsonObject.addProperty("username", pgUsername);


        return jsonObject;
    }

    @Override
    public void queryLoanPenaltyPaymentStatus(LoanPenaltyPayment loanPenaltyPayment) {
        JsonObject jsonObject = getRequestObject(loanPenaltyPayment.getMpesaCheckoutId());
        webClient.post()
                .uri(mpesaQueryURL)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .subscribe(response -> {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    paymentUtil.extractLoanPaymentPenalty(json, loanPenaltyPayment);
                });
    }

    @Override
    public void queryLoanPaymentStatus(LoanRepaymentPendingApproval loanRepaymentPendingApproval) {
        JsonObject jsonObject = getRequestObject(loanRepaymentPendingApproval.getMpesaCheckoutId());
        webClient.post()
                .uri(mpesaQueryURL)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)))
                .subscribe(res -> {
                    JsonObject json = JsonParser.parseString(res).getAsJsonObject();
                    paymentUtil.extractLoanPaymentResponse(json, loanRepaymentPendingApproval);
                });
    }


    @Override
    public void queryB2CTransactionStatus(String transactionId, boolean isContribution) {
        log.info("quering b2c status for transaction id");
        JsonObject jsonObject = getRequestObject(transactionId);
        webClient.post()
                .uri(B2CURLQueryTransactionStatus)
                .body(jsonObject.toString(), String.class)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(2)))
                .subscribe(response -> {
                    JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                    log.info("RECEIVING b2c request RESPONSE " + json.toString());
                    if (json.get("status").getAsString().equals("16"))
                        return;
                    if (json.get("status").getAsString().equals("99"))
                        return;

                    if (json.get("status").getAsString().equals("96"))
                        return;
                    // checks for only two instances contribution & loan withdrawal
                    //does not do check for member withdrawal
                    if (isContribution) {
                        Loan contributionLoan = contributionLoanRepository.findLoanByTransactionById(transactionId).orElse(null);
                        if (contributionLoan == null) return;
                        contributionLoan.setTransactionDescription(json.get("statusDescription").getAsString());

                        if (json.get("status").getAsString().equals("00")) {
                            contributionLoan.setTransactionStatus(B2CTransactionStatus.TRANSFERRED_TO_USER.name());
                            paymentUtil.creditUserWalletAccount(contributionLoan.getPhoneNumber(), contributionLoan.getContributionId(), contributionLoan.getAmount());
                        } else {
                            contributionLoan.setTransactionStatus(B2CTransactionStatus.FAILED.name());
                        }
                        contributionLoanRepository.save(contributionLoan);
                    } else {
                        WithdrawalLogs withdrawalLogs = withdrawallogsRepo.getByUniqueTransactionId(transactionId);
                        if (json.get("status").getAsString().equals("00")) {
                            Accounts accounts = withdrawalLogs.getDebitAccounts();
                            if (transactionId.contains("LA_")) {
                                LoanApplications loanApplications = withdrawalLogs.getLoanApplications();
                                withdrawalLogs.setNewbalance(accounts.getAccountbalance() - withdrawalLogs.getTransamount());
                                withdrawalLogs.setOldbalance(accounts.getAccountbalance());
                                withdrawalLogs.setTransferToUserStatus(B2CTransactionStatus.TRANSFERRED_TO_USER.name());
                                loanService.logLoanApplication(withdrawalLogs, accounts, loanApplications);
                            } else {
                                accounts.setAccountbalance(accounts.getAccountbalance() - withdrawalLogs.getTransamount());
                                withdrawalLogs.setNewbalance(accounts.getAccountbalance());
                                withdrawallogsRepo.save(withdrawalLogs);
                                accountsRepository.save(accounts);
                            }
                            paymentUtil.creditUserWalletAccount(withdrawalLogs.getCreditphonenumber(), withdrawalLogs.getContributions().getId(), (int) withdrawalLogs.getTransamount());
                        } else {
                            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(withdrawalLogs.getCreditphonenumber());
                            try {
                                notificationService.sendB2cError(withdrawalLogs.getCreditphonenumber(), memberWrapper.getFirstname(), withdrawalLogs.getTransamount(), memberWrapper.getLanguage());
                            } catch (Exception e){
                                log.error("send B2C sms error--------{}",e);
                            }

                            withdrawalLogs.setTransferToUserStatus(B2CTransactionStatus.FAILED.name());
                            withdrawallogsRepo.save(withdrawalLogs);
                        }
                    }
                });
    }
}
