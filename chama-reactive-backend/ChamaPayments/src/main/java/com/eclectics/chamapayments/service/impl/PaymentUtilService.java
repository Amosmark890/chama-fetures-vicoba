package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.repository.*;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.NotificationService;
import com.eclectics.chamapayments.service.PaymentUtil;
import com.eclectics.chamapayments.service.RouterService;
import com.eclectics.chamapayments.service.enums.PaymentEnum;
import com.eclectics.chamapayments.wrappers.esbWrappers.EsbmessageWrapper;
import com.eclectics.chamapayments.wrappers.esbWrappers.TransactionDetails;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Alex Maina
 * @created 24/12/2021
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentUtilService implements PaymentUtil {
    private final PenaltyRepository penaltyRepository;
    private final NotificationService notificationService;
    private final ContributionsPaymentRepository contributionsPaymentRepository;
    private final ChamaKycService chamaKycService;
    private final WebClient webClient;
    //    private final TransactionLogger transactionLogger;
    private final ContributionRepository contributionRepository;
    private final AccountsRepository accountsRepository;
    private final TransactionlogsRepo transactionlogsRepo;
    @Autowired
    @Lazy
    private RouterService routerService;
    private final LoanPenaltyRepository loanPenaltyRepository;
    private final LoanPenaltyPaymentRepository loanPenaltyPaymentRepository;
    private final LoanrepaymentpendingapprovalRepo loanrepaymentpendingapprovalRepo;
    private final LoansdisbursedRepo loansdisbursedRepo;
    private final LoansrepaymentRepo loansrepaymentRepo;


    @Value("${app-configs.esb-url}")
    public String esbUrl;
    @Value("${wallet.gl_account_number}")
    private String glAccount;
    @Value("${wallet.gl_phonenumber}")
    private String gl_phonenumber;

    @Override
    public String addFTWallet(String phoneNumber, String creditAccount, String debitAccount, double amount) {
        TransactionDetails transactiondetails = new TransactionDetails();
        transactiondetails.setPrefix("FT");
        transactiondetails.setPhone_number(phoneNumber);
        transactiondetails.setAmount(amount);
        transactiondetails.setDirection("REQUEST");
        transactiondetails.setHost_code("MM");
        transactiondetails.setTransaction_type("FT");
        transactiondetails.setTransaction_code("FTMM");
        transactiondetails.setDebit_account(debitAccount);
        transactiondetails.setCredit_account(creditAccount);
        transactiondetails.setCurrency("KES");
        transactiondetails.setHost_code("MM");

        Map<String, Object> response = sendMessage(transactiondetails);

        return response.get("response").toString();
    }

    @Override
    public Map<String, Object> sendMessage(TransactionDetails message) {
        EsbmessageWrapper esbmessageWrapper = new EsbmessageWrapper(message);
        Map<String, Object> responsecontainer = new HashMap<>();
        Mono<String> responseMono = webClient.post()
                .uri(esbUrl)
                .body(esbmessageWrapper, EsbmessageWrapper.class)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError, response -> {
                    response.bodyToMono(String.class)
                            .doOnNext(res -> {
                                JsonObject json = JsonParser.parseString(res).getAsJsonObject();
//                                transactionLogger.logResponsetoDB(esbmessageWrapper.getXref(), false, String.valueOf(response.statusCode().value()), readResponsecode(json));
                                responsecontainer.put("status", false);

                            });
                    return null;
                })
                .onStatus(HttpStatus::is2xxSuccessful, response -> {
                    response.bodyToMono(String.class)
                            .doOnNext(res -> {
                                JsonObject json = JsonParser.parseString(res).getAsJsonObject();
//                                transactionLogger.logResponsetoDB(esbmessageWrapper.getXref(), true, String.valueOf(response.statusCode()), readResponsecode(json));
                                responsecontainer.put("status", true);
                                responsecontainer.put("response", json);
                            });
                    return null;
                })
                .onStatus(HttpStatus::is5xxServerError, response -> {
                    response.bodyToMono(String.class)
                            .doOnNext(res -> {
//                                transactionLogger.logResponsetoDB(esbmessageWrapper.getXref(), false, "internal server error", "500");
                                responsecontainer.put("status", false);
                            });
                    return null;
                })
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(4, Duration.ofSeconds(2)));

        return responseMono.map(res -> responsecontainer).block();
    }

    private String readResponsecode(JsonObject jsonresponse) {
        if (jsonresponse.keySet().contains("response_code")) {
            //create account transaction
            return jsonresponse.get("response_code").getAsString();
        } else {
            //FT transaction
            return jsonresponse.getAsJsonObject("data").getAsJsonObject("response").get("response_code").getAsString();
        }
    }

    @Async
    @Override
    public void extractMpesaResponse(JsonObject jsonObject, ContributionPayment contributionPayment) {
        log.info("===RECEIVED RESPONSE MPESA" + jsonObject.toString());
        if (jsonObject.get("STATUS").getAsString().equals("99")) {
            return;
        }
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(contributionPayment.getPhoneNumber());
        if (!jsonObject.get("STATUS").getAsString().equals("00")) {
            if (contributionPayment.getIsPenalty()) {
                Penalty penalty = penaltyRepository.findById(contributionPayment.getPenaltyId()).get();
                penalty.setPaid(false);
                penaltyRepository.save(penalty);
                notificationService.sendPenaltyFailureMessage(contributionPayment.getPhoneNumber(), memberWrapper.getFirstname(), contributionPayment.getAmount(), memberWrapper.getLanguage());
                return;
            }
            contributionPayment.setPaymentStatus(PaymentEnum.PAYMENT_FAILED.name());
            contributionsPaymentRepository.save(contributionPayment);
            try {
                notificationService.sendContributionFailureMessage(contributionPayment.getPhoneNumber(), memberWrapper.getFirstname(), contributionPayment.getAmount(), memberWrapper.getLanguage());
            } catch (Exception e){
                log.error("sending contribution message failure{}",e.getMessage());
            }

            log.info("===== SAVED FAILED");
        } else {
            JsonObject checkoutData = jsonObject.get("MPESAPAYLOAD")
                    .getAsJsonObject();
            String paymentId = "";
            if (checkoutData.has("Body")) {
                checkoutData = checkoutData.getAsJsonObject().get("Body").getAsJsonObject().get("stkCallback").getAsJsonObject()
                        .get("CallbackMetadata").getAsJsonObject();
                JsonArray jsonObjects = checkoutData.get("Item").getAsJsonArray();
                JsonObject jsonObject1 = jsonObjects.get(1).getAsJsonObject();

                paymentId = jsonObject1.get("Value").getAsString();
            }
            if (contributionPayment.getIsPenalty()) {
                Penalty penalty = penaltyRepository.findById(contributionPayment.getPenaltyId()).get();
                penalty.setPaid(true);
                penaltyRepository.save(penalty);
            }
            contributionPayment.setPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());
            contributionPayment.setMpesaPaymentId(paymentId);
            contributionsPaymentRepository.save(contributionPayment);
            try {
                notificationService.sendContributionSuccessMessage(contributionPayment.getPhoneNumber(), memberWrapper.getFirstname(), contributionPayment.getAmount(), memberWrapper.getLanguage());
            } catch (Exception err){
                log.error("contribution success sms failure-------{}",err.getMessage());
            }

            log.info("===== SAVED SUCCESS");
            debitUserAccount(contributionPayment.getPhoneNumber(), contributionPayment.getContributionId(), contributionPayment.getAmount());
            addNewTransaction(contributionPayment.getPhoneNumber(), contributionPayment.getContributionId(), contributionPayment.getAmount(), false);
        }

    }

    private String extractUserWalletAccountId(String phoneNumber) {
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);
        return memberWrapper.getEsbwalletaccount();
    }

    private void debitUserAccount(String phoneNumber, Long contributionId, int amount) {
        String userWalletAccountNumber = extractUserWalletAccountId(phoneNumber);
        //debit GL credit user
        String response;
        JsonObject jsonObject;
        String statusCode;
        log.info("SENDING DATA TO WALLET ==> CREDIT MEMBER");
        response = addFTWallet(gl_phonenumber, userWalletAccountNumber, glAccount, (long) amount);
        log.info("REPONSE 1 ==> " + response);
        jsonObject = JsonParser.parseString(response).getAsJsonObject();
        statusCode = getStatusCode(jsonObject);
        if (!statusCode.equals("00")) {
            //todo
            log.info("ERROR SENDING DATA TO WALLET ==> CREDIT MEMBER");
            //record an error
            return;
        }
        //debit user credit
        String groupAccountNumber = extractGroupWalletAccount(contributionId);
        log.info("SENDING DATA TO WALLET ==> CREDIT GROUP");
        response = addFTWallet(String.valueOf(phoneNumber), groupAccountNumber, userWalletAccountNumber, (long) amount);

        log.info("REPONSE 2 ==> " + response);
        jsonObject = JsonParser.parseString(response).getAsJsonObject();
        statusCode = getStatusCode(jsonObject);
        if (!statusCode.equals("00")) {
            //todo
            log.info("ERROR SENDING DATA TO WALLET ==> CREDIT GROUP");
            //record an error
        }
    }

    private String getStatusCode(JsonObject object) {
        return object.get("data").getAsJsonObject().get("response").getAsJsonObject().get("response_code").getAsString();
    }

    private Optional<GroupWrapper> getGroups(Long contributionId) {
        Optional<Contributions> contributionsOptional = contributionRepository.findById(contributionId);
        if (contributionsOptional.isEmpty()) {
            return Optional.empty();
        }
        //credit user account
        //debit userAccount
        //credit
        Contributions contributions = contributionsOptional.get();
        return Optional.ofNullable(chamaKycService.getMonoGroupById(contributions.getMemberGroupId()));
    }

    private String extractGroupWalletAccount(Long contributionId) {
        Optional<GroupWrapper> optionalGroups = getGroups(contributionId);
        if (optionalGroups.isEmpty()) return null;
        GroupWrapper groups = optionalGroups.get();
        Optional<Accounts> optionalGroupIdWallet = accountsRepository.findByGroupIdWallet(groups.getId());
        if (optionalGroupIdWallet.isEmpty()) return null;
        String accountsDetails = optionalGroupIdWallet.get().getAccountdetails();
        JsonObject jsonObject = JsonParser.parseString(accountsDetails).getAsJsonObject();
        return jsonObject.get("account_number").getAsString();
    }

    /**
     * log transaction
     *
     * @param phoneNumber
     * @param contributionId
     * @param amount
     */
    private void addNewTransaction(String phoneNumber, Long contributionId, int amount, boolean isLoan) {
        Optional<Contributions> optionalContribution = contributionRepository.findById(contributionId);
        if (optionalContribution.isEmpty()) {
            log.error("New Transaction Log error , Cannot find contribution id {}", contributionId);
            return;
        }
        Contributions contributions = optionalContribution.get();
        Optional<GroupWrapper> optionalGroups = getGroups(contributions.getMemberGroupId());
        if (optionalGroups.isEmpty()) {
            log.error(" Cannot find group for contribution id {} ", contributionId);
        }
        GroupWrapper groups = optionalGroups.get();
        Accounts accounts = accountsRepository.findByGroupIdAndActive(groups.getId(), true).get(0);
        TransactionsLog transactionsLog = new TransactionsLog();
        if (!isLoan) {
            transactionsLog.setContributionNarration("Contribution payment. Member with phone number " + phoneNumber + " contributed amount " + amount + " to contribution  " + optionalContribution.get().getName());
        } else {
            transactionsLog.setContributionNarration("Loan repayment. Member with phone number " + phoneNumber + " paid loan of  " + amount + " to contribution  " + optionalContribution.get().getName());
        }
        transactionsLog.setCreditaccounts(accounts);
        transactionsLog.setDebitphonenumber(String.valueOf(phoneNumber));
        String transid = accounts.getAccountType().getAccountPrefix().concat(String.valueOf(new Date().getTime()));
        transactionsLog.setUniqueTransactionId(transid);
        transactionsLog.setOldbalance(accounts.getAccountbalance());
        transactionsLog.setNewbalance(accounts.getAccountbalance() + amount);
        transactionsLog.setTransamount(amount);
        transactionsLog.setCapturedby("mpesa");
        transactionsLog.setApprovedby("mpesa");
        transactionsLog.setContributions(optionalContribution.get());
        accounts.setAccountbalance(transactionsLog.getNewbalance());

        accountsRepository.save(accounts);
        transactionlogsRepo.save(transactionsLog);
    }

    @Override
    public void extractLoanPaymentPenalty(JsonObject jsonObject, LoanPenaltyPayment loanPenaltyPayment) {
        log.info("===RECEIVED LOAN PENALTY RESPONSE MPESA" + jsonObject.toString());
        if (jsonObject.get("STATUS").getAsString().equals("00")) {
            JsonObject checkoutData = jsonObject.get("MPESAPAYLOAD")
                    .getAsJsonObject();
            String paymentId = "";
            if (checkoutData.has("Body")) {
                checkoutData = checkoutData.getAsJsonObject().get("Body").getAsJsonObject().get("stkCallback").getAsJsonObject()
                        .get("CallbackMetadata").getAsJsonObject();

                JsonArray jsonObjects = checkoutData.get("Item").getAsJsonArray();
                JsonObject jsonObject1 = jsonObjects.get(1).getAsJsonObject();

                paymentId = jsonObject1.get("Value").getAsString();
            }

            loanPenaltyPayment.setPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());
            loanPenaltyPayment.setReceiptNumber(paymentId);

            LoanPenalty loanPenalty = loanPenaltyPayment.getLoanPenalty();

            Double dueAmount = loanPenalty.getDueAmount() - loanPenaltyPayment.getPaidAmount();
            loanPenalty.setDueAmount(dueAmount);
            loanPenaltyRepository.save(loanPenalty);
            loanPenaltyPaymentRepository.save(loanPenaltyPayment);

            MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loanPenalty.getLoansDisbursed().getLoanApplications().getMemberId());
            String phoneNumber = memberWrapper.getPhonenumber();
            Contributions contributions = loanPenalty.getLoansDisbursed().getLoanApplications().getLoanProducts().getContributions();
            debitUserAccount(phoneNumber, contributions.getId(), loanPenaltyPayment.getPaidAmount().intValue());
            addNewTransaction(phoneNumber, contributions.getId(), loanPenaltyPayment.getPaidAmount().intValue(), true);

        }
    }

    @Override
    public void extractLoanPaymentResponse(JsonObject jsonObject, LoanRepaymentPendingApproval loanRepaymentPendingApproval) {
        log.info("===RECEIVED LOAN PAYMENT RESPONSE MPESA" + jsonObject.toString());
        if (jsonObject.get("STATUS").getAsString().equals("00")) {
            loanRepaymentPendingApproval.setPending(false);
            loanRepaymentPendingApproval.setApprovedby("mpesa");
            loanRepaymentPendingApproval.setApproved(true);
            loanrepaymentpendingapprovalRepo.save(loanRepaymentPendingApproval);

            JsonObject checkoutData = jsonObject.get("MPESAPAYLOAD")
                    .getAsJsonObject();
            String paymentId = "";
            if (checkoutData.has("Body")) {
                checkoutData = checkoutData.getAsJsonObject().get("Body").getAsJsonObject().get("stkCallback").getAsJsonObject()
                        .get("CallbackMetadata").getAsJsonObject();

                JsonArray jsonObjects = checkoutData.get("Item").getAsJsonArray();
                JsonObject jsonObject1 = jsonObjects.get(1).getAsJsonObject();

                paymentId = jsonObject1.get("Value").getAsString();
            }

            LoansRepayment loansRepayment = new LoansRepayment();
            LoansDisbursed loansDisbursed = loanRepaymentPendingApproval.getLoansDisbursed();
            double newdueamount = loansDisbursed.getDueamount() - loanRepaymentPendingApproval.getAmount();

            loansRepayment.setAmount(loanRepaymentPendingApproval.getAmount());
            loansRepayment.setLoansDisbursed(loansDisbursed);
            loansRepayment.setMemberId(loanRepaymentPendingApproval.getMemberId());
            loansRepayment.setNewamount(newdueamount);
            loansRepayment.setOldamount(loansDisbursed.getDueamount());
            loansRepayment.setReceiptnumber(paymentId);
            loansRepayment.setPaymentType("MPESA");

            loansDisbursed.setDueamount(newdueamount);
            loansdisbursedRepo.save(loansDisbursed);
            loansrepaymentRepo.save(loansRepayment);
            MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loansDisbursed.getLoanApplications().getMemberId());
            String phoneNumber = memberWrapper.getPhonenumber();
            Contributions contributions = loansDisbursed.getLoanApplications().getLoanProducts().getContributions();
            //log transaction
            debitUserAccount(phoneNumber, contributions.getId(), ((int) loansRepayment.getAmount()));
            addNewTransaction(phoneNumber, contributions.getId(), ((int) loansRepayment.getAmount()), true);
        }

    }

    @Override
    public void creditUserWalletAccount(String phoneNumber, long contributionId, double amount) {
        Optional<GroupWrapper> optionalGroups = getGroups(contributionId);
        if (optionalGroups.isEmpty()) {
            return;
        }

        GroupWrapper groups = optionalGroups.get();
        String userWalletAccountNumber = extractUserWalletAccountId(phoneNumber);
        String groupAccountNumber = extractGroupWalletAccount(contributionId);

        String response;
        JsonObject jsonObject;
        String statusCode;

        //debit group account credit gl
        log.info("SENDING MONEY FROM GROUP ACCOUNT TO GL ACCOUNT");
        response = addFTWallet(String.valueOf(groups.getId()), glAccount, groupAccountNumber, amount);
        jsonObject = JsonParser.parseString(response).getAsJsonObject();
        statusCode = getStatusCode(jsonObject);
        if (!statusCode.equals("00")) {
            //todo
            log.info("ERROR SENDING MONEY TO GL ACCOUNT");
            //record an error
            return;
        }
        //debit gl credit user account
        log.info("SENDING MONEY FROM  GL ACCOUNT TO USER ACCOUNT");
        response = addFTWallet(gl_phonenumber, userWalletAccountNumber, glAccount, amount);
        jsonObject = JsonParser.parseString(response).getAsJsonObject();
        statusCode = getStatusCode(jsonObject);
        if (!statusCode.equals("00")) {
            //todo
            log.info("ERROR SENDING MONEY TO user account");
            //record an error
            return;
        }

    }
}
