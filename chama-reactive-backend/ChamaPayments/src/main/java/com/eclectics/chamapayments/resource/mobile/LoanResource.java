package com.eclectics.chamapayments.resource.mobile;

import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.service.LoanService;
import com.eclectics.chamapayments.service.enums.CanTransact;
import com.eclectics.chamapayments.wrappers.request.ApplyLoanWrapper;
import com.eclectics.chamapayments.wrappers.request.ApproveLoanWrapper;
import com.eclectics.chamapayments.wrappers.request.LoanRepaymentsWrapper;
import com.eclectics.chamapayments.wrappers.response.LoanRepaymentWrapper;
import com.eclectics.chamapayments.wrappers.response.LoanproductWrapper;
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
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/v2/payment/loan")
@RequiredArgsConstructor
public class LoanResource {

    private final Gson gson;
    private final LoanService loanService;

    @PostMapping("/apply")
//    @PreAuthorize("hasPermission(#applyLoanWrapper.loanproduct,'loanproduct',@objectAction.initFields('loans','cancreate'))")
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
    @PreAuthorize("hasPermission(#loanproductWrapper.getGroupid(),'group',@objectAction.initFields('loanproduct','cancreate'))")
    public Mono<ResponseEntity<?>> editLoanProduct(@RequestBody @Valid LoanproductWrapper loanproductWrapper) {
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

    @GetMapping(path = "/products")
    @PreAuthorize("hasPermission(#groupId,'group',@objectAction.initFields('loanproduct','canview'))")
    public Mono<ResponseEntity<?>> getLoanProducts(@RequestParam long groupId) {
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(loanService.getLoanProductsbyGroup(groupId)));
    }

