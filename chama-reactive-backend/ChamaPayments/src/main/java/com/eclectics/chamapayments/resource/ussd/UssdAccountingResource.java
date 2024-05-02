package com.eclectics.chamapayments.resource.ussd;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.wrappers.request.*;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v2/payment/ussd/account")
@RequiredArgsConstructor
public class UssdAccountingResource {

    private final AccountingService accountingService;

    /**
     * Used by KYC to fetch the group account and contributions.
     * @return the account info and contributions info
     */
    @ApiIgnore
    @GetMapping("/group-info")
    public Mono<ResponseEntity<UniversalResponse>> fetchGroupAccountAndContributions(@RequestParam Long groupId) {
        return accountingService.fetchGroupAccountAndContributions(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/balance")
    public Mono<ResponseEntity<UniversalResponse>> getMemberWalletBalance() {
        return accountingService.userWalletBalance().map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/types")
    public Mono<ResponseEntity<UniversalResponse>> getAccountTypes() {
        return Mono.fromCallable(accountingService::findAccountTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("Success", "Account types", res)));
    }

    @PostMapping("/type")
    public Mono<ResponseEntity<UniversalResponse>> getAccountType(@RequestBody AccountTypeRequest request) {
        return Mono.fromCallable(() -> accountingService.getAccounttypebyName(request.getAccountName()))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("00", "Success", res)));
    }

    @PostMapping("/group-account")
    @ApiOperation(value = "get the accounts attached to a group, the optional parameter `accounttypeid` allows filter by accounttype")
    public Mono<ResponseEntity<UniversalResponse>> getGroupAccounts(@RequestBody GroupAccountsRequest request) {
        if (request.getAccountypeId().isPresent()) {
            return Mono.fromCallable(() -> accountingService.getGroupAccountsByType(request.getGroupId(), request.getAccountypeId().get()))
                    .publishOn(Schedulers.boundedElastic())
                    .map(res -> ResponseEntity.ok().body(res));
        }
        return Mono.fromCallable(() -> accountingService.getAccountbyGroup(request.getGroupId()))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("Success", "Group accounts found", res)));
    }

    @PostMapping("/create-type")
    @ApiOperation(value = "create a new account type")
    public Mono<ResponseEntity<UniversalResponse>> createAccountType(@RequestBody @Valid CreateaccounttypeWrapper createaccounttypeWrapper) {
        return Mono.fromCallable(() -> accountingService.createNewAccountType(createaccounttypeWrapper.getName(), createaccounttypeWrapper.getPrefix(),
                        createaccounttypeWrapper.getRequiredfields()))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/contribution-types")
    @ApiOperation(value = "get the types of contributions")
    public Mono<ResponseEntity<UniversalResponse>> getContributionTypes() {
        return Mono.fromCallable(accountingService::getContributionTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/schedule-types")
    @ApiOperation(value = "get the schedules supported on the platform")
    public Mono<ResponseEntity<UniversalResponse>> getScheduleTypes() {
        return Mono.fromCallable(accountingService::getScheduleTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/amount-types")
    @ApiOperation(value = "get the amount supported on the platform")
    public Mono<ResponseEntity<UniversalResponse>> getAmountTypes() {
        return Mono.fromCallable(accountingService::getAmounttypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("Success", "Amount types", res)));
    }

    @GetMapping("/transactions")
    @ApiOperation(value = "Fetch transactions using a filter",
            notes = "The filters applicable include: group, user, userandcontribution and userandgroup. The filter id may be for the group id or contribution id")
    public Mono<ResponseEntity<?>> getTransactions(@RequestBody TransactionRequest request) {
        switch (request.getFilter()) {
            case "group":
                return accountingService.getGroupTransactions(request.getFilterId().get(), request.getPage(), request.getPage())
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactions(username, request.getPage(), request.getPage()))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "userandcontribution":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactionsByContribution(username, request.getFilterId().get(), request.getPage(), request.getPage()))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "userandgroup":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactionsByGroup(username, request.getFilterId().get(), request.getPage(), request.getPage()))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(new UniversalResponse("fail", "Wrong filter provided")));
        }
    }

    @PostMapping("/user-summary")
    public Mono<ResponseEntity<?>> getUserAccountingSummary(@RequestBody UserSummaryRequest userSummaryRequest) {
        return accountingService.getUserSummary(userSummaryRequest.getPhone(), Long.valueOf(userSummaryRequest.getContributionId()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
