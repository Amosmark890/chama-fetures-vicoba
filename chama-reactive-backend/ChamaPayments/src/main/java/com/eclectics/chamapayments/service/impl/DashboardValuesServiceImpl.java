package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.model.jpaInterfaces.AccountsTotals;
import com.eclectics.chamapayments.repository.*;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.DashboardValuesService;
import com.eclectics.chamapayments.service.enums.PaymentEnum;
import com.eclectics.chamapayments.wrappers.response.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardValuesServiceImpl implements DashboardValuesService {

    private final ContributionRepository contributionRepository;
    private final ContributionLoanRepository contributionLoanRepository;
    private final ContributionsPaymentRepository contributionsPaymentRepository;
    private final ContributionSchedulePaymentRepository contributionSchedulePaymentRepository;
    private final LoansdisbursedRepo loansdisbursedRepo;
    private final LoanapplicationsRepo loanapplicationsRepo;
    private final LoanPenaltyRepository loanPenaltyRepository;
    private final LoanPenaltyPaymentRepository loanPenaltyPaymentRepository;
    private final LoansrepaymentRepo loansrepaymentRepo;
    private final LoanproductsRepository loanproductsRepository;
    private final TransactionlogsRepo transactionlogsRepo;
    private final WithdrawallogsRepo withdrawallogsRepo;
    private final ChamaKycService chamaKycService;
    private final AccountsRepository accountsRepository;

    private static final BiPredicate<GroupWrapper, String> groupFilterByGroupNameParam = (group, groupName) -> {
        if (!groupName.trim().equalsIgnoreCase("all")) {
            return group.getName().equalsIgnoreCase(groupName.trim());
        }
        return true;
    };

    static Map<String, TemporalAdjuster> timeAdjusters() {
        Map<String, TemporalAdjuster> adjusterHashMap = new HashMap<>();
        adjusterHashMap.put("days", TemporalAdjusters.ofDateAdjuster(d -> d)); // identity
        adjusterHashMap.put("weeks", TemporalAdjusters.previousOrSame(DayOfWeek.of(1)));
        adjusterHashMap.put("months", TemporalAdjusters.firstDayOfMonth());
        adjusterHashMap.put("years", TemporalAdjusters.firstDayOfYear());
        return adjusterHashMap;
    }

    static Comparator<Map<Object, Object>> mapComparator() {
        return new Comparator<Map<Object, Object>>() {
            @SneakyThrows
            public int compare(Map<Object, Object> m1, Map<Object, Object> m2) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date date1 = sdf.parse(m1.get("dateofday").toString());
                Date date2 = sdf.parse(m2.get("dateofday").toString());
                return date1.compareTo(date2);
            }
        };
    }

    @Override
    public Mono<Map<String, Object>> transactionsData() {
        return Mono.fromCallable(() -> {
            long totalContributions = contributionRepository.count();

            int activeContributions = contributionRepository.countByActiveTrue();

            int inactiveContributions = contributionRepository.countByActiveFalse();

            long totalContributionPayments = contributionsPaymentRepository.count();

            long successfulContributionPayments = contributionsPaymentRepository.countByPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());

            long unSuccessfulContributionPayments = contributionsPaymentRepository.countByPaymentStatus(PaymentEnum.PAYMENT_FAILED.name());

            long successfulContributionsAmount = contributionsPaymentRepository.getTotalSuccessfulContributions();

            long totalDisbursedLoans = loansdisbursedRepo.count();

            long totalLoanRepayments = loansrepaymentRepo.count();

            long successfulRepaymentsAmount = loansrepaymentRepo.getSuccessfulRepaymentsAmount();

            long totalLoanPenalties = loanPenaltyRepository.count();

            long totalSuccessfullyPaidLoanPenalties = loanPenaltyRepository.countByPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());

            long clearedLoanPenalties = loanPenaltyRepository.getSumOfSuccessfulRepayments();

            long sumOfTotalLoansDisbursed = loansdisbursedRepo.getSumOfTotalLoansDisbursed();

            long totalTransactions = transactionlogsRepo.count();

            long totalWithdrawals = withdrawallogsRepo.count();

            AccountsTotals accountsTotals = accountsRepository.getGroupAccountBalances();

            Map<String, Object> accountingDashData = new LinkedHashMap<>();
            accountingDashData.put("totalcontributions", totalContributions);
            accountingDashData.put("activecontributions", activeContributions);
            accountingDashData.put("inactivecontributions", inactiveContributions);
            accountingDashData.put("totalcontributionpayments", totalContributionPayments);
            accountingDashData.put("successfulcontributionpayments", successfulContributionPayments);
            accountingDashData.put("unSuccessfulcontributionpayments", unSuccessfulContributionPayments);
            accountingDashData.put("successfulcontributionsamount", successfulContributionsAmount);
            accountingDashData.put("totaldisbursedloans", totalDisbursedLoans);
            accountingDashData.put("totalloanrepayments", totalLoanRepayments);
            accountingDashData.put("successfulloanrepaymentsamount", successfulRepaymentsAmount);
            accountingDashData.put("totalloanpenalties", totalLoanPenalties);
            accountingDashData.put("totalsuccessfullypaidloanpenalties", totalSuccessfullyPaidLoanPenalties);
            accountingDashData.put("clearedloanpenalties", clearedLoanPenalties);
            accountingDashData.put("sumoftotalloansdisbursed", sumOfTotalLoansDisbursed);
            accountingDashData.put("totaltransactions", totalTransactions);
            accountingDashData.put("totalwithdrawals", totalWithdrawals);
            accountingDashData.put("totalGroupAvailableBalance", accountsTotals.getGroupbalances());
            accountingDashData.put("totalCbsAvailableBalance", accountsTotals.getCoreaccountbalances());

            return accountingDashData;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupTransactionsByType(Date startDate, Date endDate, String period, String transactionType, String group, String additional, Pageable pageable) {
        switch (transactionType) {
            case "contributionPayment":
                return getContributionPaymentReport(startDate, endDate, period, group, pageable);
            case "contributionSchedulePayment":
                return getContributionSchedulePayment(startDate, endDate, period, group, pageable);
            case "disbursedLoans":
                return getLoanDisbursed(startDate, endDate, period, group, pageable);
            case "disbursedLoansPerProduct":
                return getDisbursedLoansPerProduct(startDate, endDate, period, group, additional, pageable);
            case "loansPendingApprovalPerProduct":
                return getLoansPendingApprovalByLoanProduct(startDate, endDate, period, group, additional, pageable);
            case "loanProductsByGroup":
                return getLoanProductsByGroup(startDate, endDate, period, group, additional, pageable);
            case "loanPaymentsByGroup":
                return getLoanRepaymentsByGroupAndProductId(startDate, endDate, period, group, additional, pageable);
            case "overdueLoans":
                return getGroupOverdueLoans(startDate, endDate, period, group, additional, pageable);
            case "loanPenalties":
                return getLoanPenalties(startDate, endDate, period, group, pageable);
            case "pendingLoanApplications":
                return getPendingLoanApplications(startDate, endDate, period, group, pageable);
            case "approvedLoanApplications":
                return getApprovedLoanApplications(startDate, endDate, period, group, pageable);
            case "transactionLogs":
                return getTransactionsLogsByGroup(startDate, endDate, period, group, pageable);
            case "withdrawalLogs":
                return getWithdrawalLogs(startDate, endDate, period, group, pageable);
            default:
                return Mono.just(new UniversalResponse("failed", String.format("transaction by type %s not found", transactionType)));
        }
    }

    private Mono<UniversalResponse> getWithdrawalLogs(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<WithdrawalLogs> withdrawalLogs;
            int numOfRecords = 0;
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);

            if (group.equals("all")) {
                withdrawalLogs = withdrawallogsRepo.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable);
                numOfRecords = withdrawallogsRepo.countAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate);
            } else {
                if (groups != null) {
                    withdrawalLogs = withdrawallogsRepo.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable)
                            .parallelStream()
                            .filter(log -> log.getContributions().getMemberGroupId() == groups.getId())
                            .collect(Collectors.toList());
                    numOfRecords = withdrawalLogs.size();
                } else {
                    return new UniversalResponse("success", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<WithdrawLogsWrapper> withdrawLogsWrappers = withdrawalLogs.parallelStream()
                    .map(log -> WithdrawLogsWrapper.builder()
                            .transactionId(log.getUniqueTransactionId())
                            .amount(log.getTransamount())
                            .contributionNarration(log.getContribution_narration())
                            .debitAccountId(log.getDebitAccounts().getId())
                            .contributionAccount(log.getContributions().getId())
                            .isContribution(null == log.getContributions())
                            .contributionName(null == log.getContributions() ? "" : log.getContributions().getName())
                            .isLoanApplication(null == log.getLoanApplications())
                            .creditUserNumber(log.getCreditphonenumber())
                            .newBalance(log.getNewbalance())
                            .oldBalance(log.getOldbalance())
                            .capturedBy(log.getCapturedby())
                            .reason(log.getWithdrawalreason())
                            .transferToUserStatus(log.getTransferToUserStatus())
                            .createdOn(log.getCreatedOn())
                            .build()).collect(Collectors.toList());

            Map<String, List<WithdrawLogsWrapper>> disbursedLoans = withdrawLogsWrappers.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            UniversalResponse response = new UniversalResponse("success", "Group withdrawal logs", responseData);
            response.setMetadata(numOfRecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getApprovedLoanApplications(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoanApplications> loanApplicationsList;
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);

            if (group.equalsIgnoreCase("all")) {
                loanApplicationsList = loanapplicationsRepo.findAllByApprovedAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate, pageable).getContent();

            } else {
                if (groups != null) {
                    loanApplicationsList = loanapplicationsRepo.findAllByApprovedAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate, pageable)
                            .getContent()
                            .parallelStream()
                            .filter(loan -> loan.getLoanProducts().getGroupId() == groups.getId())
                            .collect(Collectors.toList());
                } else {
                    return new UniversalResponse("failed", String.format("Group search by name %s failed", group), new ArrayList<>());
                }
            }
            List<GroupLoansApprovedWrapper> approvedWrappers =
                    loanApplicationsList.stream()
                            .map(loan -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                                if (member == null) return new GroupLoansApprovedWrapper();
                                return GroupLoansApprovedWrapper.builder()
                                        .loanproductid(loan.getId())
                                        .loanapplicationid(loan.getLoanProducts().getId())
                                        .amount(loan.getAmount())
                                        .loanproductname(loan.getLoanProducts().getProductname())
                                        .appliedon(loan.getCreatedOn())
                                        .membername(String.format("%s %s", member.getFirstname(), member.getLastname()))
                                        .memberphonenumber(member.getPhonenumber())
                                        .unpaidloans(loan.getUnpaidloans())
                                        .status(loan.getStatus())
                                        .approvedBy(loan.getApprovedby())
                                        .build();
                            }).collect(Collectors.toList());


            Map<String, List<GroupLoansApprovedWrapper>> approvedLoansMap = approvedWrappers.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedon()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            approvedLoansMap.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            return new UniversalResponse("success", "Approved Groups Loans", Map.of("data", responseData));
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getPendingLoanApplications(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoanApplications> loanApplicationsList = new ArrayList<>();
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);

            if (group.equalsIgnoreCase("all")) {
                loanApplicationsList = loanapplicationsRepo.findAllByPendingAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate);
            } else {
                if (groupWrapper != null) {
                    loanApplicationsList = loanapplicationsRepo.findAllByPendingAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate)
                            .parallelStream()
                            .filter(loan -> groupWrapper.getName().toLowerCase().equalsIgnoreCase(group)).collect(Collectors.toList());
                } else {
                    return new UniversalResponse("failed", String.format("Group search by name %s failed", group), new ArrayList<>());
                }
            }

            List<GroupLoansPendingApproval> pendingWrapper =
                    loanApplicationsList.stream()
                            .map(loan -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                                if (member == null) return new GroupLoansPendingApproval();
                                return GroupLoansPendingApproval.builder()
                                        .loanproductid(loan.getId())
                                        .loanapplicationid(loan.getLoanProducts().getId())
                                        .amount(loan.getAmount())
                                        .loanproductname(loan.getLoanProducts().getProductname())
                                        .appliedon(loan.getCreatedOn())
                                        .membername(String.format("%s %s", member.getFirstname(), member.getLastname()))
                                        .memberphonenumber(member.getPhonenumber())
                                        .unpaidloans(loan.getUnpaidloans())
                                        .build();
                            }).collect(Collectors.toList());


            Map<String, List<GroupLoansPendingApproval>> loansPendingApproval = pendingWrapper.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedon()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            loansPendingApproval.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            return new UniversalResponse("success", "Groups Loan Pending Approval", Map.of("data", responseData));
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getLoanPenalties(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoanPenalty> loansPenaltyList = loanPenaltyRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable);
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);

            if (!group.equals("all")) {
                if (groupWrapper == null) {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                } else {
                    loansPenaltyList = loansPenaltyList.parallelStream()
                            .filter(loan -> loan.getLoansDisbursed().getGroupId() == groupWrapper.getId())
                            .collect(Collectors.toList());
                }
            }

            List<LoanPenaltyReportWrapper> loanPenaltyWrapper = loansPenaltyList.parallelStream()
                    .map(penalty -> {
                        MemberWrapper member = chamaKycService.getMonoMemberDetailsById(penalty.getMemberId());
                        GroupWrapper g = chamaKycService.getMonoGroupById(penalty.getLoansDisbursed().getGroupId());
                        if (member == null || g == null) return new LoanPenaltyReportWrapper();
                        return LoanPenaltyReportWrapper.builder()
                                .loanPenaltyId(penalty.getId())
                                .penaltyAmount(penalty.getPenaltyAmount())
                                .paymentStatus(penalty.getPaymentStatus())
                                .paidAmount(penalty.getPaidAmount())
                                .dueAmount(penalty.getDueAmount())
                                .transactionId(penalty.getTransactionId())
                                .loanDueDate(penalty.getLoanDueDate().toString())
                                .lastPaymentDate(penalty.getLastPaymentDate())
                                .groupName(g.getName())
                                .memberName(String.format(" %s %s", member.getFirstname(), member.getLastname()))
                                .memberPhoneNumber(member.getPhonenumber())
                                .createdOn(penalty.getCreatedOn())
                                .build();
                    }).collect(Collectors.toList());

            Map<String, List<LoanPenaltyReportWrapper>> penaltyData = loanPenaltyWrapper.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

            List<Map<Object, Object>> penaltyResponse = new ArrayList<>();

            penaltyData.forEach((key, value) -> penaltyResponse.add(Map.of("dateofday", key, "objects", value)));
            penaltyResponse.sort(mapComparator());
            return new UniversalResponse("success", "penalty reports by groups", penaltyResponse);
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getGroupOverdueLoans(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        return Mono.fromCallable(() -> {
            long loanProductId;
            try {
                loanProductId = Long.parseLong(additional);
            } catch (NumberFormatException ex) {
                return new UniversalResponse("failed", "Additional additional param value must be a loan product id");
            }
            List<LoansDisbursed> loansDisbursedList;
            int recordsCount;
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (group.equalsIgnoreCase("all")) {
                loansDisbursedList = loanProductId == 0 ? loansdisbursedRepo.findAllOverdue(startDate, endDate, pageable) : loansdisbursedRepo.findAllOverdueByLoanProductId(loanProductId, startDate, endDate, pageable);
                recordsCount = loanProductId == 0 ? loansdisbursedRepo.countAllOverdue(startDate, endDate) : loansdisbursedRepo.countAllOverdueByLoanProductId(loanProductId, startDate, endDate);
            } else {
                if (groupWrapper != null) {
                    loansDisbursedList = loanProductId == 0 ? loansdisbursedRepo.findAllOverdueByGroup(groupWrapper.getId(), startDate, endDate, pageable) : loansdisbursedRepo.findAllOverdueByLoanProductIdAndGroup(loanProductId, groupWrapper.getId(), startDate, endDate, pageable);
                    recordsCount = loanProductId == 0 ? loansdisbursedRepo.countAllOverdueByGroup(groupWrapper.getId(), startDate, endDate) : loansdisbursedRepo.countAllOverdueByLoanProductIdAndGroup(loanProductId, groupWrapper.getId(), startDate, endDate);
                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                    .parallelStream()
                    .map(disbursed -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(disbursed.getMemberId());
                                GroupWrapper g = chamaKycService.getMonoGroupById(disbursed.getGroupId());
                                if (member == null) return new LoansDisbursedWrapper();
                                return LoansDisbursedWrapper.builder()
                                        .accountTypeId(disbursed.getId())
                                        .loanId(disbursed.getLoanApplications().getId())
                                        .appliedOn(disbursed.getCreatedOn())
                                        .contributionId(disbursed.getLoanApplications().getLoanProducts().getContributions().getId())
                                        .contributionName(disbursed.getLoanApplications().getLoanProducts().getContributions().getName())
                                        .dueAmount(disbursed.getDueamount())
                                        .principal(disbursed.getPrincipal())
                                        .dueDate(disbursed.getDuedate())
                                        .interest(disbursed.getInterest())
                                        .groupId(g.getId())
                                        .groupName(g.getName())
                                        .interest(disbursed.getInterest())
                                        .recipient(String.format("%s  %s", member.getFirstname(), member.getLastname()))
                                        .recipientNumber(member.getPhonenumber())
                                        .approvedBy(disbursed.getLoanApplications().getApprovedby())
                                        .build();
                            }
                    ).collect(Collectors.toList());

            Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));

            responseData.sort(mapComparator());
            Map<String, Integer> noRecords = new HashMap<>();
            noRecords.put("numofrecords", recordsCount);
            UniversalResponse response = new UniversalResponse("success", "Group overdue loans", responseData);
            response.setMetadata(noRecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getLoanRepaymentsByGroupAndProductId(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoansRepayment> loansRepaymentList;
            long productId;
            long recordCount;
            try {
                productId = Long.parseLong(additional);
            } catch (Exception ex) {
                return new UniversalResponse("failed", "loan Id field must be a number");
            }
            if (group.equalsIgnoreCase("all")) {
                loansRepaymentList = productId == 0 ? loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable) :
                        loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable)
                                .parallelStream()
                                .filter(loansRepayment -> loansRepayment.getLoansDisbursed().getLoanApplications().getLoanProducts().getId() == productId).collect(Collectors.toList());
                recordCount = loansrepaymentRepo.countAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate);
            } else {
                GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);

                if (groupWrapper != null) {
                    loansRepaymentList = loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable)
                            .parallelStream()
                            .filter(loan -> groupFilterByGroupNameParam.test(groupWrapper, group))
                            .filter(loan -> productId == 0 || loan.getLoansDisbursed().getLoanApplications().getLoanProducts().getId() == productId)
                            .collect(Collectors.toList());
                    recordCount = loansrepaymentRepo.countloanpaymentsbyGroupid(groupWrapper.getId());

                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<LoanRepaymentWrapper> loanRepaymentWrappersList = loansRepaymentList
                    .parallelStream()
                    .map(lr -> mapLoansRepaymentToLoanRepaymentWrapper(lr))
                    .collect(Collectors.toList());

            Map<String, List<LoanRepaymentWrapper>> disbursedLoans = loanRepaymentWrappersList.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedDate()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            Map<String, Object> numofrecords = new HashMap<>();
            numofrecords.put("numofrecords", recordCount);
            UniversalResponse response = new UniversalResponse("success", "Loan re-payments by group and product id", responseData);
            response.setMetadata(numofrecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private LoanRepaymentWrapper mapLoansRepaymentToLoanRepaymentWrapper(LoansRepayment loansRepayment) {
        MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loansRepayment.getMemberId());
        GroupWrapper groupWrapper = chamaKycService.getGroupById(loansRepayment.getLoansDisbursed().getGroupId()).orElse(null);
        if (memberWrapper == null || groupWrapper == null) return null;
        return LoanRepaymentWrapper.builder()
                .id(loansRepayment.getId())
                .memberId(loansRepayment.getMemberId())
                .memberName(String.format("%s  %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                .initialLoan(loansRepayment.getOldamount())
                .balance(loansRepayment.getNewamount())
                .paidAmount(loansRepayment.getAmount())
                .receiptNumber(loansRepayment.getReceiptnumber())
                .groupId(loansRepayment.getLoansDisbursed().getGroupId())
                .groupName(groupWrapper.getName())
                .paymentType(loansRepayment.getPaymentType())
                .createdDate(loansRepayment.getCreatedOn())
                .build();
    }

    private Mono<UniversalResponse> getLoanProductsByGroup(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoanProducts> loanProductsList = new ArrayList<>();
            int recordCount = 0;
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (group.equalsIgnoreCase("all")) {
                switch (additional) {
                    case "active":
                        loanProductsList = loanproductsRepository.findAllByIsActiveAndSoftDeleteAndCreatedOnBetween(true, false, startDate, endDate, pageable);
                        recordCount = loanproductsRepository.countLoanProductsByIsActiveAndSoftDeleteAndCreatedOnBetween(true, false, startDate, endDate);
                        break;
                    case "inactive":
                        loanProductsList = loanproductsRepository.findAllByIsActiveAndSoftDeleteAndCreatedOnBetween(false, false, startDate, endDate, pageable);
                        recordCount = loanproductsRepository.countLoanProductsByIsActiveAndSoftDeleteAndCreatedOnBetween(false, false, startDate, endDate);
                        break;
                    case "all":
                        loanProductsList = loanproductsRepository.findAllBySoftDeleteAndCreatedOnBetween(false, startDate, endDate, pageable);
                        recordCount = loanproductsRepository.countAllBySoftDeleteAndCreatedOnBetween(false, startDate, endDate);
                        break;
                }
            } else {
                if (groupWrapper != null) {
                    switch (additional) {
                        case "active":
                            loanProductsList = loanproductsRepository.findAllByGroupIdAndIsActiveAndCreatedOnBetween(groupWrapper.getId(), true, startDate, endDate, pageable);
                            recordCount = loanproductsRepository.countByGroupIdAndIsActiveAndCreatedOnBetween(groupWrapper.getId(), true, startDate, endDate);
                            break;
                        case "inactive":
                            loanProductsList = loanproductsRepository.findAllByGroupIdAndIsActiveAndCreatedOnBetween(groupWrapper.getId(), false, startDate, endDate, pageable);
                            recordCount = loanproductsRepository.countByGroupIdAndIsActiveAndCreatedOnBetween(groupWrapper.getId(), true, startDate, endDate);
                            break;
                        case "all":
                            loanProductsList = loanproductsRepository.findAllByGroupIdAndSoftDelete(groupWrapper.getId(), false, pageable);
                            recordCount = loanproductsRepository.countAllByGroupIdAndSoftDelete(groupWrapper.getId(), false);
                            break;
                    }
                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<LoanProductsWrapperReport> loanProductsWrapperReports = loanProductsList.stream()
                    .map(this::mapToLoanProductsWrapper)
                    .collect(Collectors.toList());

            Map<String, List<LoanProductsWrapperReport>> approvedLoansMap = loanProductsWrapperReports.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            approvedLoansMap.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            Map<String, Object> numOfRecords = new HashMap<>();
            numOfRecords.put("numofrecords", recordCount);
            UniversalResponse response = new UniversalResponse("success", String.format("%s Loan products", additional), responseData);
            response.setMetadata(numOfRecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private LoanProductsWrapperReport mapToLoanProductsWrapper(LoanProducts product) {
        GroupWrapper groupWrapper = chamaKycService.getGroupById(product.getGroupId()).orElse(null);
        if (groupWrapper == null) return null;
        return LoanProductsWrapperReport.builder()
                .productid(product.getId())
                .productname(product.getProductname())
                .description(product.getDescription())
                .max_principal(product.getMax_principal())
                .min_principal(product.getMin_principal())
                .interesttype(product.getInteresttype())
                .interestvalue(product.getInterestvalue())
                .paymentperiod(product.getPaymentperiod())
                .paymentperiodtype(product.getPaymentperiodtype())
                .contributionid(product.getContributions().getId())
                .contributionname(product.getContributions().getName())
                .contributionbalance(product.getContributions().getContributionAmount())
                .groupid(groupWrapper.getId())
                .groupname(groupWrapper.getName())
                .ispenaltypercentage(product.getIsPercentagePercentage())
                .usersavingvalue(product.getUserSavingValue())
                .debitAccountId(product.getDebitAccountId().getId())
                .isActive(product.getIsActive())
                .penaltyPeriod(product.getPenaltyPeriod())
                .createdOn(product.getCreatedOn())
                .build();
    }

    private Mono<UniversalResponse> getLoansPendingApprovalByLoanProduct(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        return Mono.fromCallable(() -> {
            long loanProductId;
            int recordCount = 0;
            try {
                loanProductId = Long.parseLong(additional);
            } catch (NumberFormatException ex) {
                return new UniversalResponse("failed", "Additional  param value must be a loan product id");
            }
            LoanProducts loanProducts = loanproductsRepository.findById(loanProductId).orElse(null);
            if (loanProducts == null)
                return new UniversalResponse("failed", String.format("Loan product by id %s does not exist", loanProductId));
            List<LoanApplications> loansApplicationList;
            if (group.equalsIgnoreCase("all")) {
                loansApplicationList = loanapplicationsRepo.findAllByLoanProductsAndCreatedOnBetweenAndApprovedAndSoftDeleteOrderByCreatedOnDesc(loanProducts, startDate, endDate, false, false, pageable);
            } else {
                GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
                if (groupWrapper == null) {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                } else {
                    loansApplicationList = loanapplicationsRepo.findAllByLoanProductsAndCreatedOnBetweenAndApprovedAndSoftDeleteOrderByCreatedOnDesc(loanProducts, startDate, endDate, false, false, pageable)
                            .parallelStream()
                            .filter(application -> groupFilterByGroupNameParam.test(groupWrapper, group))
                            .collect(Collectors.toList());
                    recordCount = loanapplicationsRepo.countByLoanProductsAndApproved(loanProductId, groupWrapper.getId(), startDate, endDate);
                }
            }
            List<GroupLoansPendingApproval> pendingWrapper =
                    loansApplicationList.stream()
                            .map(loan -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                                if (member == null) return new GroupLoansPendingApproval();
                                return GroupLoansPendingApproval.builder()
                                        .loanproductid(loan.getId())
                                        .loanapplicationid(loan.getLoanProducts().getId())
                                        .amount(loan.getAmount())
                                        .loanproductname(loan.getLoanProducts().getProductname())
                                        .appliedon(loan.getCreatedOn())
                                        .membername(String.format("%s %s", member.getFirstname(), member.getLastname()))
                                        .memberphonenumber(member.getPhonenumber())
                                        .unpaidloans(loan.getUnpaidloans())
                                        .build();
                            }).collect(Collectors.toList());

            Map<String, List<GroupLoansPendingApproval>> loansPendingApproval = pendingWrapper.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedon()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            loansPendingApproval.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());

            Map<String, Integer> numrecords = new HashMap<>();
            numrecords.put("numofrecords", recordCount);

            UniversalResponse response = new UniversalResponse("success", "Groups Loan Pending Approval", responseData);
            response.setMetadata(numrecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getDisbursedLoansPerProduct(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        return Mono.fromCallable(() -> {
            long loanProductId;
            try {
                loanProductId = Long.parseLong(additional);
            } catch (NumberFormatException ex) {
                return new UniversalResponse("failed", "Additional additional param value must be a loan product id");
            }
            List<LoansDisbursed> loansDisbursedList;
            int recordsCount;
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (group.equalsIgnoreCase("all")) {
                loansDisbursedList = loansdisbursedRepo.findAllByLoanProductId(loanProductId, startDate, endDate, pageable);
                recordsCount = loansdisbursedRepo.countLoansDisbursedbyLoanproductAndGroup(loanProductId, startDate, endDate);
            } else {
                if (groupWrapper != null) {
                    loansDisbursedList = loansdisbursedRepo.findByLoanproductAndGroup(loanProductId, groupWrapper.getId(), startDate, endDate, pageable);
                    recordsCount = loansdisbursedRepo.countLoansDisbursedbyLoanproductAndGroup(loanProductId, groupWrapper.getId(), startDate, endDate);
                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                    .parallelStream()
                    .map(disbursed -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(disbursed.getMemberId());
                                if (member == null) return new LoansDisbursedWrapper();
                                return LoansDisbursedWrapper.builder()
                                        .accountTypeId(disbursed.getId())
                                        .loanId(disbursed.getLoanApplications().getId())
                                        .appliedOn(disbursed.getCreatedOn())
                                        .contributionId(disbursed.getLoanApplications().getLoanProducts().getContributions().getId())
                                        .contributionName(disbursed.getLoanApplications().getLoanProducts().getContributions().getName())
                                        .dueAmount(disbursed.getDueamount())
                                        .principal(disbursed.getPrincipal())
                                        .dueDate(disbursed.getDuedate())
                                        .interest(disbursed.getInterest())
                                        .groupId(groupWrapper.getId())
                                        .groupName(groupWrapper.getName())
                                        .interest(disbursed.getInterest())
                                        .recipient(String.format("%s  %s", member.getFirstname(), member.getLastname()))
                                        .recipientNumber(member.getPhonenumber())
                                        .approvedBy(disbursed.getLoanApplications().getApprovedby())
                                        .build();
                            }
                    ).collect(Collectors.toList());

            Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));

            responseData.sort(mapComparator());
            Map<String, Integer> noRecords = new HashMap<>();
            noRecords.put("numofrecords", recordsCount);
            UniversalResponse response = new UniversalResponse("success", "Disbursed Loan products by products id", responseData);
            response.setMetadata(noRecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getLoanDisbursed(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<LoansDisbursed> loansDisbursedList;
            int recordsCount;
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (group.equalsIgnoreCase("all")) {
                loansDisbursedList = loansdisbursedRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable);
                recordsCount = loansdisbursedRepo.countAllByCreatedOnBetween(startDate, endDate);
            } else {
                if (groupWrapper != null) {
                    loansDisbursedList = loansdisbursedRepo.findAllByGroupIdAndCreatedOnBetweenOrderByCreatedOnDesc(groupWrapper.getId(), startDate, endDate, pageable);
                    recordsCount = loansdisbursedRepo.countAllByGroupIdAndCreatedOnBetween(groupWrapper.getId(), startDate, endDate);
                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                    .parallelStream()
                    .map(disbursed -> {
                                MemberWrapper member = chamaKycService.getMonoMemberDetailsById(disbursed.getMemberId());
                                if (member == null) return new LoansDisbursedWrapper();
                                return LoansDisbursedWrapper.builder()
                                        .accountTypeId(disbursed.getId())
                                        .loanId(disbursed.getLoanApplications().getId())
                                        .appliedOn(disbursed.getCreatedOn())
                                        .contributionId(disbursed.getLoanApplications().getLoanProducts().getContributions().getId())
                                        .contributionName(disbursed.getLoanApplications().getLoanProducts().getContributions().getName())
                                        .dueAmount(disbursed.getDueamount())
                                        .principal(disbursed.getPrincipal())
                                        .dueDate(disbursed.getDuedate())
                                        .interest(disbursed.getInterest())
                                        .groupId(disbursed.getGroupId())
                                        .groupName(chamaKycService.getGroupNameByGroupId(disbursed.getGroupId()).orElse(""))
                                        .interest(disbursed.getInterest())
                                        .recipient(String.format("%s  %s", member.getFirstname(), member.getLastname()))
                                        .recipientNumber(member.getPhonenumber())
                                        .approvedBy(disbursed.getLoanApplications().getApprovedby())
                                        .build();
                            }
                    ).collect(Collectors.toList());

            Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                    .collect(Collectors.groupingBy((t -> t.getAppliedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            Map<String, Integer> noRecords = new HashMap<>();
            noRecords.put("numofrecords", recordsCount);
            UniversalResponse response = new UniversalResponse("success", "Loans disbursed", responseData);
            response.setMetadata(noRecords);
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getContributionSchedulePayment(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<ContributionSchedulePayment> contributionSchedulePaymentList;

            if (group.equalsIgnoreCase("all")) {
                contributionSchedulePaymentList = contributionSchedulePaymentRepository.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable);
            } else {
                GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
                if (groupWrapper != null) {
                    contributionSchedulePaymentList = contributionSchedulePaymentRepository.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable)
                            .parallelStream()
                            .filter(cont -> {
                                Contributions contributions = contributionRepository.findById(cont.getContributionId()).orElse(null);
                                return (contributions != null && groupWrapper.getName().toLowerCase().equalsIgnoreCase(group));
                            }).collect(Collectors.toList());
                } else {
                    return new UniversalResponse("failed", String.format("Group search by name %s failed", group), new ArrayList<>());
                }
            }

            List<ContributionSchedulePaymentWrapper> contributionSchedulePaymentWrapperList = contributionSchedulePaymentList
                    .parallelStream()
                    .map(cont -> {
                        Contributions contributions = contributionRepository.findById(cont.getContributionId()).orElse(new Contributions());
                        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contributions.getMemberGroupId());
                        return ContributionSchedulePaymentWrapper.builder()
                                .schedulePaymentId(cont.getId())
                                .groupName(groupWrapper.getName())
                                .contributionPaymentId(cont.getContributionId())
                                .contributionName(contributions.getName())
                                .contributionStartDate(contributions.getStartDate())
                                .contributionType(contributions.getContributionType().getName())
                                .scheduleType(contributions.getScheduleType().getName())
                                .expectedContributionDate(cont.getExpectedContributionDate())
                                .createdOn(cont.getCreatedOn())
                                .scheduledId(cont.getContributionScheduledId())
                                .build();
                    }).collect(Collectors.toList());

            Map<String, List<ContributionSchedulePaymentWrapper>> paymentWrapperMap = contributionSchedulePaymentWrapperList.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            paymentWrapperMap.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            return new UniversalResponse("success", "Contributions Schedule Payment", Map.of("data", responseData));
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getContributionPaymentReport(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<ContributionPayment> contributionPaymentList;
            GroupWrapper groups = chamaKycService.findGroupsByActiveAndSoftDeleteAndName(true, false, group);

            if (group.equalsIgnoreCase("all")) {
                contributionPaymentList = contributionsPaymentRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable);
            } else {
                if (groups != null) {
                    contributionPaymentList = contributionsPaymentRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable)
                            .parallelStream()
                            .filter(cont -> {
                                Contributions contributions = contributionRepository.findById(cont.getContributionId()).orElse(null);
                                return (contributions != null && groups.getName().toLowerCase().equalsIgnoreCase(group));
                            }).collect(Collectors.toList());
                } else {
                    return new UniversalResponse("failed", String.format("Group search by name %s failed", group), new ArrayList<>());
                }
            }
            List<ContributionPaymentWrapper> paymentWrapperList = contributionPaymentList.stream()
                    .map(cont -> {
                        Contributions contributions = contributionRepository.findById(cont.getContributionId()).orElse(new Contributions());
                        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contributions.getMemberGroupId());
                        return ContributionPaymentWrapper.builder()
                                .contributionPaymentId(cont.getContributionId())
                                .contributionName(contributions.getName())
                                .groupName(groupWrapper.getName())
                                .contributionType(contributions.getContributionType().getName())
                                .scheduleType(contributions.getScheduleType().getName())
                                .transactionId(cont.getTransactionId())
                                .paymentStatus(cont.getPaymentStatus())
                                .amount(cont.getAmount())
                                .phoneNumber(String.valueOf(cont.getPhoneNumber()))
                                .createdOn(cont.getCreatedOn())
                                .mpesaPaymentId(cont.getMpesaPaymentId())
                                .paymentFailureReason(cont.getPaymentFailureReason())
                                .mpesaCheckoutId(cont.getMpesaCheckoutId())
                                .paymentType(cont.getPaymentType())
                                .receiptImageUrl(cont.getReceiptImageUrl())
                                .isPenalty(cont.getIsPenalty())
                                .isCombinedPayment(cont.getIsCombinedPayment() != null ? cont.getIsCombinedPayment() : false)
                                .build();
                    }).collect(Collectors.toList());

            Map<String, List<ContributionPaymentWrapper>> paymentWrapperMap = paymentWrapperList.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            paymentWrapperMap.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            return new UniversalResponse("success", "Contributions Payment", Map.of("data", responseData));
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getTransactionsLogsByGroup(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<TransactionsLog> transactionsLogList;
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (group.equalsIgnoreCase("all")) {
                transactionsLogList = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable);
            } else {
                if (groups != null) {
                    transactionsLogList = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable)
                            .parallelStream()
                            .filter(transactionsLog -> transactionsLog.getContributions().getMemberGroupId() == groups.getId())
                            .collect(Collectors.toList());
                } else {
                    return new UniversalResponse("failed", String.format("Group search with name %s failed", group), new ArrayList<>());
                }
            }
            List<TransactionLogsWrapper> transactionLogsWrapperList = transactionsLogList
                    .parallelStream()
                    .map(this::mapToLogsWrapper)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            Map<String, List<TransactionLogsWrapper>> transactionLogs = transactionLogsWrapperList.parallelStream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn()
                            .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
            List<Map<Object, Object>> responseData = new ArrayList<>();
            transactionLogs.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
            responseData.sort(mapComparator());
            Map<String, Object> responseMessage = Map.of("data", responseData);
            return new UniversalResponse("success", "Transaction logs by group", responseMessage);
        }).publishOn(Schedulers.boundedElastic());
    }

    private TransactionLogsWrapper mapToLogsWrapper(TransactionsLog transactionsLog) {
        GroupWrapper group = chamaKycService.getMonoGroupById(transactionsLog.getContributions().getMemberGroupId());

        if (group == null) {
            log.info("Group with id {} not found.", transactionsLog.getContributions().getMemberGroupId());
            return null;
        }

        return TransactionLogsWrapper.builder()
                .id(transactionsLog.getId())
                .debitPhonenUmber(transactionsLog.getDebitphonenumber())
                .transactionAmount(transactionsLog.getTransamount())
                .contributionId(transactionsLog.getContributions().getId())
                .contributionsName(transactionsLog.getContributions().getName())
                .groupId(group.getId())
                .groupName(group.getName())
                .createdOn(transactionsLog.getCreatedOn())
                .contributionNarration(transactionsLog.getContributionNarration())
                .build();
    }
}
