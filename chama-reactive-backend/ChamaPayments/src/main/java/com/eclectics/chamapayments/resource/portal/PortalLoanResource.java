package com.eclectics.chamapayments.resource.portal;

import com.eclectics.chamapayments.service.LoanService;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Optional;

import static com.eclectics.chamapayments.service.constants.StringConstantsUtil.PHONE_NUMBER_MATCH;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/portal/payments/loan")
public class PortalLoanResource {

    private final LoanService loanService;

    @GetMapping(path = "/disbursed/{filter}")
    public Mono<ResponseEntity<?>> getDisbursedLoans(@PathVariable String filter,
                                                     @RequestParam Optional<Long> filterid,
                                                     @RequestParam(required = false)
                                                     @Size(min = 10, max = 12, message = "Phone number length should of length 10-12")
                                                     @Pattern(regexp = PHONE_NUMBER_MATCH, message = "Please provide a  valid phone number")
                                                     String phoneNumber,
                                                     @RequestParam int page,
                                                     @RequestParam int size) {
        switch (filter) {
            case "group":
                return loanService.getDisbursedLoansperGroup(filterid.get(), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "loanproduct":
                return loanService.getDisbursedLoansperLoanproduct(filterid.get(), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                if (phoneNumber == null)
                    return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("Failed", "Provide phone number")));
                return loanService.getDisbursedLoansperUser(phoneNumber, page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("Failed", "Unsupported filter")));
        }
    }

    @GetMapping("/products")
    public Mono<ResponseEntity<?>> groupLoanProducts(@RequestParam Long groupId) {
        return loanService.getLoanProductsbyGroup(groupId)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/applications")
    @ApiOperation(value = "Fetch loan applications for a product or a user's loan applications", notes = "To fetch a users loan applications, pass their phone number. Please note if this is not passed, you get the loan applications for a loan product. Thanks!")
    public Mono<ResponseEntity<?>> loanApplications(@RequestParam(required = false) Long loanProductId,
                                                    @RequestParam(required = false)
                                                    @Size(min = 10, max = 12, message = "Phone number length should of length 10-12")
                                                    @Pattern(regexp = PHONE_NUMBER_MATCH, message = "Please provide a  valid phone number")
                                                    String phoneNumber,
                                                    @RequestParam Integer page, @RequestParam Integer size) {
        if (phoneNumber != null && phoneNumber.length() == 12)
            return loanService.getUserLoanApplications(phoneNumber, page, size)
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        if (loanProductId != null)
            return loanService.getLoanApplications(loanProductId, page, size)
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

        return Mono.just(ResponseEntity.badRequest().body(new UniversalResponse("fail", "At least one parameter is needed.")));
    }

    @GetMapping(path = "/loanpayments/{filter}")
    @ApiOperation(value = "get disbursed loans using various filters, `group`,`user`,`disbursedloan`, `loanproduct`")
    public Mono<ResponseEntity<?>> getLoanPayments(@PathVariable String filter, @RequestParam long filterid,
                                                   @RequestParam int page, @RequestParam int size) {
        switch (filter) {
            case "group":
                return loanService.getLoanPaymentsbyGroupid(filterid, page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "user":
                return loanService.getLoanPaymentsbyUser(String.valueOf(filterid), page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "disbursedloan":
                return loanService.getLoanPaymentsbyDisbursedloan(filterid, page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            case "loanproduct":
                return loanService.getLoanPaymentsByLoanProductProduct(filterid, page, size)
                        .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
            default:
                return Mono.just(ResponseEntity.ok(new UniversalResponse("failed", "unsupported filter")));
        }
    }

    @GetMapping(path = "/dueloans")
    @ApiOperation(value = "get the overdue loans")
    public Mono<ResponseEntity<?>> overdueLoans(@RequestParam long groupid, @RequestParam int page, @RequestParam int size) {
        return loanService.getOverdueLoans(groupid, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping(path = "/penalties")
    public Mono<ResponseEntity<?>> getMemberLoanPenalties(
            @RequestParam @Size(min = 10, max = 12, message = "Phone number length can only be of length 10-12")
            @Pattern(regexp = PHONE_NUMBER_MATCH, message = "Phone number cannot contain special characters and letters") String phoneNumber,
            @RequestParam Integer page,
            @RequestParam Integer size) {
        return loanService.getMemberLoansPenalties(phoneNumber, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
