package com.eclectics.chamapayments.resource.mobile;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.wrappers.request.CreateaccounttypeWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Optional;

import static com.eclectics.chamapayments.service.constants.StringConstantsUtil.PHONE_NUMBER_MATCH;

@RestController
@RequestMapping("/api/v2/payment/account")
@RequiredArgsConstructor
public class AccountingResource {

    private final AccountingService accountingService;

    @GetMapping("/balance")
    public Mono<ResponseEntity<UniversalResponse>> getMemberWalletBalance() {
        return accountingService.userWalletBalance().map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/group-balance/{gid}")
    public Mono<ResponseEntity<UniversalResponse>> getGroupAccountBalance(@PathVariable("gid") Long groupId) {
        return accountingService.groupAccountBalance(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/types")
    public Mono<ResponseEntity<UniversalResponse>> getAccountTypes() {
        return Mono.fromCallable(accountingService::findAccountTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("success", "Account types", res)));
    }

    @GetMapping("/type")
    public Mono<ResponseEntity<UniversalResponse>> getAccountType(@RequestParam String name) {
        return Mono.fromCallable(() -> accountingService.getAccounttypebyName(name))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("00", "success  ", res)));
    }

    @GetMapping("/group-account")
    //@PreAuthorize("hasPermission( #groupid,'group',@objectAction.initFields('groupaccount','canview'))")
    @ApiOperation(value = "get the accounts attached to a group, the optional parameter `accounttypeid` allows filter by accounttype")
    public Mono<ResponseEntity<UniversalResponse>> getGroupAccounts(@RequestParam long groupid, @RequestParam Optional<Long> accountypeid) {
        if (accountypeid.isPresent()) {
            return Mono.fromCallable(() -> accountingService.getGroupAccountsByType(groupid, accountypeid.get()))
                    .publishOn(Schedulers.boundedElastic())
                    .map(res -> ResponseEntity.ok().body(res));
        }
        return Mono.fromCallable(() -> accountingService.getAccountbyGroup(groupid))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("success", "Group accounts found", res)));
    }

    @PostMapping("/create-type")
    @ApiOperation(value = "create a new account type")
    public Mono<ResponseEntity<UniversalResponse>> createAccountType(@RequestBody @Valid CreateaccounttypeWrapper createaccounttypeWrapper) {
        return Mono.fromCallable(() -> accountingService.createNewAccountType(createaccounttypeWrapper.getName(), createaccounttypeWrapper.getPrefix(),
                        createaccounttypeWrapper.getRequiredfields()))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/contribution-types")
    @ApiOperation(value = "get the types of contributions")
    public Mono<ResponseEntity<UniversalResponse>> getContributionTypes() {
        return Mono.fromCallable(accountingService::getContributionTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/schedule-types")
    @ApiOperation(value = "get the schedules supported on the platform")
    public Mono<ResponseEntity<UniversalResponse>> getScheduleTypes() {
        return Mono.fromCallable(accountingService::getScheduleTypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/amount-types")
    @ApiOperation(value = "get the amount supported on the platform")
    public Mono<ResponseEntity<UniversalResponse>> getAmountTypes() {
        return Mono.fromCallable(accountingService::getAmounttypes)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("success", "Amount types", res)));
    }

    @GetMapping("/user-groups")
    public Mono<ResponseEntity<?>> getGroupAccountsMemberBelongsTo() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(accountingService::getGroupAccountsMemberBelongsTo)
                .map(res -> ResponseEntity.ok().body(new UniversalResponse("success", "Amount types", res)));
    }

    @GetMapping("/transactions/{filter}")
    @ApiOperation(value = "Fetch transactions using a filter",
            notes = "The filters applicable include: group, user, userandcontribution and userandgroup. The filter id may be for the group id or contribution id")
    public Mono<ResponseEntity<?>> getTransactions(@PathVariable String filter, @RequestParam Optional<Long> filterId, @RequestParam Integer page, @RequestParam Integer size) {
        switch (filter) {
            case "group":
                return accountingService.getGroupTransactions(filterId.get(), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactions(username, page, size))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "userandcontribution":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactionsByContribution(username, filterId.get(), page, size))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "userandgroup":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> accountingService.getUserTransactionsByGroup(username, filterId.get(), page, size))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(new UniversalResponse("fail", "Wrong filter provided")));
        }
    }

    @GetMapping("/user-summary")
    public Mono<ResponseEntity<?>> getUserAccountingSummary(@RequestParam String phone, @RequestParam Long contributionId) {
        return accountingService.getUserSummary(phone, contributionId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
