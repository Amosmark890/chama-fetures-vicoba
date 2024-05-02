package com.eclectics.chamapayments.resource.ussd;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.service.enums.CanTransact;
import com.eclectics.chamapayments.wrappers.request.*;
import com.eclectics.chamapayments.wrappers.response.ContributionDetailsWrapper;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/payment/ussd/contribution")
public class UssdContributionsResource {

    private final AccountingService accountingService;

    @PostMapping(value = "/user-loan-limit")
    @ApiOperation(value = "Get users loan limit")
    public Mono<ResponseEntity<UniversalResponse>> getUsersLoanLimit(@RequestBody LoanLimitRequest request) {

        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.checkLoanLimit(username, request.getGroupId(),
                        request.getContributionId(), request.getLoanProductId()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @CanTransact
    @PostMapping("/payment")
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

    @PostMapping("/withdraw")
    public Mono<ResponseEntity<UniversalResponse>> recordWithdrawal(@RequestBody @Valid RequestwithdrawalWrapper requestwithdrawalWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.recordWithdrawal(requestwithdrawalWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/approve")
    @PreAuthorize("hasPermission(#request.getGroupId(), 'group', @objectAction.initFields('withdrawalapproval','canview'))")
    public Mono<ResponseEntity<?>> approveContributionWithdrawal(@RequestBody WithdrawalApprovalRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.approveWithdrawalRequest(request.getRequestId(), request.getApprove(), username))
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

    @PostMapping("/pending-withdrawal")
    public Mono<ResponseEntity<?>> getUserPendingWithdrawals() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> Mono.fromCallable(() -> accountingService.getPendingWithdrawalRequestbyUser(username))
                        .publishOn(Schedulers.boundedElastic()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/user-contribution-payment")
    public Mono<ResponseEntity<?>> getUserContributionPayments() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getUserContributionPayments)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-contribution-payment")
    public Mono<ResponseEntity<?>> getGroupContributionPayments(@RequestBody GroupContributionsRequest request) {
        return accountingService.getUssdGroupContributionPayments(request.getContributionId(), request.getPage(), request.getSize())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update-contribution")
    public Mono<ResponseEntity<?>> updateContribution(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.updateContribution(contributionDetailsWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/upcoming")
    public Mono<ResponseEntity<?>> getUserUpcomingContributionPayments(@RequestBody GroupWrapper groupWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> accountingService.getUserUpcomingPayments(username, groupWrapper.getId()))
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/pay-for-other")
    @PreAuthorize("hasPermission(#dto.getGroupId() == null ? 0 : #dto.getGroupId(), 'group', @objectAction.initFields('contributionpayment','cancreate'))")
    @CanTransact
    public Mono<ResponseEntity<UniversalResponse>> makeContributionForOther(@RequestBody ContributionPaymentDto dto, @ModelAttribute String username) {
        return accountingService.makeContributionForOtherMember(dto, username)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/all")
    @ApiOperation(value = "Get contributions in a group", notes = "Just pass the group id")
    public Mono<ResponseEntity<?>> getContributionsInGroup(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return accountingService.getGroupContributions(contributionDetailsWrapper.getGroupid())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/details")
    @ApiOperation(value = "Get contributions in a group", notes = "Just pass the id")
    public Mono<ResponseEntity<?>> getContribution(@RequestBody ContributionDetailsWrapper contributionDetailsWrapper) {
        return accountingService.getGroupContribution(contributionDetailsWrapper.getId())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/penalties")
    public Mono<ResponseEntity<?>> getMemberContributionPenalties() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getAllMemberPenalties)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
