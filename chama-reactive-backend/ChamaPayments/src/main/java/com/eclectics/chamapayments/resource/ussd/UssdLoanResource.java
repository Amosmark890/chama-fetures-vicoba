package com.eclectics.chamapayments.resource.ussd;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.LoanService;
import com.eclectics.chamapayments.wrappers.request.*;
import com.eclectics.chamapayments.wrappers.response.LoanproductWrapper;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v2/payment/ussd/loan")
@RequiredArgsConstructor
public class UssdLoanResource {

    private final LoanService loanService;

    @PostMapping("/apply")
    public Mono<ResponseEntity<?>> applyLoan(@RequestBody ApplyLoanWrapper applyLoanWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.applyLoan(applyLoanWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/approve")
    @PreAuthorize("hasPermission(#approveLoanWrapper.getGroupid(),'loanapplication',@objectAction.initFields('loans','canedit'))")
    public Mono<ResponseEntity<?>> approveLoanApplication(@RequestBody @Valid ApproveLoanWrapper approveLoanWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.approveLoanApplication(approveLoanWrapper.isApprove(), approveLoanWrapper.getLoanapplicationid(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/create-product")
    @PreAuthorize("hasPermission(#loanproductWrapper.getGroupid(),'group',@objectAction.initFields('loanproduct','cancreate'))")
    public Mono<ResponseEntity<?>> createLoanProduct(@RequestBody @Valid LoanproductWrapper loanproductWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.createLoanProduct(loanproductWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/product-edit")
    @PreAuthorize("hasPermission(#loanproductWrapper.getGroupid(),'group',@objectAction.initFields('loanproduct','canedit'))")
    public Mono<ResponseEntity<?>> editLoanProduct(@RequestBody LoanproductWrapper loanproductWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.editLoanProduct(loanproductWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/product-activate")
    @PreAuthorize("hasPermission(#loanproductWrapper.getGroupid(),'group',@objectAction.initFields('loanproduct','cancreate'))")
    public Mono<ResponseEntity<?>> activateLoanProduct(@RequestBody LoanproductWrapper loanproductWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.activateDeactivateLoanProduct(loanproductWrapper, username, true))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/product-deactivate")
    @PreAuthorize("hasPermission(#loanproductWrapper.getGroupid(),'group',@objectAction.initFields('loanproduct','cancreate'))")
    public Mono<ResponseEntity<?>> deActivateLoanProduct(@RequestBody LoanproductWrapper loanproductWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.activateDeactivateLoanProduct(loanproductWrapper, username, false))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/products")
    @PreAuthorize("hasPermission(#request.getGroupId(), 'group' , @objectAction.initFields('loanproduct','canview'))")
    public Mono<ResponseEntity<?>> getLoanProducts(@RequestBody LoanProductRequest request,boolean isActive) {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(loanService.getActiveLoanProductsbyGroup(request.getGroupId(),isActive)));
    }

    @PostMapping(path = "/product/deactivated")
    @PreAuthorize("hasPermission(#request.getGroupId(),'group',@objectAction.initFields('loanproduct','canview'))")
    public Mono<ResponseEntity<?>> getInactiveLoanProduct(@RequestBody LoanProductRequest request) {
        return loanService.getInactiveGroupLoanProducts(request.getId())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/penalty/{filter}")
    public Mono<ResponseEntity<?>> getLoanPenalties(@RequestBody LoanPenaltiesRequest request) {
        switch (request.getFilter()) {
            case "group":
                return loanService.getGroupLoansPenalties(request.getFilterId().get())
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(loanService::getMemberLoansPenalties)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().body(new UniversalResponse("Failed", "unsupported filter")));
        }
    }

    @PostMapping("/")
    public Mono<ResponseEntity<?>> getLoanPaymentsByUser(@RequestBody PageDataRequest request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoanPaymentsbyUser(username, request.getPage(), request.getSize()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/pay")
//    @CanTransact
    public Mono<ResponseEntity<?>> getLoanPaymentsByGroup(@RequestBody LoanProductRequest request, @ModelAttribute String usernme) {
        return loanService.getLoanProductsbyGroup(request.getId())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/user-pending-approval")
    @ApiOperation(value = "Fetch user loans pending approval", notes = "Just pass the page and size.")
    public Mono<ResponseEntity<?>> getUserLoansPendingApproval(@RequestBody PageDataRequest pageDataRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyUser(username, pageDataRequest.getPage(), pageDataRequest.getSize()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/group-pending-approval")
    @ApiOperation(value = "Fetch user loans pending approval", notes = "Pass the group id, page and size.")
    public Mono<ResponseEntity<?>> getGroupLoansPendingApproval(@RequestBody PageDataRequest pageDataRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyGroup(pageDataRequest.getId(), pageDataRequest.getPage(), pageDataRequest.getSize()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/guarantee")
    @ApiOperation(value = "Approve or decline guarantorship", notes = "Just pass the loan id. All other fields are not required.")
    public Mono<ResponseEntity<?>> approveDenyGuarantorRequest(@RequestParam Long lid, @RequestParam Boolean guarantee) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.approveDenyGuarantorRequest(lid, guarantee, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/user-guarantor-loans")
    public Mono<ResponseEntity<?>> getGuarantorLoans() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(loanService::getGuarantorLoans)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/user-guarantor-denied-loans")
    public Mono<ResponseEntity<?>> getDeniedGuarantorLoans() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(loanService::getUserDeclinedGuarantorLoans)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/pending-approval")
    @ApiOperation(value = "Fetch user loans pending approval", notes = "Pass the loan product id, page and size.")
    public Mono<ResponseEntity<?>> getLoansPendingApprovalByLoanProduct(@RequestBody PageDataRequest pageDataRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyLoanProduct(pageDataRequest.getId(), username, pageDataRequest.getPage(), pageDataRequest.getSize()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/repay")
    public Mono<ResponseEntity<?>> initiateLoanRepayment(@RequestBody LoanRepaymentsWrapper request) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.initiateLoanRepayment(request, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(path = "/disbursed")
    @ApiOperation(value = "Fetch disbursed loans for group, user or loan product.", notes = "To fetch user disbursed loans just pass the page and size.")
    public Mono<ResponseEntity<?>> getDisbursedLoans(@RequestBody LoanDisbursedRequest request) {
        switch (request.getFilter()) {
            case "group":
                return loanService.getDisbursedLoansperGroup(request.getFilterid().get(), request.getPage(), request.getSize())
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "loanproduct":
                return loanService.getDisbursedLoansperLoanproduct(request.getFilterid().get(), request.getPage(), request.getSize())
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> loanService.getDisbursedLoansperUser(username, request.getPage(), request.getSize()))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }
    }
}
