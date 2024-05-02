package com.eclectics.chamapayments.resource.mobile;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.service.CallbackServicePublisher;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.enums.CanTransact;
import com.eclectics.chamapayments.wrappers.request.*;
import com.eclectics.chamapayments.wrappers.response.ContributionDetailsWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v2/payment/contribution")
@RequiredArgsConstructor
public class ContributionsResource {

    private final Gson gson;
    private final ChamaKycService chamaKycService;
    private final AccountingService accountingService;
    private final CallbackServicePublisher callbackServicePublisher;

    @PostMapping("/new")
    public Mono<ResponseEntity<?>> createContribution(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return accountingService.addContribution(contributionDetailsWrapper)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/edit")
    @PreAuthorize("hasPermission(#contributionDetailsWrapper.getGroupid(), 'group', @objectAction.initFields('groupdetails','canedit'))")
    public Mono<ResponseEntity<?>> editContribution(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.editContribution(contributionDetailsWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/user-loan-limit")
    @ApiOperation(value = "Get users loan limit", notes = "Pass the group id, contribution id and loan product id")
    public Mono<ResponseEntity<UniversalResponse>> getUserLoanLimit(@RequestBody LoanLimitRequest loanLimitRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.checkLoanLimit(username, loanLimitRequest.getGroupId(), loanLimitRequest.getContributionId(), loanLimitRequest.getLoanProductId()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/pay-for-other")
    @PreAuthorize("hasPermission(#dto.getGroupId() == null ? 0 : #dto.getGroupId(), 'group', @objectAction.initFields('contributionpayment','cancreate'))")
    @CanTransact
    public Mono<ResponseEntity<UniversalResponse>> makeContributionForOther(@RequestBody ContributionPaymentDto dto, @ModelAttribute String username) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(walletAccount -> accountingService.makeContributionForOtherMember(dto, walletAccount))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/payment")
    @PreAuthorize("hasPermission(#dto.getGroupId() == null ? 0 : #dto.getGroupId, 'group', @objectAction.initFields('contributionpayment','cancreate'))")
    @CanTransact
    public Mono<ResponseEntity<UniversalResponse>> makeContribution(@RequestBody ContributionPaymentDto dto, @ModelAttribute String username) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(walletAccount -> accountingService.makeContribution(dto, walletAccount))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/penalty-payment")
    @PreAuthorize("hasPermission(#dto.getGroupId() == null ? 0 : #dto.getGroupId, 'group', @objectAction.initFields('contributionpayment','cancreate'))")
    @CanTransact
    public Mono<ResponseEntity<UniversalResponse>> payForContributionPenalty(@RequestBody ContributionPaymentDto dto, @ModelAttribute String username) {
        return accountingService.payForContributionPenalty(dto, username)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @PostMapping("/ft-callback")
    public Mono<ResponseEntity<?>> fundsTransferCallback(@RequestBody String body) {
        callbackServicePublisher.publishCallback(body);
        return Mono.just(ResponseEntity.ok().build());
    }

    @PostMapping("/withdraw")
    @CanTransact
    public Mono<ResponseEntity<UniversalResponse>> recordWithdrawal(@RequestBody @Valid RequestwithdrawalWrapper
                                                                            requestwithdrawalWrapper, @ModelAttribute String username) {
        return accountingService.recordWithdrawal(requestwithdrawalWrapper, username)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasPermission(#request.getGroupId(), 'group', @objectAction.initFields('withdrawalapproval','canedit'))")
    public Mono<ResponseEntity<?>> approveContributionWithdrawal(@RequestBody WithdrawalApprovalRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.approveWithdrawalRequest(request.getRequestId(), request.getApprove(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/approve-receipt-payment")
    @ApiOperation(value = "Approve contribution made by receipt")
    @PreAuthorize("hasPermission(#request.getGroupId(), 'group', @objectAction.initFields('withdrawalapproval','canview'))")
    public Mono<ResponseEntity<?>> approveContributionByReceipt(@RequestBody WithdrawalApprovalRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.approveContributionPayment(request.getRequestId(), request.getApprove(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-pending-withdrawal")
    @PreAuthorize("hasPermission(#request.getGroupId(), 'group', @objectAction.initFields('withdrawalapproval','canview'))")
    public Mono<ResponseEntity<?>> getPendingWithdrawals(@RequestBody PendingWithdrawalsRequest request) {
        return Mono.fromCallable(() -> accountingService.getPendingWithdrawalRequestByGroupId(request.getGroupId(),
                        request.getPage(), request.getSize()))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new UniversalResponse("success", "withdrawals pending approval", res.getContent())));
    }

    @GetMapping("/pending-withdrawal")
    public Mono<ResponseEntity<?>> getUserPendingWithdrawals() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> Mono.fromCallable(() -> accountingService.getPendingWithdrawalRequestbyUser(username)).publishOn(Schedulers.boundedElastic()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user-contribution-payment")
    public Mono<ResponseEntity<?>> getUserContributionPayments() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getUserContributionPayments)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-contribution-payment")
    public Mono<ResponseEntity<?>> getGroupContributionPayments(@RequestBody GroupContributionsRequest request) {
        return accountingService.getGroupContributionPayments(request.getContributionId(), request.getPage(), request.getSize())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/upcoming/{groupId}")
    public Mono<ResponseEntity<?>> getUserUpcomingContributionPayments(@PathVariable Long groupId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.getUserUpcomingPayments(username, groupId))
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/upcoming/user")
    public Mono<ResponseEntity<?>> getUserUpcomingContributionPayment() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getUserUpcomingPayments)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/upcoming")
    public Mono<ResponseEntity<?>> getAllUserUpcomingContributionPayments() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getAllUserUpcomingPayments)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/members")
    public Mono<ResponseEntity<?>> getMembers() {
        return Mono.fromCallable(chamaKycService::getFluxMembers)
                .flatMap(Flux::collectList)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PutMapping("/update-contribution")
    public Mono<ResponseEntity<?>> updateContribution(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.updateContribution(contributionDetailsWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user/analytics")
    public Mono<ResponseEntity<?>> getUserTotalContributionsInGroup() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getUserContributionsPerGroup)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/penalties")
    public Mono<ResponseEntity<?>> getMemberContributionPenalties() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getAllMemberPenalties)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group/penalties")
    public Mono<ResponseEntity<?>> getGroupContributionPenalties(@RequestParam long groupId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return accountingService.getGroupContributionPenalties(groupId, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/all")
    public Mono<ResponseEntity<?>> getContributionsInGroup(@RequestParam Long groupId) {
        return accountingService.getGroupContributions(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/{cid}")
    public Mono<ResponseEntity<?>> getContribution(@PathVariable Long cid) {
        return accountingService.getGroupContribution(cid)
                .doOnNext(res -> log.info(res.getStatus()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/contribution-receipt-payment", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<?>> addContributionReceiptPayment(
            @RequestPart("payment-details") String body, @RequestPart("file") Mono<FilePart> file) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> file.flatMap(receipt -> {
                    MakecontributionWrapper makecontributionWrapper = gson.fromJson(body, MakecontributionWrapper.class);

                    return accountingService.addContributionReceiptPayment(makecontributionWrapper, receipt, username);
                })).map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-pending-receipt-payments")
    public Mono<ResponseEntity<?>> getPendingPaymentApprovalByGroup(@RequestParam Long groupId) {
        return Mono.fromCallable(() -> accountingService.getPendingPaymentApprovalByGroupId(groupId))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", "Receipt payments pending approval", res))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/pending-receipt-payments")
    public Mono<ResponseEntity<?>> getPendingPaymentApprovalByUser(@RequestParam Long groupId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> Mono.fromCallable(() -> accountingService.getPendingPaymentApprovalByUser(username)).publishOn(Schedulers.boundedElastic()))
                .map(res -> new UniversalResponse("success", "Receipt payments pending approval", res))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }


    @PostMapping("/user-overpaid-contribution")
    public Mono<ResponseEntity<?>> getUserOverpaidContributions() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getOverpaidContributions)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }
//
//    @PostMapping("/user-outstanding-contribution")
//    public Mono<ResponseEntity<?>> getUserOutstandingContributions( ) {
//        return CustomAuthenticationUtil.getUsername()
//                .flatMap(accountingService::getOutstandingContributio)
//                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
//    }

}
