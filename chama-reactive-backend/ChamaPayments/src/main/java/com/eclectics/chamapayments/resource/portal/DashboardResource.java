package com.eclectics.chamapayments.resource.portal;

import com.eclectics.chamapayments.service.AccountingService;
import com.eclectics.chamapayments.service.DashboardValuesService;
import com.eclectics.chamapayments.service.LoanService;
import com.eclectics.chamapayments.wrappers.response.UniversalResponse;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

import static com.eclectics.chamapayments.service.constants.StringConstantsUtil.LOWER_CASE_MATCH;
import static com.eclectics.chamapayments.service.constants.StringConstantsUtil.UPPER_AND_LOWER_CASE_MATCH;

@Validated
@RestController
@RequestMapping("/portal/payments/dash")
@RequiredArgsConstructor
public class DashboardResource {

    private final LoanService loanService;
    private final AccountingService accountingService;
    private final DashboardValuesService dashboardValuesService;

    @ApiIgnore
    @GetMapping("/accounting")
    public Mono<ResponseEntity<?>> getAccountingValues() {
        return dashboardValuesService.transactionsData()
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-account")
    public Mono<ResponseEntity<?>> getGroupAccounts(@RequestParam Long groupId) {
        return Mono.fromCallable(() -> accountingService.getAccountbyGroup(groupId))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/transactions")
    public Mono<ResponseEntity<?>> getTransactionData(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, yearly")
            @RequestParam(name = "period", defaultValue = "days")
            @Size(max = 7, message = "The length of the pattern cannot be greater than 7 characters")
            @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Period cannot contain special characters or digits")
            String period,
            @ApiParam(value = "contributionPayment," +
                    " contributionSchedulePayment,loanApplications,loanProducts," +
                    "loansDisbursed,loansRepayments, loansRepaymentPendingApproval," +
                    " transactionLogs, transactionsPendingApproval, transactionTypes, withdrawalLogs, withdrawalsPendingApproval")
            @RequestParam(name = "transactionType", defaultValue = "contributionPayment")
            @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Transaction filter cannot contain capital letters, special characters or digits.")
            String transactionType,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "1")
            int page,
            @ApiParam(value = "group name, default all")
            @RequestParam(name = "group", defaultValue = "all")
            String group,
            @ApiParam(value = "extra queryParams")
            @RequestParam(name = "additional", defaultValue = "")
            @Pattern(regexp = "\\w", message = "Additional filter cannnot contain special characters")
            String additional
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return dashboardValuesService.getGroupTransactionsByType(startDate, endDate, period, transactionType, group, additional, pageable)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/transactiontrend")
    public Mono<ResponseEntity<?>> transactionTrend(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, yearly")
            @RequestParam(name = "period", defaultValue = "days")
            @Size(max = 7, message = "Invalid period length")
            @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Period cannot contain special characters or digits")
            String period,
            @ApiParam(value = "group name, default all")
            @RequestParam(name = "group", defaultValue = "")
            @Pattern(regexp = LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group,
            @ApiParam(value = "country , default kenya")
            @RequestParam(name = "country", defaultValue = "Tanzania")
            @Size(max = 20, message = "Country cannot have a length greater than 25")
            @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Country cannot have special characters or digits")
            String country
    ) {
        return Mono.fromCallable(() -> accountingService.groupTransactionsDetailed(startDate, endDate, period, group))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("Success", "Transactions trend", res))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/groupTransaction")
    public Mono<ResponseEntity<?>> groupTransactionTrend(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, yearly")
            @RequestParam(name = "period", defaultValue = "days")
            @Size(max = 7, message = "The ")
            @Pattern(regexp = UPPER_AND_LOWER_CASE_MATCH, message = "Period cannot contain special characters or digits")
            String period,
            @ApiParam(value = "group name, default all")
            @RequestParam(name = "group", defaultValue = "")
            @Pattern(regexp = LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group) {
        return Mono.fromCallable(() -> accountingService.groupTransactionsDetailed(startDate, endDate, period, group))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("Success", "Group transactions trend"))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/grouploansrepayment")
    public Mono<ResponseEntity<?>> groupsLoansSummary(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = "group name, default all")
            @RequestParam(name = "group", defaultValue = "all")
            @Pattern(regexp = LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group,
            @ApiParam(value = "size , default 1")
            @RequestParam(name = "size", defaultValue = "10")
            int size,
            @ApiParam(value = "pageno, default 1")
            @RequestParam(name = "pageno", defaultValue = "0")
            int pageNo
    ) {
        Pageable page = PageRequest.of(pageNo, size);
        return loanService.getGroupsLoanSummaryPayment(group, startDate, endDate, page)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