    @GetMapping(path = "/products/user")
    public Mono<ResponseEntity<?>> getLoanProducts() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(loanService::getUserLoanProducts)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping(path = "/product/deactivated")
    @PreAuthorize("hasPermission(#groupId,'group',@objectAction.initFields('loanproduct','canview'))")
    public Mono<ResponseEntity<?>> getInactiveLoanProduct(@RequestParam long groupId) {
        return loanService.getInactiveGroupLoanProducts(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping(path = "/penalty/{filter}")
    public Mono<ResponseEntity<?>> getLoanPenalties(@PathVariable String filter, @RequestParam Optional<Long> filterId) {
        switch (filter) {
            case "group":
                return loanService.getGroupLoansPenalties(filterId.get())
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(loanService::getMemberLoansPenalties)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().body(new UniversalResponse("fail", "unsupported filter")));
        }
    }

    @GetMapping("/loan-repayment")
    @ApiOperation(value = "Fetch loan repayments per user and/or loandisbursed id.", notes = "Pass the loan disbursed id if you wish to fetch the repayments for a particular loan disbursed")
    public Mono<ResponseEntity<?>> getLoanPaymentsByUser(@RequestParam Optional<Long> filterId, @RequestParam Integer page, @RequestParam Integer size) {
        return filterId.<Mono<ResponseEntity<?>>>map(loanDisbursedId -> loanService.getLoanPaymentsbyDisbursedloan(loanDisbursedId, page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res)))
                .orElseGet(() -> CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> loanService.getLoanPaymentsbyUser(username, page, size))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res)));
    }

    @GetMapping("/pay/{groupId}")
    public Mono<ResponseEntity<?>> getLoanPaymentsByGroup(@PathVariable Long groupId) {
        return loanService.getLoanProductsbyGroup(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping(path = "/disbursed/{filter}")
    public Mono<ResponseEntity<?>> getDisbursedLoans(@PathVariable String filter, @RequestParam Optional<Long> filterid,
                                                     @RequestParam int page, @RequestParam int size) {
        switch (filter) {
            case "group":
                return loanService.getDisbursedLoansperGroup(filterid.get(), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "loanproduct":
                return loanService.getDisbursedLoansperLoanproduct(filterid.get(), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return CustomAuthenticationUtil.getUsername()
                        .flatMap(username -> loanService.getDisbursedLoansperUser(username, page, size))
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("fail", "Unsupported filter")));
        }
    }

    @PostMapping("/repay")
    @CanTransact
    public Mono<ResponseEntity<?>> initiateLoanRepayment(@RequestBody LoanRepaymentsWrapper request, @ModelAttribute String username) {
        return loanService.initiateLoanRepayment(request, username)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user-pending-approval")
    public Mono<ResponseEntity<?>> getUserLoansPendingApproval(@RequestParam Integer page, @RequestParam Integer size) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyUser(username, page, size))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-pending-approval")
    public Mono<ResponseEntity<?>> getGroupLoansPendingApproval(@RequestParam("gid") Long groupId, @RequestParam Integer page, @RequestParam Integer size) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyGroup(groupId, page, size))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/guarantee")
    @ApiOperation(value = "Approve or decline guarantorship", notes = "Just pass the loan id. All other fields are not required.")
    public Mono<ResponseEntity<?>> approveDenyGuarantorRequest(@RequestParam Long lid, @RequestParam Boolean guarantee) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.approveDenyGuarantorRequest(lid, guarantee, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user-guarantor-loans")
    public Mono<ResponseEntity<?>> getGuarantorLoans() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(loanService::getGuarantorLoans)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user-guarantor-denied-loans")
    public Mono<ResponseEntity<?>> getDeniedGuarantorLoans() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(loanService::getUserDeclinedGuarantorLoans)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/pending-approval")
    @ApiOperation(value = "Loans pending approval by loan product")
    public Mono<ResponseEntity<?>> getLoansPendingApprovalByLoanProduct(@RequestParam Long pid, @RequestParam Integer page, @RequestParam Integer size) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoansPendingApprovalbyLoanProduct(pid, username, page, size))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiOperation(value = "Record loan payment")
    @PostMapping(path = "/receipt-payment", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<?>> recordLoanRepayment(@RequestPart("payment-details") String body,
                                                       @RequestPart("file") Mono<FilePart> file) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> file.flatMap(receipt -> {
                    LoanRepaymentWrapper loanRepaymentWrapper = gson.fromJson(body, LoanRepaymentWrapper.class);
                    return loanService.recordLoanRepayment(loanRepaymentWrapper.getId(), loanRepaymentWrapper.getPaidAmount(),
                            loanRepaymentWrapper.getReceiptNumber(), receipt, username);
                })).map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/approve-receipt-payment")
    @ApiOperation(value = "Approve loan receipt payment", notes = "Please make sure to pass the group id")
    @PreAuthorize("hasPermission(#loanRepaymentsWrapper.getGroupId(),'group',@objectAction.initFields('loans','canedit'))")
    public Mono<ResponseEntity<?>> approveReceiptPayment(@RequestBody LoanRepaymentsWrapper loanRepaymentsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.approveLoanRepayment(loanRepaymentsWrapper.getLoanpaymentid(), loanRepaymentsWrapper.isApprove(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/user-payments-pending-approval")
    @ApiOperation(value = "Get user payments pending approval")
    public Mono<ResponseEntity<?>> getLoanPaymentPendingApprovalByUser(@RequestParam Integer page, @RequestParam Integer size) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoanPaymentPendingApprovalByUser(username, page, size))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-payments-pending-approval")
    @ApiOperation(value = "Get group payments pending approval")
    @PreAuthorize("hasPermission(#groupId,'group',@objectAction.initFields('loans','canview'))")
    public Mono<ResponseEntity<?>> getLoanPaymentPendingApprovalByGroup(@RequestParam Long groupId, @RequestParam Integer page, @RequestParam Integer size) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> loanService.getLoanPaymentPendingApprovalByGroup(groupId, username, page, size))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-loan-repayments")
    @ApiOperation(value = "Get group loan repayments")
    public Mono<ResponseEntity<?>> getLoanPaymentsByGroup(@RequestParam Long groupId, @RequestParam Integer page, @RequestParam Integer size) {
        return loanService.getLoanPaymentsbyGroupid(groupId, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
