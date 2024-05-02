package com.eclectics.chamapayments.service.impl;

//import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.config.CustomAuthenticationUtil;
import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.model.jpaInterfaces.UpcomingContributionsProjection;
import com.eclectics.chamapayments.repository.*;
import com.eclectics.chamapayments.service.*;
import com.eclectics.chamapayments.service.enums.GeneralEnums;
import com.eclectics.chamapayments.service.enums.PaymentEnum;
import com.eclectics.chamapayments.service.enums.TransactionType;
import com.eclectics.chamapayments.util.TransactionIdGenerator;
import com.eclectics.chamapayments.wrappers.request.AccountDto;
import com.eclectics.chamapayments.wrappers.request.ContributionPaymentDto;
import com.eclectics.chamapayments.wrappers.request.MakecontributionWrapper;
import com.eclectics.chamapayments.wrappers.request.RequestwithdrawalWrapper;
import com.eclectics.chamapayments.wrappers.response.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;

import javax.annotation.PostConstruct;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.eclectics.chamapayments.util.RequestConstructor.constructBody;
import static com.eclectics.chamapayments.util.RequestConstructor.getBalanceInquiryReq;
import static java.util.stream.Collectors.groupingBy;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountingServiceImpl implements AccountingService {
    private final AccountTypeRepository accountTypeRepository;
    private final LoanPenaltyRepository loanPenaltyRepository;

    private final LoanproductsRepository loanproductsRepository;
    private final LoansdisbursedRepo loansdisbursedRepo;
    private final GroupRepository groupRepository;
    private final LoansrepaymentRepo loansrepaymentRepo;
    private final AccountsRepository accountsRepository;
    private final LoanService loanService;
    private final OutstandingContributionPaymentRepository outstandingContributionPaymentRepository;
    private final ContributionTypesRepository contributionTypesRepository;
    private final ScheduleTypeRepository scheduleTypesRepository;
    private final ContributionRepository contributionsRepository;
    private final ContributionLoanRepository contributionLoanRepository;
    private final TransactionlogsRepo transactionlogsRepo;
    private final AmounttypeRepo amounttypeRepo;
    private final WithdrawallogsRepo withdrawallogsRepo;
    private final InvestmentsRepo investmentsRepo;
    private final TransactionspendingaApprovalRepo transactionspendingaApprovalRepo;
    private final WithdrawalspendingapprovalRepo withdrawalspendingapprovalRepo;
    private final ContributionsPaymentRepository contributionsPaymentRepository;
    private final ContributionSchedulePaymentRepository contributionSchedulePaymentRepository;
    private final LoanapplicationsRepo loanapplicationsRepo;
    private final ChamaKycService chamaKycService;
    private final FileHandlerService fileHandlerService;
    private final NotificationService notificationService;
    private final PublishingService publishingService;
    private final PenaltyRepository penaltyRepository;
    private final ESBLoggingService esbLoggingService;
    private final OverpaidContributionRepository overpaidContributionRepository;
    BiPredicate<Long, String> groupFilterByGroupNameParamId;
    private final NumberFormat numberFormat;
    private final ESBService esbService;
    private final ResourceBundleMessageSource source;

    @Value("${vicoba.url}")
    private String vicobaUrl;
    private WebClient webClient;

    JsonParser jsonParser = new JsonParser();
    Gson gson = new Gson();

    @PostConstruct
    private void init() {
        groupFilterByGroupNameParamId = (groupId, filterName) -> {
            String groupName = chamaKycService.getMonoGroupNameByGroupId(groupId);
            if (groupName == null || !filterName.equalsIgnoreCase("all")) {
                return filterName.equalsIgnoreCase(groupName);
            }
            return true;
        };

        webClient = WebClient
                .builder()
                .baseUrl(vicobaUrl)
                .build();
    }

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    @Override
    public List<AccountType> findAccountTypes() {
        return accountTypeRepository.findAll();
    }

    @Override
    public AccountType getAccounttypebyName(String name) {
        return accountTypeRepository.findByAccountNameContains(name);
    }

    @Override
    public boolean checkIfGrouphasWalletAccount(long groupId) {
        AccountType accountType = getAccounttypebyName("WALLET");
        return accountsRepository.countByGroupIdAndAccountType(groupId, accountType) > 0;
    }

    @Override
    public Optional<AccountType> getAccounttypebyID(long accounttypeid) {
        return accountTypeRepository.findById(accounttypeid);
    }

    @Override
    public boolean checkAccounttype(String name) {
        return accountTypeRepository.existsAccountTypeByAccountName(name);
    }

    @Override
    public void saveAccounttype(AccountType accountType) {
        accountTypeRepository.save(accountType);
    }

    @Override
    public boolean checkIfAccountExists(String accountnumber, long groupId) {
        return accountsRepository.countByAccountdetailsAndGroupId(accountnumber, groupId) > 0;
    }

    @Override
    public void createAccount(Accounts accounts) {
        accountsRepository.save(accounts);
    }

    @Override
    public List<AccountDto> getAccountbyGroup(long groupId) {
        return accountsRepository.findByGroupIdAndActive(groupId, true)
                .stream()
                .map(a -> AccountDto.builder()
                        .accountId(a.getId())
                        .groupId(a.getGroupId())
                        .accountbalance(a.getAccountbalance())
                        .active(a.isActive())
                        .availableBal(a.getAvailableBal())
                        .name(a.getName())
                        .accountdetails(gson.fromJson(a.getAccountdetails(), AccountDetails.class))
                        .accountType(a.getAccountType())
                        .build()).collect(Collectors.toList());
    }

    @Override
    public List<Accounts> getAccountsbyGroupandType(long groupId, AccountType accountType) {
        return accountsRepository.findByGroupIdAndAccountTypeAndActive(groupId, accountType, true);
    }

    @Override
    public List<ContributionType> getContributiontypes() {
        return contributionTypesRepository.findAll();
    }

    @Override
    public void saveContributiontype(ContributionType contributionType) {
        contributionTypesRepository.save(contributionType);
    }

    @Override
    public boolean checkContributiontype(String name) {
        return contributionTypesRepository.countByName(name) > 0;
    }

    @Override
    public List<ScheduleTypes> getScheduletypes() {
        return scheduleTypesRepository.findAll();
    }

    @Override
    public boolean checkScheduletype(String name) {
        return scheduleTypesRepository.countByName(name) > 0;
    }

    @Override
    public void saveScheduleType(ScheduleTypes scheduleTypes) {
        scheduleTypesRepository.save(scheduleTypes);
    }

    @Override
    public boolean checkAmounttype(String name) {
        return amounttypeRepo.countByName(name) > 0;
    }

    @Override
    public void createAmounttype(AmountType amountType) {
        amounttypeRepo.save(amountType);
    }

    @Override
    public List<AmountType> getAmounttypes() {
        return amounttypeRepo.findAll();
    }

    @Override
    public Optional<AmountType> getAmounttypebyID(long id) {
        return amounttypeRepo.findById(id);
    }

    @Override
    public Optional<ContributionType> getContributiontypebyID(long contributiontypeid) {
        return contributionTypesRepository.findById(contributiontypeid);
    }

    @Override
    public double getBalancebyContributions(Contributions contributions) {
        double totalpayments = getSumbycontributions(contributions);
        double totalwithdrawals = getTotalWithdrawalbyContribution(contributions);
        return totalpayments - totalwithdrawals;
    }

    public double getTotalWithdrawalbyContribution(Contributions contributions) {
        return withdrawallogsRepo.getTotalbyContribution(contributions);
    }

    @Override
    public Optional<ScheduleTypes> getScheduletypebyID(long schedultypeid) {
        return scheduleTypesRepository.findById(schedultypeid);
    }

    @Override
    public int countContributionsbyNameandGroup(String name, long groupId) {
        return contributionsRepository.countByNameAndMemberGroupId(name, groupId);
    }

    @Override
    public Contributions createContribution(Contributions contributions) {
        return contributionsRepository.save(contributions);
    }

    @Override
    public List<Contributions> getContributionsbyGroup(long groupId, Pageable pageable) {
        return contributionsRepository.findByMemberGroupId(groupId, pageable);
    }

    @Override
    public int countcontributionsByGroup(long groupId) {
        return contributionsRepository.countByMemberGroupId(groupId);
    }

    @Override
    public Optional<Accounts> getAccountbyId(long accountid) {
        return accountsRepository.findById(accountid);
    }

    @Override
    public Optional<Contributions> getContributionbyId(long contributionid) {
        return contributionsRepository.findById(contributionid);
    }

    @Override
    public Optional<TransactionsPendingApproval> getPendingPaymentapprovalbyId(long id) {
        return transactionspendingaApprovalRepo.findByIdAndPendingTrue(id);
    }

    @Override
    public Optional<WithdrawalsPendingApproval> getPendingWithdrawalapprovalbyId(long id) {
        return withdrawalspendingapprovalRepo.findByIdAndPendingTrue(id);
    }

    @Override
    public long countAllContributions() {
        return contributionsRepository.count();
    }

    @Override
    public int countActiveContributions() {
        return contributionsRepository.countByActiveTrue();
    }

    @Override
    public int countInactiveContributions() {
        return contributionsRepository.countByActiveFalse();
    }

    @Override
    public double getSumbycontributions(Contributions contributions) {
        return transactionlogsRepo.getTotalbyContribution(contributions);
    }

    @Override
    public Mono<UniversalResponse> approveContributionPayment(long paymentId, boolean approved, String approvedBy) {
        return Mono.fromCallable(() -> {
            TransactionsPendingApproval transactionsPendingApproval =
                    transactionspendingaApprovalRepo.findByIdAndPendingTrue(paymentId)
                            .orElse(null);
            if (transactionsPendingApproval == null)
                return new UniversalResponse("fail", getResponseMessage("transactionPendingApprovalNotFound"));

            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(approvedBy);
            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("approverNotFound"));

            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(transactionsPendingApproval.getContribution().getMemberGroupId());

            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            if (!groupWrapper.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsInactive"));

            if (!approved) {
                transactionsPendingApproval.setApproved(false);
                transactionsPendingApproval.setApprovedby(approvedBy);
                transactionsPendingApproval.setPending(false);
                transactionsPendingApproval.setLastModifiedDate(new Date());
            } else {
                Accounts accounts = transactionsPendingApproval.getAccount();
                Contributions contributions = transactionsPendingApproval.getContribution();

                TransactionsLog transactionsLog = new TransactionsLog();
                transactionsLog.setContributionNarration(transactionsPendingApproval.getContribution_narration());
                transactionsLog.setCreditaccounts(accounts);
                transactionsLog.setDebitphonenumber(transactionsPendingApproval.getPhonenumber());
                String transid = accounts.getAccountType().getAccountPrefix().concat(String.valueOf(new Date().getTime()));
                transactionsLog.setUniqueTransactionId(transid);
                transactionsLog.setOldbalance(accounts.getAvailableBal());
                transactionsLog.setNewbalance(accounts.getAvailableBal() + transactionsPendingApproval.getAmount());
                transactionsLog.setTransamount(transactionsPendingApproval.getAmount());
                transactionsLog.setCapturedby(transactionsPendingApproval.getCapturedby());
                transactionsLog.setApprovedby(approvedBy);
                transactionsLog.setContributions(contributions);

                transactionsPendingApproval.setApproved(true);
                transactionsPendingApproval.setApprovedby(approvedBy);
                transactionsPendingApproval.setPending(false);

                //set new account balance
                accounts.setAvailableBal(transactionsLog.getNewbalance());

                transactionlogsRepo.save(transactionsLog);
                accountsRepository.save(accounts);
                transactionspendingaApprovalRepo.save(transactionsPendingApproval);

                updateContributionPayment(transactionsPendingApproval.getContributionPaymentId());
                sendContributionTextToMembers(memberWrapper, groupWrapper.getId(), (int) transactionsPendingApproval.getAmount(), false);
                return new UniversalResponse("success", getResponseMessage("transactionApprovalAccepted"));
            }
            try {
                notificationService.sendContributionFailureMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), (int) transactionsPendingApproval.getAmount(), memberWrapper.getLanguage());
            } catch(Exception e){
                log.error("sending contribution message failure {}",e.getMessage());
            }

            transactionspendingaApprovalRepo.save(transactionsPendingApproval);
            return new UniversalResponse("success", getResponseMessage("transactionApprovalDeclined"));
        }).publishOn(Schedulers.boundedElastic());
    }

    private void updateContributionPayment(Long id) {
        Optional<ContributionPayment> contributionPaymentOptional = contributionsPaymentRepository.findById(id);
        if (contributionPaymentOptional.isEmpty()) {
            return;
        }

        ContributionPayment contributionPayment = contributionPaymentOptional.get();
        contributionPayment.setPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());
        contributionsPaymentRepository.save(contributionPayment);
    }

    @Override
    public List<PaymentApproval> getPendingPaymentApprovalByGroupId(long groupid) {
        return transactionspendingaApprovalRepo.findByGroupandPendingTrue(groupid)
                .stream()
                .map(transactionsPendingApprovalPaymentApprovalFunction())
                .sorted().collect(Collectors.toList());
    }

    public String loadReceiptImageUrl(long paymentId) {
        ContributionPayment payment = contributionsPaymentRepository
                .findById(paymentId).orElse(null);
        if (payment != null) {
            String path = payment.getReceiptImageUrl();
            if (!path.isEmpty()) {
                return fileHandlerService.getFileUrl(path);
            }
        }
        return "";
    }

    @Override
    public List<PaymentApproval> getPendingPaymentApprovalByUser(String phonenumber) {
        return transactionspendingaApprovalRepo.findByPhonenumberAndPendingTrue(phonenumber)
                .stream()
                .map(transactionsPendingApprovalPaymentApprovalFunction())
                .sorted().collect(Collectors.toList());
    }

    @Override
    public List<PaymentApproval> getPendingPaymentApprovalByContribution(Contributions contributions) {
        return transactionspendingaApprovalRepo.findByContributionAndPendingTrue(contributions)
                .stream()
                .map(transactionsPendingApprovalPaymentApprovalFunction())
                .sorted().collect(Collectors.toList());
    }

    Function<TransactionsPendingApproval, PaymentApproval> transactionsPendingApprovalPaymentApprovalFunction() {
        return p -> PaymentApproval.builder()
                .creditaccountid(p.getAccount().getId())
                .creditaccountname(p.getAccount().getName())
                .creditaccounttype(p.getAccount().getAccountType().getAccountName())
                .amount(p.getAmount())
                .capturedby(p.getCapturedby())
                .contributionid(p.getContribution().getId())
                .debitaccount(p.getPhonenumber())
                .narration(p.getContribution_narration())
                .paymentid(p.getId())
                .appliedon(p.getCreatedOn())
                .receiptImageUrl(loadReceiptImageUrl(p.getContributionPaymentId()))
                .build();
    }

    @Override
    public PageDto getPendingWithdrawalRequestByGroupId(long groupId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.abs(page), Math.min(size, 50));

        Page<WithdrawalsPendingApproval> pagedData = withdrawalspendingapprovalRepo.findByGroupandPendingTrue(groupId, pageable);

        List<WithdrawalApproval> withdrawalApprovalList = pagedData.getContent()
                .stream()
                .map(mapWithdrawalsToWithdrawalResponse())
                .collect(Collectors.toList());

        return new PageDto(pagedData.getNumber(), pagedData.getTotalPages(), withdrawalApprovalList);
    }

    Function<WithdrawalsPendingApproval, WithdrawalApproval> mapWithdrawalsToWithdrawalResponse() {
        return p -> WithdrawalApproval.builder()
                .amount(p.getAmount())
                .capturedby(p.getCapturedby())
                .contributionid(p.getContribution().getId())
                .creditaccount(p.getPhonenumber())
                .debitaccountid(p.getAccount().getId())
                .debitaccountname(p.getAccount().getName())
                .debitaccounttype(p.getAccount().getAccountType().getAccountName())
                .requestid(p.getId())
                .withdrawal_narration(p.getWithdrawal_narration())
                .withdrawalreason(p.getWithdrawalreason())
                .status(p.getStatus())
                .appliedon(p.getCreatedOn())
                .build();
    }

    @Override
    public List<WithdrawalApproval> getPendingWithdrawalRequestbyUser(String phonenumber) {
        return withdrawalspendingapprovalRepo.findByPhonenumberAndPendingTrue(phonenumber)
                .stream()
                .map(mapWithdrawalsToWithdrawalResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<WithdrawalApproval> getPendingWithdrawalRequestByContribution(Contributions contributions) {
        return withdrawalspendingapprovalRepo.findByContributionAndPendingTrue(contributions)
                .stream()
                .map(mapWithdrawalsToWithdrawalResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    Function<TransactionsLog, TransactionLogWrapper> mapTransactionLogToWrapperResponse() {
        return p -> {
            Optional<String> groupName = chamaKycService.getGroupNameByGroupId(p.getContributions().getMemberGroupId());
            Optional<MemberWrapper> memberWrapperOptional = chamaKycService.searchMemberByPhoneNumber(p.getDebitphonenumber());
            if (groupName.isEmpty() || memberWrapperOptional.isEmpty()) return null;
            MemberWrapper memberWrapper = memberWrapperOptional.get();
            String memberName = memberWrapper.getFirstname().concat(" ").concat(memberWrapper.getLastname());
            return TransactionLogWrapper.builder()
                    .amount(p.getTransamount())
                    .capturedby(p.getCapturedby())
                    .contributionid(p.getContributions().getId())
                    .contributionname(p.getContributions().getName())
                    .creditaccount(parseAccount(p.getCreditaccounts()))
                    .creditaccountname(p.getCreditaccounts().getName())
                    .debitaccount(p.getDebitphonenumber())
                    .narration(p.getContributionNarration())
                    .transactionid(p.getUniqueTransactionId())
                    .transactiondate(p.getCreatedOn())
                    .groupname(groupName.get())
                    .membername(memberName)
                    .accounttype(p.getCreditaccounts().getAccountType().getAccountName())
                    .build();
        };
    }

    Function<WithdrawalLogs, TransactionLogWrapper> mapWithdrawalLogsToWrapperResponse() {
        return p -> {
            Optional<MemberWrapper> memberWrapperOptional = chamaKycService.searchMemberByPhoneNumber(p.getCreditphonenumber());
            Optional<String> groupName = chamaKycService.getGroupNameByGroupId(p.getContributions().getMemberGroupId());
            if (memberWrapperOptional.isEmpty() || groupName.isEmpty()) return null;
            MemberWrapper memberWrapper = memberWrapperOptional.get();
            String memberName = memberWrapper.getFirstname().concat(" ").concat(memberWrapper.getLastname());
            return TransactionLogWrapper.builder()
                    .amount(p.getTransamount())
                    .capturedby(p.getCapturedby())
                    .contributionid(p.getContributions().getId())
                    .contributionname(p.getContributions().getName())
                    .creditaccount(p.getCreditphonenumber())
                    .creditaccountname(memberName)
                    .debitaccount(memberWrapper.getImsi())
                    .narration(p.getContribution_narration())
                    .transactionid(p.getUniqueTransactionId())
                    .transactiondate(p.getCreatedOn())
                    .groupname(groupName.get())
                    .membername(memberName)
                    .accounttype(p.getDebitAccounts().getAccountType().getAccountName())
                    .build();
        };
    }

    String parseAccount(Accounts accounts) {
        String raw_requiredfields = accounts.getAccountType().getAccountFields();
        JsonArray fields = jsonParser.parse(raw_requiredfields).getAsJsonArray();
        String accountdetail_field = "na";
        if (fields.contains(jsonParser.parse("account_number"))) {
            JsonObject accountdetails = jsonParser.parse(accounts.getAccountdetails()).getAsJsonObject();
            accountdetail_field = accountdetails.get("account_number").getAsString();
        }
        return accountdetail_field;

    }

    @Override
    public List<TransactionLogWrapper> getTransactionsbyGroup(long groupid, Pageable pageable) {
        return transactionlogsRepo.getTransactionsbygroup(groupid, pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());
    }

    @Override
    public UniversalResponse getTransactionsByGroup(long groupid, Pageable pageable) {
        Page<TransactionsLog> pagedData = transactionlogsRepo.getTransactionsbygroup(groupid, pageable);

        List<TransactionLogWrapper> data = pagedData.getContent()
                .parallelStream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());

        return UniversalResponse.builder()
                .status("Success")
                .message("Transactions by group")
                .data(data)
                .metadata(Map.of("currentPage", pagedData.getNumber(), "numOfRecords", pagedData.getNumberOfElements(), "totalPages", pagedData.getTotalPages()))
                .timestamp(new Date())
                .build();
    }

    @Override
    public int countTransactionsByGroup(long groupid) {
        return transactionlogsRepo.countTransactionsbygroup(groupid);
    }

    @Override
    public int countWithdrawalsByGroup(long groupid) {
        return withdrawallogsRepo.countWithdrawalbygroup(groupid);
    }

    @Override
    public List<TransactionLogWrapper> getWithdrawalsbyGroup(long groupid, Pageable pageable) {
        return withdrawallogsRepo.getWithdrawalsbygroup(groupid, pageable)
                .stream()
                .map(mapWithdrawalLogsToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionLogWrapper> getTransactionsbyUser(String phonenumber, Pageable pageable) {
        return transactionlogsRepo.findByDebitphonenumberOrderByCreatedOnDesc(phonenumber, pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());
    }

    @Override
    public UniversalResponse getTransactionsByUser(String phonenumber, Pageable pageable) {
        Page<TransactionsLog> pagedData = transactionlogsRepo.findByDebitphonenumberOrderByCreatedOnDesc(phonenumber, pageable);

        List<TransactionLogWrapper> data = pagedData.getContent()
                .parallelStream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());
        return UniversalResponse.builder()
                .status("Success")
                .message("Transactions by user")
                .data(data)
                .metadata(Map.of("currentPage", pagedData.getNumber(), "numOfRecords", pagedData.getNumberOfElements(), "totalPages", pagedData.getTotalPages()))
                .timestamp(new Date())
                .build();
    }

    @Override
    public int countTransactionsbyUser(String phonenumber) {
        return transactionlogsRepo.countByDebitphonenumber(phonenumber);
    }

    @Override
    public int countWithdrawalsbyUser(String phonenumber) {
        return withdrawallogsRepo.countByCreditphonenumber(phonenumber);
    }

    @Override
    public List<TransactionLogWrapper> getWithdrawalsbyUser(String phonenumber, Pageable pageable) {
        return withdrawallogsRepo.findByCreditphonenumber(phonenumber, pageable)
                .stream()
                .map(mapWithdrawalLogsToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionLogWrapper> getTransactionByContributions(Long contributionId, Pageable pageable) {
        Optional<Contributions> contribution = contributionsRepository.findById(contributionId);

        if (contribution.isEmpty()) return Collections.emptyList();

        return transactionlogsRepo.findByContributionsOrderByCreatedOnDesc(contribution.get(), pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public UniversalResponse getTransactionsByContributions(Long contributionId, Pageable pageable) {
        Optional<Contributions> contribution = contributionsRepository.findById(contributionId);

        if (contribution.isEmpty()) return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));

        Page<TransactionsLog> pagedData = transactionlogsRepo.findByContributionsOrderByCreatedOnDesc(contribution.get(), pageable);

        List<TransactionLogWrapper> data = pagedData.getContent()
                .parallelStream()
                .map(mapTransactionLogToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
        return UniversalResponse.builder()
                .status("Success")
                .message("Transactions by contributions")
                .data(data)
                .metadata(Map.of("currentPage", pagedData.getNumber(), "numOfRecords", pagedData.getNumberOfElements(), "totalPages", pagedData.getTotalPages()))
                .timestamp(new Date())
                .build();
    }

    @Override
    public int countTransactionByContributions(Contributions contributions) {
        return transactionlogsRepo.countByContributions(contributions);
    }

    @Override
    public int countWithdrawalsbyContribution(Contributions contributions) {
        return withdrawallogsRepo.countByContributions(contributions);
    }

    @Override
    public List<TransactionLogWrapper> getWithdrawalsbyContribution(Long contributionId, Pageable pageable) {
        Optional<Contributions> contribution = contributionsRepository.findById(contributionId);

        if (contribution.isEmpty()) return Collections.emptyList();

        return withdrawallogsRepo.findByContributions(contribution.get(), pageable)
                .stream()
                .map(mapWithdrawalLogsToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public List<TransactionLogWrapper> getTransactionsbyUserandContributions(String phonenumber, Contributions contributions, Pageable pageable) {
        return transactionlogsRepo.findByDebitphonenumberAndContributionsOrderByCreatedOnDesc(phonenumber, contributions, pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());
    }

    @Override
    public int countTransactionsbyUserandContributions(String phonenumber, Contributions contributions) {
        return transactionlogsRepo.countByDebitphonenumberAndContributions(phonenumber, contributions);
    }

    @Override
    public int countTransactionsbyUserandGroupId(String phonenumber, long groupId) {
        return transactionlogsRepo.countTransactionsbygroupandmember(groupId, phonenumber);
    }

    @Override
    public List<TransactionLogWrapper> getTransactionsbyUserandGroupId(String phonenumber, long groupId, Pageable pageable) {
        return transactionlogsRepo.getTransactionsbygroupandmember(groupId, phonenumber, pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .collect(Collectors.toList());
    }

    @Override
    public int countTransactionsbyAccount(Accounts accounts) {
        return transactionlogsRepo.countByCreditaccounts(accounts);
    }

    @Override
    public List<TransactionLogWrapper> getTransactionsbyAccount(Long accountId, Pageable pageable) {
        Optional<Accounts> account = accountsRepository.findById(accountId);

        if (account.isEmpty()) return Collections.emptyList();

        return transactionlogsRepo.findByCreditaccountsOrderByCreatedByDesc(account.get(), pageable)
                .stream()
                .map(mapTransactionLogToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public UniversalResponse getTransactionsByAccount(Long accountId, Pageable pageable) {
        Optional<Accounts> account = accountsRepository.findById(accountId);

        if (account.isEmpty()) return new UniversalResponse("fail", getResponseMessage("accountNotFound"));

        Page<TransactionsLog> pagedData = transactionlogsRepo.findByCreditaccountsOrderByCreatedByDesc(account.get(), pageable);

        List<TransactionLogWrapper> data = pagedData.getContent()
                .parallelStream()
                .map(mapTransactionLogToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());

        return UniversalResponse.builder()
                .status("Success")
                .message("Transactions by account")
                .data(data)
                .metadata(Map.of("currentPage", pagedData.getNumber(), "numOfRecords", pagedData.getNumberOfElements(), "totalPages", pagedData.getTotalPages()))
                .timestamp(new Date())
                .build();
    }

    @Override
    public int countWithdrawalsbyAccount(Accounts accounts) {
        return withdrawallogsRepo.countByDebitAccounts(accounts);
    }

    @Override
    public List<TransactionLogWrapper> getWithdrawalsbyAccount(Long accountId, Pageable pageable) {
        Optional<Accounts> account = accountsRepository.findById(accountId);

        if (account.isEmpty()) return Collections.emptyList();

        return withdrawallogsRepo.findByDebitAccounts(account.get(), pageable)
                .stream()
                .map(mapWithdrawalLogsToWrapperResponse())
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public double getAverageheldinAccounts() {
        return accountsRepository.getAverageheldinAccounts();
    }

    @Override
    public double getSumheldinAccounts() {
        return accountsRepository.getTotalheldinAccounts();
    }

    @Override
    public long getAccountsCount() {
        return accountsRepository.count();
    }

    @Override
    public long getPaymentsCount() {
        return transactionlogsRepo.count();
    }

    @Override
    public double getPaymentsTotal() {
        return transactionlogsRepo.getSumContributed();
    }

    @Override
    public double getPaymentsAvg() {
        return transactionlogsRepo.getAvgContributed();
    }

    @Override
    public long getWithdrawalsCount() {
        return withdrawallogsRepo.count();
    }

    @Override
    public double getWithdrawalsTotal() {
        return withdrawallogsRepo.getSumContributed();
    }

    @Override
    public double getWithdrawalsAvg() {
        return withdrawallogsRepo.getAvgContributed();
    }

    @Override
    public void recordInvestment(Investments investments) {
        investmentsRepo.save(investments);
    }

    @Override
    public boolean checkInvestmentRecord(String name, long groupId) {
        return investmentsRepo.countByNameAndGroupId(name, groupId) > 0;
    }

    Function<Investments, InvestmentWrapper> mapInvestmentsToWrapperResponse() {
        return p -> {
            String groupName = chamaKycService.getMonoGroupNameByGroupId(p.getGroupId());
            if (groupName == null) return null;
            return InvestmentWrapper.builder()
                    .description(p.getDescription())
                    .groupid(p.getGroupId())
                    .groupname(groupName)
                    .id(p.getId())
                    .managername(p.getManagername())
                    .name(p.getName())
                    .value(p.getValue())
                    .build();
        };
    }

    @Override
    public List<InvestmentWrapper> getInvestmentrecordsbyGroup(long groupId, Pageable pageable) {
        return investmentsRepo.findByGroupId(groupId, pageable)
                .stream()
                .map(mapInvestmentsToWrapperResponse())
                .collect(Collectors.toList());
    }

    @Override
    public int countInvestmentrecordsbyGroup(long groupId) {
        return investmentsRepo.countByGroupId(groupId);
    }

    static Map<String, TemporalAdjuster> timeAdjusters() {
        Map<String, TemporalAdjuster> adjusterHashMap = new HashMap<>();
        adjusterHashMap.put("days", TemporalAdjusters.ofDateAdjuster(d -> d)); // identity
        adjusterHashMap.put("weeks", TemporalAdjusters.previousOrSame(DayOfWeek.of(1)));
        adjusterHashMap.put("months", TemporalAdjusters.firstDayOfMonth());
        adjusterHashMap.put("years", TemporalAdjusters.firstDayOfYear());
        return adjusterHashMap;
    }

    static Comparator<Map<String, Object>> mapComparator() {
        return new Comparator<Map<String, Object>>() {
            @SneakyThrows
            public int compare(Map<String, Object> m1, Map<String, Object> m2) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date date1 = sdf.parse(m1.get("dateofday").toString());
                Date date2 = sdf.parse(m2.get("dateofday").toString());
                return date1.compareTo(date2);
            }
        };
    }

    @Override
    public Optional<Investments> getInvestmentbyID(long id) {
        return investmentsRepo.findById(id);
    }


    BiPredicate<String, String> groupFilterByGroupNameParamName = (groupName, filterName) -> {
        if (!filterName.equalsIgnoreCase("all")) {
            return groupName.equalsIgnoreCase(filterName);
        }
        return true;
    };

    @Override
    public List<Map<String, Object>> groupTransactionsDetailed(Date startDate, Date endDate, String period, String group) {
        List<TransactionsLog> transactionsLogList = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate);
        List<WithdrawalLogs> withdrawalLogsList = withdrawallogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate);
        Period dp = Period.between(endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        int diffDays = Math.abs(dp.getDays());

        long groupsTransCount = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .count();
        double totalTransactedAmount = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        double totalGroupPayments = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .map(t -> {
                    double diff = t.getNewbalance() - t.getOldbalance();
                    return diff > 0 ? diff : 0;
                })
                .reduce(0.0, Double::sum);

        double totalGroupPaymentsCount = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(t -> (t.getNewbalance() - t.getOldbalance()) > 0)
                .count();

        double totalGroupWithdrawals = withdrawalLogsList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .mapToDouble(WithdrawalLogs::getTransamount)
                .sum();

        double walletTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 1)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        long countWalletTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 1)
                .count();


        double bankTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 2)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        double mobileMoneyTotal = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 3)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        double saccoTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 4)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        double pettyCashTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 5)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        double investmentTotals = transactionsLogList.stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .filter(transactionsLog -> transactionsLog.getCreditaccounts().getAccountType().getId() == 7)
                .map(TransactionsLog::getTransamount)
                .reduce(0.0, Double::sum);

        long totalGroupWithdrawalsCount = withdrawalLogsList.size();

        double transactedAmtAvg = totalTransactedAmount / diffDays;
        double groupPaymentAvg = totalGroupPayments / diffDays;
        double withdrawalsAvg = totalGroupWithdrawals / diffDays;
        double walletAvg = walletTotals / diffDays;

        Map<String, Object> transactionsMap = new LinkedHashMap<>();
        transactionsMap.put("transactions Count", groupsTransCount);
        transactionsMap.put("total amount", totalTransactedAmount);
        transactionsMap.put("average transactions", transactedAmtAvg);

        Map<String, Object> groupPaymentMap = new LinkedHashMap<>();
        groupPaymentMap.put("payments count", totalGroupPaymentsCount);
        groupPaymentMap.put("total payments", totalGroupPayments);
        groupPaymentMap.put("average payments", groupPaymentAvg);

        Map<String, Object> groupWithdrawalsMap = new LinkedHashMap<>();
        groupWithdrawalsMap.put("withdrawals count", totalGroupWithdrawalsCount);
        groupWithdrawalsMap.put("total withdrawals", totalGroupWithdrawals);
        groupWithdrawalsMap.put("average withdrawals", withdrawalsAvg);

        Map<String, Object> walletTransactions = new LinkedHashMap<>();
        walletTransactions.put("transactions count", countWalletTotals);
        walletTransactions.put("total amount", walletTotals);
        walletTransactions.put("average", walletAvg);

        Map<String, Object> transactionsByAccountType = new LinkedHashMap<>();
        transactionsByAccountType.put("wallet", walletTotals);
        transactionsByAccountType.put("bank", bankTotals);
        transactionsByAccountType.put("mobileMoney", mobileMoneyTotal);
        transactionsByAccountType.put("sacco", saccoTotals);
        transactionsByAccountType.put("pettyCash", pettyCashTotals);
        transactionsByAccountType.put("investment", investmentTotals);

        Map<Object, Double> transData = transactionsLogList.stream()
                .filter(t -> groupFilterByGroupNameParamId.test(t.getContributions().getMemberGroupId(), group))
                .map(t -> {
                    if ((t.getNewbalance() - t.getOldbalance()) < 0) {
                        t.setTransamount(t.getTransamount() * -1);
                        return t;
                    }
                    return t;
                })
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), Collectors.summingDouble(TransactionsLog::getTransamount)));

        List<Map<String, Object>> transactionList = new ArrayList<>();
        transData.forEach((key, value) -> transactionList.add(new HashMap<>() {{
            put("dateofday", key);
            put("value", value);
        }}));

        transactionList.sort(mapComparator());
        Map<String, Object> transactionSummary = new LinkedHashMap<String, Object>() {{
            put("transaction", transactionsMap);
            put("transactionByAccounts", transactionsByAccountType);
            put("payment", groupPaymentMap);
            put("wallet", walletTransactions);
            put("withdrawal", groupWithdrawalsMap);
        }};
        Map<String, Object> transactionData = new LinkedHashMap<String, Object>() {{
            put("transactionData", transactionList);
        }};

        List<Map<String, Object>> response = new ArrayList<>();
        response.add(transactionSummary);
        response.add(transactionData);

        return response;
    }

    @Override
    public List<Map<String, Object>> groupTransactionsbyDate(Date startDate, Date endDate, String period, String group, String country) {
        List<TransactionsLog> data = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate)
                .stream()
                .filter(p -> groupFilterByGroupNameParamId.test(p.getContributions().getMemberGroupId(), group))
                .collect(Collectors.toList());

        Map<Object, Long> transData = data.stream()
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), Collectors.counting()));

        List<Map<String, Object>> response = new ArrayList<>();
        transData.forEach((key, value) -> response.add(Map.of("dateofday", key, "objects", value)));
        response.sort(mapComparator());
        return response;
    }

    @Override
    public List<Map<String, Object>> groupRegistrationTrend(Date startDate, Date endDate, String period, String group, String country) {
        Flux<GroupWrapper> groupsList = chamaKycService.findMonoGroupsCreatedBetweenOrderAsc(startDate, endDate);
        Flux<MemberWrapper> membersList = groupsList
                .filter(g -> groupFilterByGroupNameParamName.test(g.getName(), group))
                .flatMap(g -> chamaKycService.getFluxGroupMembers(g.getId()));

        List<Map<String, Object>> groupsResponse = new ArrayList<>();
        List<Map<String, Object>> membersResponse = new ArrayList<>();
        Map<String, Long> groupsData = groupsList
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), Collectors.counting()))
                .block();

        groupsData.forEach((key, value) -> groupsResponse.add(Map.of("dateofday", key, "objects", value)));

        Map<String, Long> membersData = membersList
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), Collectors.counting()))
                .block();
        membersData.forEach((key, value) -> membersResponse.add(Map.of("dateofday", key, "objects", value)));
        groupsResponse.sort(mapComparator());
        membersResponse.sort(mapComparator());
        List<Map<String, Object>> response = new ArrayList<>();
        if (group.equalsIgnoreCase("all")) {
            response.add(Map.of("groups", groupsResponse));
        } else {
            response.add(Map.of("groups", new ArrayList<>()));
        }
        response.add(Map.of("individuals", membersResponse));
        return response;
    }


    @Override
    public UniversalResponse getGroupTransactionByType(Date startDate, Date endDate, String period, String transactionType, String group, String additional, Pageable pageable) {
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
                return getLoansPendingApprovalbyLoanProduct(startDate, endDate, period, group, additional, pageable);
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
                return new UniversalResponse("fail", getResponseMessage("transactionByTpeNotFound") + transactionType);
        }
    }

    private UniversalResponse getWithdrawalLogs(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<WithdrawalLogs> withdrawalLogs;
        int numOfRecords = 0;
        if (group.equals("all")) {
            withdrawalLogs = withdrawallogsRepo.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable);
            numOfRecords = withdrawallogsRepo.countAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups != null) {
                withdrawalLogs = withdrawallogsRepo.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable)
                        .stream()
                        .filter(log -> groupFilterByGroupNameParamId.test(groups.getId(), group))
                        .collect(Collectors.toList());


            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<WithdrawLogsWrapper> withdrawLogsWrappers = withdrawalLogs.stream()
                .map(log -> WithdrawLogsWrapper.builder()
                        .transactionId(log.getUniqueTransactionId())
                        .contributionNarration(log.getContribution_narration())
                        .debitAccountId(log.getDebitAccounts().getId())
                        .contributionAccount(log.getContributions().getId())
                        .isContribution(null == log.getContributions())
                        .contributionName(null == log.getContributions() ? "" : log.getContributions().getName())
                        .isLoanApplication(null == log.getLoanApplications())
                        .loanApplicationId(log.getLoanApplications().getId())
                        .creditUserNumber(log.getCreditphonenumber())
                        .newBalance(log.getNewbalance())
                        .oldBalance(log.getOldbalance())
                        .capturedBy(log.getCapturedby())
                        .reason(log.getWithdrawalreason())
                        .transferToUserStatus(log.getTransferToUserStatus())
                        .createdOn(log.getCreatedOn())
                        .build()).collect(Collectors.toList());

        Map<String, List<WithdrawLogsWrapper>> disbursedLoans = withdrawLogsWrappers.stream()
                .collect(groupingBy((t -> t.getCreatedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        disbursedLoans.forEach((key, value) -> responseData.add(Map.of("dateofday", key, "objects", value)));
        responseData.sort(mapComparator());
        Map<String, Integer> noRecords = new HashMap<>();
        noRecords.put("numofrecords", numOfRecords);
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("groupWithdrawalLogs"), responseData);
        response.setMetadata(noRecords);
        return response;
    }

    Function<TransactionsLog, TransactionLogsWrapper> mapTransactionLogToTransactionWrappper() {
        return (transaction) -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(transaction.getContributions().getMemberGroupId());
            if (groupWrapper == null) return null;
            return TransactionLogsWrapper.builder()
                    .id(transaction.getId())
                    .updatedBalance(transaction.getNewbalance())
                    .initialBalance(transaction.getOldbalance())
                    .transactionAmount(transaction.getTransamount())
                    .creditAccountId(transaction.getCreditaccounts().getId())
                    .contributionNarration(transaction.getContributionNarration())
                    .debitPhonenUmber(transaction.getDebitphonenumber())
                    .capturedBy(transaction.getCapturedby())
                    .contributionId(transaction.getContributions().getId())
                    .contributionsName(transaction.getContributions().getName())
                    .groupId(groupWrapper.getId())
                    .groupName(groupWrapper.getName())
                    .createdOn(transaction.getCreatedOn())
                    .build();
        };
    }

    private UniversalResponse getTransactionsLogsByGroup(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<TransactionsLog> transactionsLogList;
        if (group.equalsIgnoreCase("all")) {
            transactionsLogList = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups != null) {
                transactionsLogList = transactionlogsRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable)
                        .stream()
                        .filter(log -> groupFilterByGroupNameParamId.test(groups.getId(), group))
                        .collect(Collectors.toList());
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<TransactionLogsWrapper> transactionLogsWrapperList = transactionsLogList
                .stream()
                .map(mapTransactionLogToTransactionWrappper())
                .collect(Collectors.toList());

        Map<String, List<TransactionLogsWrapper>> transactionLogs = transactionLogsWrapperList.stream()
                .collect(groupingBy((t -> t.getCreatedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        transactionLogs.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Object> responseMssg = new LinkedHashMap<>() {{
            put("data", responseData);
        }};
        return new UniversalResponse("success", getResponseMessage("transactionLogsByGroup"), responseMssg);
    }


    private UniversalResponse getApprovedLoanApplications(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<LoanApplications> loanApplicationsList;
        if (group.equalsIgnoreCase("all")) {
            loanApplicationsList = loanapplicationsRepo.findAllByApprovedAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate, pageable).getContent();
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(false, false, group);
            if (groups != null) {
                loanApplicationsList = loanapplicationsRepo.findAllByApprovedAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate, pageable)
                        .getContent()
                        .stream()
                        .filter(loan -> loan.getLoanProducts().getGroupId() == groups.getId())
                        .collect(Collectors.toList());
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<GroupLoansApprovedWrapper> approvedWrappers =
                loanApplicationsList.stream()
                        .map(loan -> {
                            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(false, false, group);
                            MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                            if (groupWrapper == null || memberWrapper == null) return null;
                            return GroupLoansApprovedWrapper.builder()
                                    .loanproductid(loan.getId())
                                    .loanapplicationid(loan.getLoanProducts().getId())
                                    .amount(loan.getAmount())
                                    .loanproductname(loan.getLoanProducts().getProductname())
                                    .appliedon(loan.getCreatedOn())
                                    .membername(String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                                    .memberphonenumber(memberWrapper.getPhonenumber())
                                    .unpaidloans(loan.getUnpaidloans())
                                    .isGuarantor(loan.getLoanProducts().isGuarantor())
                                    .approvedBy(loan.getApprovedby())
                                    .build();
                        }).collect(Collectors.toList());

        Map<String, List<GroupLoansApprovedWrapper>> approvedLoansMap = approvedWrappers.stream()
                .collect(groupingBy((t -> t.getAppliedon()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        approvedLoansMap.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Object> responseMssg = new LinkedHashMap<>() {{
            put("data", responseData);
        }};
        return new UniversalResponse("success", getResponseMessage("approvedGroupLoans"), responseMssg);

    }


    private UniversalResponse getPendingLoanApplications(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<LoanApplications> loanApplicationsList;
        if (group.equalsIgnoreCase("all")) {
            loanApplicationsList = loanapplicationsRepo.findAllByPendingAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate);
        } else {
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(false, false, group);
            if (groupWrapper != null) {
                loanApplicationsList = loanapplicationsRepo.findAllByPendingAndSoftDeleteAndCreatedOnBetweenOrderByCreatedOnDesc(true, false, startDate, endDate)
                        .stream()
                        .filter(loan -> groupFilterByGroupNameParamId.test(loan.getLoanProducts().getGroupId(), group)).collect(Collectors.toList());
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }

        List<GroupLoansPendingApproval> pendingWrapper =
                loanApplicationsList.stream()
                        .map(loan -> {
                            MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
                            if (memberWrapper == null) return null;
                            return GroupLoansPendingApproval.builder()
                                    .loanproductid(loan.getId())
                                    .loanapplicationid(loan.getLoanProducts().getId())
                                    .amount(loan.getAmount())
                                    .loanproductname(loan.getLoanProducts().getProductname())
                                    .appliedon(loan.getCreatedOn())
                                    .membername(String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                                    .memberphonenumber(memberWrapper.getPhonenumber())
                                    .unpaidloans(loan.getUnpaidloans())
                                    .isGuarantor(loan.getLoanProducts().isGuarantor())
                                    .build();
                        }).collect(Collectors.toList());
        Map<String, List<GroupLoansPendingApproval>> loansPendingApproval = pendingWrapper.stream()
                .collect(groupingBy((t -> t.getAppliedon()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        loansPendingApproval.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Object> responseMssg = new LinkedHashMap<>() {{
            put("data", responseData);
        }};
        return new UniversalResponse("success", getResponseMessage("groupsLoanPendingApproval"), responseMssg);

    }


    private UniversalResponse getLoanPenalties(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<LoanPenalty> loansPenaltyList = loanPenaltyRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable);
        if (!group.equals("all")) {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups == null) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            } else {
                loansPenaltyList = loansPenaltyList.stream()
                        .filter(loan -> groupFilterByGroupNameParamId.test(loan.getLoansDisbursed().getGroupId(), group))
                        .collect(Collectors.toList());
            }
        }
        List<LoanPenaltyReportWrapper> loanPenaltyWrapper = loansPenaltyList.stream()
                .map(penalty -> {
                    GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(penalty.getLoansDisbursed().getGroupId());
                    MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(penalty.getMemberId());
                    if (groupWrapper == null || memberWrapper == null) return null;
                    return LoanPenaltyReportWrapper.builder()
                            .loanPenaltyId(penalty.getId())
                            .penaltyAmount(penalty.getPenaltyAmount())
                            .paymentStatus(penalty.getPaymentStatus())
                            .paidAmount(penalty.getPaidAmount())
                            .dueAmount(penalty.getDueAmount())
                            .transactionId(penalty.getTransactionId())
                            .loanDueDate(penalty.getLoanDueDate().toString())
                            .lastPaymentDate(penalty.getLastPaymentDate())
                            .groupName(groupWrapper.getName())
                            .memberName(String.format(" %s %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                            .memberPhoneNumber(memberWrapper.getPhonenumber())
                            .createdOn(penalty.getCreatedOn())
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, List<LoanPenaltyReportWrapper>> penaltyData = loanPenaltyWrapper.stream()
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        List<Map<String, Object>> penaltyResponse = new ArrayList<>();

        penaltyData.forEach((key, value) -> penaltyResponse.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        penaltyResponse.sort(mapComparator());
        return new UniversalResponse("success", getResponseMessage("penaltyReportsByGroup"), penaltyResponse);
    }

    private UniversalResponse getGroupOverdueLoans(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        long loanProductId;
        try {
            loanProductId = Long.parseLong(additional);
        } catch (NumberFormatException ex) {
            return new UniversalResponse("fail", getResponseMessage("loanProductAdditionalParamRequirement"));
        }
        List<LoansDisbursed> loansDisbursedList;
        int recordsCount;
        if (group.equalsIgnoreCase("all")) {
            loansDisbursedList = loanProductId == 0 ? loansdisbursedRepo.findAllOverdue(startDate, endDate, pageable) : loansdisbursedRepo.findAllOverdueByLoanProductId(loanProductId, startDate, endDate, pageable);
            recordsCount = loanProductId == 0 ? loansdisbursedRepo.countAllOverdue(startDate, endDate) : loansdisbursedRepo.countAllOverdueByLoanProductId(loanProductId, startDate, endDate);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups != null) {
                loansDisbursedList = loanProductId == 0 ? loansdisbursedRepo.findAllOverdueByGroup(groups.getId(), startDate, endDate, pageable) : loansdisbursedRepo.findAllOverdueByLoanProductIdAndGroup(loanProductId, groups.getId(), startDate, endDate, pageable);
                recordsCount = loanProductId == 0 ? loansdisbursedRepo.countAllOverdueByGroup(groups.getId(), startDate, endDate) : loansdisbursedRepo.countAllOverdueByLoanProductIdAndGroup(loanProductId, groups.getId(), startDate, endDate);
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                .stream()
                .map(disbursed -> {
                    GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(disbursed.getGroupId());
                    MemberWrapper memberWrapper = chamaKycService.getMonoMemberDetailsById(disbursed.getMemberId());
                    if (groupWrapper == null || memberWrapper == null) return null;
                    return LoansDisbursedWrapper.builder()
                            .accountTypeId(disbursed.getId())
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
                            .isGuarantor(disbursed.getLoanApplications().getLoanProducts().isGuarantor() ? "true" : "false")
                            .recipient(String.format("%s  %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                            .recipientNumber(memberWrapper.getPhonenumber())
                            .approvedBy(disbursed.getLoanApplications().getApprovedby())
                            .build();
                })
                .collect(Collectors.toList());

        Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                .collect(groupingBy((t -> t.getAppliedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        disbursedLoans.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Integer> noRecords = new HashMap<>();
        noRecords.put("numofrecords", recordsCount);
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("groupOverdueLoans"), responseData);
        response.setMetadata(noRecords);
        return response;
    }

    Function<LoansRepayment, LoanRepaymentWrapper> mapLoansRepaymentToLoanRepaymentWrapper() {
        return (loansRepayment) -> {
            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loansRepayment.getMemberId());
            GroupWrapper group = chamaKycService.getMonoGroupById(loansRepayment.getLoansDisbursed().getGroupId());
            if (member == null || group == null) return null;
            return LoanRepaymentWrapper.builder()
                    .id(loansRepayment.getId())
                    .memberId(member.getId())
                    .memberName(String.format("%s  %s", member.getFirstname(), member.getLastname()))
                    .initialLoan(loansRepayment.getOldamount())
                    .balance(loansRepayment.getNewamount())
                    .paidAmount(loansRepayment.getAmount())
                    .receiptNumber(loansRepayment.getReceiptnumber())
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .paymentType(loansRepayment.getPaymentType())
                    .createdDate(loansRepayment.getCreatedOn())
                    .build();
        };
    }

    private UniversalResponse getLoanRepaymentsByGroupAndProductId(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        List<LoansRepayment> loansRepaymentList;
        long productId;
        long recordCount;
        try {
            productId = Long.parseLong(additional);
        } catch (Exception ex) {
            return new UniversalResponse("fail", getResponseMessage("loanIdMustBeANumber"));
        }
        if (group.equalsIgnoreCase("all")) {
            loansRepaymentList = productId == 0 ? loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable) :
                    loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable)
                            .stream()
                            .filter(loansRepayment -> loansRepayment.getLoansDisbursed().getLoanApplications().getLoanProducts().getId() == productId).collect(Collectors.toList());
            recordCount = loansrepaymentRepo.countAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate);
        } else {
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groupWrapper != null) {
                loansRepaymentList = loansrepaymentRepo.findAllByCreatedOnBetweenAndSoftDeleteFalseOrderByCreatedOnDesc(startDate, endDate, pageable)
                        .stream()
                        .filter(loan -> groupFilterByGroupNameParamId.test(loan.getLoansDisbursed().getGroupId(), group))
                        .filter(loan -> productId == 0 || loan.getLoansDisbursed().getLoanApplications().getLoanProducts().getId() == productId)
                        .collect(Collectors.toList());
                recordCount = loansrepaymentRepo.countloanpaymentsbyGroupid(groupWrapper.getId());

            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<LoanRepaymentWrapper> loanRepaymentWrappersList = loansRepaymentList
                .stream()
                .map(mapLoansRepaymentToLoanRepaymentWrapper())
                .collect(Collectors.toList());

        Map<String, List<LoanRepaymentWrapper>> disbursedLoans = loanRepaymentWrappersList.stream()
                .collect(groupingBy((t -> t.getCreatedDate()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        disbursedLoans.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        responseData.sort(mapComparator());
        Map<String, Object> numofrecords = new HashMap<>();
        numofrecords.put("numofrecords", recordCount);
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("loanRepaymentByGroupAndProductId"), responseData);
        response.setMetadata(numofrecords);
        return response;
    }

    private double loanLimit(LoanProducts loanProducts) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        try {
            int limit = loanService.checkLoanLimit(auth.getName(), loanProducts.getContributions().getId());
            if (!loanProducts.isGuarantor()) {
                limit = (loanProducts.getUserSavingValue() * limit) / 100;
            }
            return limit;
        } catch (Exception exception) {
            return 0.0;
        }
    }

    private UniversalResponse getLoanProductsByGroup(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        List<LoanProducts> loanProductsList = new ArrayList<>();
        int recordCount = 0;
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
            GroupWrapper groupWrapper = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
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
                return new UniversalResponse("fail", String.format("Group search with name %s failed", group), new ArrayList<>());
            }
        }
        List<LoanProductsWrapperReport> loanproductWrapperList =
                loanProductsList.stream()
                        .map(product -> {
                            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(product.getGroupId());
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
                                    .isguarantor(product.isGuarantor())
                                    .ispenalty(product.isPenalty())
                                    .ispenaltypercentage(product.getIsPercentagePercentage())
                                    .usersavingvalue(product.getUserSavingValue())
                                    .userLoanLimit(loanLimit(product))
                                    .debitAccountId(product.getDebitAccountId().getId())
                                    .isActive(product.getIsActive())
                                    .penaltyPeriod(product.getPenaltyPeriod())
                                    .createdOn(product.getCreatedOn())
                                    .build();
                        }).collect(Collectors.toList());

        Map<String, List<LoanProductsWrapperReport>> approvedLoansMap = loanproductWrapperList.stream()
                .collect(groupingBy((t -> t.getCreatedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        approvedLoansMap.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        responseData.sort(mapComparator());
        Map<String, Object> numOfRecords = new HashMap<>();
        numOfRecords.put("numofrecords", recordCount);
        UniversalResponse response = new UniversalResponse("success", String.format(getResponseMessage("loanProductsWithAdditional"), additional), responseData);
        response.setMetadata(numOfRecords);
        return response;
    }

    Function<LoanApplications, GroupLoansPendingApproval> mapLoanApplicationsToWrapper() {
        return loan -> {
            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(loan.getMemberId());
            if (member == null) return null;
            return GroupLoansPendingApproval.builder()
                    .loanproductid(loan.getId())
                    .loanapplicationid(loan.getLoanProducts().getId())
                    .amount(loan.getAmount())
                    .loanproductname(loan.getLoanProducts().getProductname())
                    .appliedon(loan.getCreatedOn())
                    .membername(String.format("%s %s", member.getFirstname(), member.getLastname()))
                    .memberphonenumber(member.getPhonenumber())
                    .unpaidloans(loan.getUnpaidloans())
                    .isGuarantor(loan.getLoanProducts().isGuarantor())
                    .build();
        };
    }

    private UniversalResponse getLoansPendingApprovalbyLoanProduct(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        long loanProductId;
        int recordCount = 0;
        try {
            loanProductId = Long.parseLong(additional);
        } catch (NumberFormatException ex) {
            return new UniversalResponse("fail", "Additional  param value must be a loan product id");
        }
        LoanProducts loanProducts = loanproductsRepository.findById(loanProductId).orElse(null);
        if (loanProducts == null)
            return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));
        List<LoanApplications> loansApplicationList;
        if (group.equalsIgnoreCase("all")) {
            loansApplicationList = loanapplicationsRepo.findAllByLoanProductsAndCreatedOnBetweenAndApprovedAndSoftDeleteOrderByCreatedOnDesc(loanProducts, startDate, endDate, false, false, pageable);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups == null) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            } else {
                loansApplicationList = loanapplicationsRepo.findAllByLoanProductsAndCreatedOnBetweenAndApprovedAndSoftDeleteOrderByCreatedOnDesc(loanProducts, startDate, endDate, false, false, pageable)
                        .stream()
                        .filter(application -> groupFilterByGroupNameParamId.test(application.getLoanProducts().getGroupId(), group))
                        .collect(Collectors.toList());
                recordCount = loanapplicationsRepo.countByLoanProductsAndApproved(loanProductId, groups.getId(), startDate, endDate);
            }
        }
        List<GroupLoansPendingApproval> pendingWrapper =
                loansApplicationList.stream()
                        .map(mapLoanApplicationsToWrapper())
                        .collect(Collectors.toList());

        Map<String, List<GroupLoansPendingApproval>> loansPendingApproval = pendingWrapper.stream()
                .collect(groupingBy((t -> t.getAppliedon()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        loansPendingApproval.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        responseData.sort(mapComparator());

        Map<String, Integer> numrecords = new HashMap<>();
        numrecords.put("numofrecords", recordCount);

        UniversalResponse response = new UniversalResponse("success", getResponseMessage("groupsLoanPendingApproval"), responseData);
        response.setMetadata(numrecords);
        return response;
    }


    Function<LoansDisbursed, LoansDisbursedWrapper> mapLoansDisbursedToWrapper() {
        return disbursed -> {
            String groupName = chamaKycService.getMonoGroupNameByGroupId(disbursed.getGroupId());
            MemberWrapper member = chamaKycService.getMonoMemberDetailsById(disbursed.getMemberId());
            if (groupName.isEmpty() || member == null) return null;
            return LoansDisbursedWrapper.builder()
                    .accountTypeId(disbursed.getId())
                    .appliedOn(disbursed.getCreatedOn())
                    .contributionId(disbursed.getLoanApplications().getLoanProducts().getContributions().getId())
                    .contributionName(disbursed.getLoanApplications().getLoanProducts().getContributions().getName())
                    .dueAmount(disbursed.getDueamount())
                    .principal(disbursed.getPrincipal())
                    .dueDate(disbursed.getDuedate())
                    .interest(disbursed.getInterest())
                    .groupId(disbursed.getGroupId())
                    .groupName(groupName)
                    .interest(disbursed.getInterest())
                    .isGuarantor(disbursed.getLoanApplications().getLoanProducts().isGuarantor() ? "true" : "false")
                    .recipient(String.format("%s  %s", member.getFirstname(), member.getLastname()))
                    .recipientNumber(member.getPhonenumber())
                    .approvedBy(disbursed.getLoanApplications().getApprovedby())
                    .build();
        };
    }

    private UniversalResponse getDisbursedLoansPerProduct(Date startDate, Date endDate, String period, String group, String additional, Pageable pageable) {
        long loanProductId;
        try {
            loanProductId = Long.parseLong(additional);
        } catch (NumberFormatException ex) {
            return new UniversalResponse("fail", getResponseMessage("loanProductAdditionalParamRequirement"));
        }
        List<LoansDisbursed> loansDisbursedList;
        int recordsCount;
        if (group.equalsIgnoreCase("all")) {
            loansDisbursedList = loansdisbursedRepo.findAllByLoanProductId(loanProductId, startDate, endDate, pageable);
            recordsCount = loansdisbursedRepo.countLoansDisbursedbyLoanproductAndGroup(loanProductId, startDate, endDate);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups != null) {
                loansDisbursedList = loansdisbursedRepo.findByLoanproductAndGroup(loanProductId, groups.getId(), startDate, endDate, pageable);
                recordsCount = loansdisbursedRepo.countLoansDisbursedbyLoanproductAndGroup(loanProductId, groups.getId(), startDate, endDate);
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                .stream()
                .map(mapLoansDisbursedToWrapper())
                .collect(Collectors.toList());

        Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                .collect(groupingBy((t -> t.getAppliedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        disbursedLoans.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Integer> noRecords = new HashMap<>();
        noRecords.put("numofrecords", recordsCount);
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("disbursedLoanProductsById"), responseData);
        response.setMetadata(noRecords);
        return response;
    }

    private UniversalResponse getLoanDisbursed(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<LoansDisbursed> loansDisbursedList;
        int recordsCount;
        if (group.equalsIgnoreCase("all")) {
            loansDisbursedList = loansdisbursedRepo.findAllByCreatedOnBetweenOrderByCreatedOnDesc(startDate, endDate, pageable);
            recordsCount = loansdisbursedRepo.countAllByCreatedOnBetween(startDate, endDate);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups != null) {
                loansDisbursedList = loansdisbursedRepo.findAllByGroupIdAndCreatedOnBetweenOrderByCreatedOnDesc(groups.getId(), startDate, endDate, pageable);
                recordsCount = loansdisbursedRepo.countAllByGroupIdAndCreatedOnBetween(groups.getId(), startDate, endDate);
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<LoansDisbursedWrapper> loansDisbursedWrapperList = loansDisbursedList
                .stream()
                .map(mapLoansDisbursedToWrapper())
                .sorted()
                .collect(Collectors.toList());

        Map<String, List<LoansDisbursedWrapper>> disbursedLoans = loansDisbursedWrapperList.stream()
                .collect(groupingBy((t -> t.getAppliedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        disbursedLoans.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        responseData.sort(mapComparator());
        Map<String, Integer> noRecords = new HashMap<>();
        noRecords.put("numofrecords", recordsCount);
        UniversalResponse response = new UniversalResponse("success", getResponseMessage("activeLoanProducts"), responseData);
        response.setMetadata(noRecords);
        return response;
    }

    private UniversalResponse getContributionSchedulePayment(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<ContributionSchedulePayment> contributionSchedulePaymentList;
        if (group.equalsIgnoreCase("all")) {
            contributionSchedulePaymentList = contributionSchedulePaymentRepository.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(false, false, group);
            if (groups != null) {
                contributionSchedulePaymentList = contributionSchedulePaymentRepository.findAllByCreatedOnBetweenAndSoftDelete(startDate, endDate, false, pageable)
                        .stream()
                        .filter(cont -> {
                            Contributions contributions = contributionsRepository.findById(cont.getContributionId()).orElse(null);
                            return (contributions != null && contributions.getMemberGroupId() == groups.getId());
                        }).collect(Collectors.toList());
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }

        List<ContributionSchedulePaymentWrapper> contributionSchedulePaymentWrapperList = contributionSchedulePaymentList
                .stream()
                .map(cont -> {
                    Contributions contributions = contributionsRepository.findById(cont.getContributionId()).orElse(new Contributions());
                    String groupName = chamaKycService.getMonoGroupNameByGroupId(contributions.getMemberGroupId());
                    if (groupName.isEmpty()) return null;
                    return ContributionSchedulePaymentWrapper.builder()
                            .schedulePaymentId(cont.getId())
                            .groupName(groupName)
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
                .collect(groupingBy((t -> t.getCreatedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        paymentWrapperMap.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        responseData.sort(mapComparator());
        Map<String, Object> responseMssg = new LinkedHashMap<String, Object>() {{
            put("data", responseData);
        }};
        return new UniversalResponse("success", getResponseMessage("contributionsSchedulePayment"), responseMssg);
    }

    private UniversalResponse getContributionPaymentReport(Date startDate, Date endDate, String period, String group, Pageable pageable) {
        List<ContributionPayment> contributionPaymentList;
        if (group.equalsIgnoreCase("all")) {
            contributionPaymentList = contributionsPaymentRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable);
        } else {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(false, false, group);
            if (groups != null) {
                contributionPaymentList = contributionsPaymentRepository.findAllByCreatedOnBetweenAndSoftDeleteFalse(startDate, endDate, pageable)
                        .stream()
                        .filter(cont -> {
                            Contributions contributions = contributionsRepository.findById(cont.getContributionId()).orElse(null);
                            return (contributions != null && contributions.getMemberGroupId() == groups.getId());
                        }).collect(Collectors.toList());
            } else {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }
        }
        List<ContributionPaymentWrapper> paymentWrapperList = contributionPaymentList.stream()
                .map(cont -> {
                    Contributions contributions = contributionsRepository.findById(cont.getContributionId()).orElse(new Contributions());
                    String groupName = chamaKycService.getMonoMemberGroupNameById(contributions.getMemberGroupId());

                    if (groupName == null) return null;
                    return ContributionPaymentWrapper.builder()
                            .contributionPaymentId(cont.getContributionId())
                            .contributionName(contributions.getName())
                            .groupName(groupName)
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
                .collect(groupingBy((t -> t.getCreatedOn()
                        .toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));
        List<Map<String, Object>> responseData = new ArrayList<>();
        paymentWrapperMap.forEach((key, value) -> responseData.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        responseData.sort(mapComparator());
        Map<String, Object> responseMssg = new LinkedHashMap<>() {{
            put("data", responseData);
        }};
        return new UniversalResponse("success", getResponseMessage("contributionsPayment"), responseMssg);
    }


    @Override
    public UniversalResponse getGroupQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable) {
        List<GroupWrapper> groupsList = chamaKycService.findFluxGroupsByActiveAndSoftDeleteAndCreatedOnBetween(status, false, startDate, endDate, pageable)
                .collect(Collectors.toList())
                .block();

        Map<String, List<GroupReportWrapper>> groupsData = groupsList.stream()
                .map(group -> GroupReportWrapper.builder()
                        .groupId(group.getId())
                        .name(group.getName())
                        .location(group.getLocation())
                        .description(group.getDescription())
                        .isActive(group.isActive())
                        .createdBy(String.format(" %s %s", group.getCreator().getFirstname(), group.getCreator().getLastname()))
                        .creatorPhone(group.getCreator().getPhonenumber())
                        .hasWallet(group.isWalletexists())
                        .groupImage(group.getGroupImageUrl())
                        .purpose(group.getPurpose())
                        .createdOn(group.getCreatedOn())
                        .build())
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));


        List<Map<String, Object>> groupsResponse = new ArrayList<>();

        groupsData.forEach((key, value) -> groupsResponse.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));
        groupsResponse.sort(mapComparator());
        return new UniversalResponse("success", String.format(getResponseMessage("groupsListByStatus"), status ? "active" : "inactive"), groupsResponse);
    }


    @Override
    public UniversalResponse getMemberQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable, String group) {
        List<MemberWrapper> membersList = chamaKycService.findAllByCreatedOnBetweenAndSoftDeleteAndActive(startDate, endDate, false, status)
                .collectList()
                .block();
        if (!group.equals("all")) {
            GroupWrapper groups = chamaKycService.findMonoGroupByActiveAndSoftDeleteAndNameLike(true, false, group);
            if (groups == null) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            } else {
                membersList = membersList.stream()
                        .filter(p -> groupFilterByGroupNameParamId.test(groups.getId(), group))
                        .collect(Collectors.toList());
            }
        }


        Map<String, List<MemberWrapper>> membersData = membersList.stream()
                .collect(groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

        List<Map<String, Object>> membersResponse = new ArrayList<>();

        membersData.forEach((key, value) -> membersResponse.add(new HashMap<>() {{
            put("dateofday", key);
            put("objects", value);
        }}));

        membersResponse.sort(mapComparator());
        return new UniversalResponse("success", String.format(getResponseMessage("memberListByStatus"), status ? "active" : "inactive"), membersResponse);
    }

    @Override
    public UniversalResponse getGroupAccountsByType(long groupId, Long accountTypeId) {
        Optional<AccountType> accountType = accountTypeRepository.findById(accountTypeId);

        if (accountType.isEmpty()) return new UniversalResponse("fail", getResponseMessage("accountTypeNotFound"));

        List<Accounts> accounts = getAccountsbyGroupandType(groupId, accountType.get());
        return new UniversalResponse("success", getResponseMessage("groupAccounts"), accounts);
    }

    @Override
    public UniversalResponse createNewAccountType(String name, String prefix, List<String> requiredFields) {
        if (accountTypeRepository.existsAccountTypeByAccountName(name))
            return new UniversalResponse("fail", getResponseMessage("accountTypeExists"));

        AccountType accountType = new AccountType();
        accountType.setAccountName(name);
        accountType.setAccountPrefix(prefix);
        accountType.setAccountFields(gson.toJson(requiredFields));

        accountTypeRepository.save(accountType);

        return new UniversalResponse("success", getResponseMessage("accountTypeCreated"));
    }

    @Override
    public UniversalResponse getContributionTypes() {
        return new UniversalResponse("success", getResponseMessage("contributionTypes"), getContributiontypes());
    }

    @Override
    public UniversalResponse getScheduleTypes() {
        return new UniversalResponse("success", getResponseMessage("scheduleTypes"), getScheduletypes());
    }

    @Override
    public Mono<UniversalResponse> checkLoanLimit(String phoneNumber, long groupId, Long contributionId, Long productId) {
        log.info("Contribution id {} and username {} and status {}", contributionId, phoneNumber, PaymentEnum.PAYMENT_SUCCESS.name());
        return Mono.fromCallable(() -> {
            Optional<LoanProducts> loanProductsOptional = loanproductsRepository.findById(productId);

            if (loanProductsOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("loanProductNotFound"));

            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);

            if (member == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            GroupMemberWrapper groupMemberWrapper = chamaKycService.memberIsPartOfGroup(groupId, phoneNumber);

            if (groupMemberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("memberIsNotPartOfGroup"));

            List<ContributionPayment> userContributions =
                    contributionsPaymentRepository.findByContributionIdAndPhoneNumberAndPaymentStatus(contributionId, phoneNumber, PaymentEnum.PAYMENT_SUCCESS.name());

            int totalContributions = userContributions.parallelStream().mapToInt(ContributionPayment::getAmount).sum();

            LoanProducts loanProduct = loanProductsOptional.get();
            Double loanLimit = (totalContributions * loanProduct.getUserSavingValue()) / 100.0;
            log.info("loan limit is ---------{}",loanLimit);

            return new UniversalResponse("success", "user loan limit", Map.of("loan_limit", loanLimit.intValue()));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> userWalletBalance() {
        return CustomAuthenticationUtil.getUsername().flatMap(esbService::balanceInquiry);
    }

    @Override
    public Mono<UniversalResponse> groupAccountBalance(Long groupId) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupId);

            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            List<Accounts> groupAccounts = accountsRepository.findByGroupIdAndActive(groupId, true);

            if (groupAccounts.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupAccountNotFound"));

            Accounts account = groupAccounts.get(0);

            Map<String, Object> metadata = Map.of(
                    "groupId", groupWrapper.getId(),
                    "groupName", groupWrapper.getName(),
                    "availableBal", account.getAvailableBal(),
                    "actualBal", account.getAccountbalance(),
                    "balance", account.getAccountbalance() - account.getAvailableBal());

            return new UniversalResponse("success", getResponseMessage("groupAccountTotals"), metadata);

        }).publishOn(Schedulers.boundedElastic());
    }

    @Scheduled(fixedDelay = 300000)
    @SchedulerLock(name = "groupAccountBalanceInquiry", lockAtMostFor = "6m")
    public void groupAccountBalanceInquiry() {
        accountsRepository.findAll().stream().filter(Accounts::isActive)
                .forEach(account -> {
                    long groupId = account.getGroupId();

                    GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupId);

                    if (groupWrapper == null || !groupWrapper.isActive())
                        return;

                    //includes normal contributions and penalties
                    Integer contributionsAmount = contributionsPaymentRepository.calculateTotalContributionsForGroup(groupId).orElse(0);

                    List<LoansDisbursed> loansDisbursed = loansdisbursedRepo.findAllByGroupIdOrderByCreatedOnDesc(groupId);

                    double loansDisbursedSum = loansDisbursed.stream().mapToDouble(LoansDisbursed::getDueamount).sum();

                    double loanRepaymentsTotal = loansDisbursed.stream()
                            .map(loansrepaymentRepo::findByLoansDisbursedOrderByCreatedOnDesc)
                            .mapToDouble(list -> list.stream().mapToDouble(LoansRepayment::getAmount).sum())
                            .sum();

                    double groupTotals = contributionsAmount + loanRepaymentsTotal - loansDisbursedSum;
                    Map<String, String> balanceInquiryReq = getBalanceInquiryReq(groupWrapper.getCsbAccount());

                    webClient
                            .post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(gson.toJson(balanceInquiryReq))
                            .retrieve()
                            .bodyToMono(String.class)
                            .subscribe(jsonString -> {
                                JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

                                if (jsonObject.get("48").getAsString().equals("Failed")) return;

                                BalanceInquiry balanceInquiry = gson.fromJson(jsonObject.get("54").getAsJsonObject(), BalanceInquiry.class);

                                account.setAccountbalance(Double.parseDouble(balanceInquiry.getAvailableBal()));
                                account.setAvailableBal(groupTotals);
                                account.setLastModifiedDate(new Date());

                                createAccount(account);
                            });
                });
    }

    @Override
    public Mono<UniversalResponse> addContribution(ContributionDetailsWrapper wrapper) {
        return Mono.fromCallable(() -> {
            Optional<ContributionType> contributionType = contributionTypesRepository.findById(wrapper.getContributiontypeid());
            Optional<ScheduleTypes> scheduleType = scheduleTypesRepository.findById(wrapper.getScheduletypeid());
            //
            Optional<AmountType> amountType = amounttypeRepo.findById(wrapper.getAmounttypeid());
            if (contributionsRepository.countByNameAndMemberGroupId(wrapper.getName(), wrapper.getGroupid()) > 0)
                return new UniversalResponse("fail", getResponseMessage("contributionExists"));

            if (chamaKycService.getMonoGroupById(wrapper.getGroupid()) == null)
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            if (scheduleType.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("scheduleTypeNotFound"));

            if (contributionType.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionTypeNotFound"));

            if (amountType.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("amountTypeNotFound"));

            Contributions contribution = new Contributions();
            contribution.setContributiondetails(gson.toJson(wrapper.getContributiondetails()));
            contribution.setContributionType(contributionType.get());
            contribution.setCreatedBy(wrapper.getCreatedby());
            contribution.setMemberGroupId(wrapper.getGroupid());
            contribution.setName(wrapper.getName());
            contribution.setScheduleType(scheduleType.get());
            contribution.setStartDate(wrapper.getStartdate());
            contribution.setAmountType(amountType.get());
            contribution.setActive(true);

            createContribution(contribution);
            return new UniversalResponse("success", getResponseMessage("contributionAddedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    /**
     * Initiates a contribution payment.
     *
     * @param dto           that contains group id, contribution id and amount to contribute
     * @param walletAccount the member's wallet account
     * @return a success or fail message
     */
    @Override
    public Mono<UniversalResponse> makeContribution(ContributionPaymentDto dto, String walletAccount) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(dto.getGroupId());
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(walletAccount);

            if (dto.getAmount() < 1)
                return new UniversalResponse("fail", getResponseMessage("amountNotValid"));

            if (member == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            if (dto.getCoreAccount().length() < 14)
                dto.setCoreAccount("");

            UniversalResponse coreAccountValidationRes = validateCoreAccount(dto, member);
            if (coreAccountValidationRes != null) return coreAccountValidationRes;

            checkBalance(dto, member);

            ContributionSchedulePayment contributionSchedulePayment =
                    contributionSchedulePaymentRepository.findByContributionScheduledId(dto.getSchedulePaymentId());

            if (contributionSchedulePayment == null) {
                return new UniversalResponse("fail", getResponseMessage("contributionSchedulePaymentFound"));
            }

            Optional<Contributions> contributions =
                    contributionsRepository.findByIdAndMemberGroupId(contributionSchedulePayment.getContributionId(), dto.getGroupId());

            if (contributions.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));
            }

            if (groupWrapper == null) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));
            }

            if (!groupWrapper.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsDeactivated"));

            String transactionId = TransactionIdGenerator.generateTransactionId("CNT");
            Map<String, String> esbRequest = constructBody(
                    groupWrapper.getCsbAccount(),
                    dto.getCoreAccount().isBlank() ? walletAccount : dto.getCoreAccount(),
                    dto.getAmount(),
                    contributionSchedulePayment.getId(),
                    Optional.empty(),
                    transactionId,
                    dto.getCoreAccount().isBlank() ? "MC" : "MCC"
            );
            String body = gson.toJson(esbRequest);

            Contributions contrib = contributions.get();
            ContributionPayment contributionPayment = ContributionPayment.builder()
                    .contributionId(contrib.getId())
                    .groupAccountId(contrib.getMemberGroupId())
                    .schedulePaymentId(contributionSchedulePayment.getContributionScheduledId())
                    .amount(dto.getAmount())
                    .transactionId(transactionId)
                    .isCombinedPayment(false)
                    .paymentStatus(PaymentEnum.PAYMENT_PENDING.name())
                    .isPenalty(false)
                    .phoneNumber(walletAccount)
                    .build();
            // Do a funds transfer from Member wallet to Group core account.
            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .publishOn(Schedulers.boundedElastic())
                    .map(res -> {
                        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);

//                        if (jsonObject.get("48").getAsString().equalsIgnoreCase("fail")) {
//                            log.info("Contribution payment failure reason... {}", jsonObject.get("54").getAsString());
//                            return new UniversalResponse("fail", getResponseMessage("contributionCannotBeMade"));
//                        }
                        if (!jsonObject.get("48").getAsString().equalsIgnoreCase("Successful")) {
                            log.info("Contribution payment failure reason... {}", jsonObject.get("54").getAsString());
                            return new UniversalResponse("fail", getResponseMessage("contributionCannotBeMade"));
                        }

                        log.info("Esb response {}",jsonObject);

                        ContributionPayment savedContributionPayment = contributionsPaymentRepository.save(contributionPayment);

                        List<Accounts> groupAccounts = accountsRepository.findByGroupIdAndActive(groupWrapper.getId(), true);

                        saveTransactionLog(savedContributionPayment, member, contrib, groupAccounts.get(0), transactionId);

                        return new UniversalResponse("success", getResponseMessage("requestReceived"));
                    })
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("serviceNotAvailable")))
                    .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    private UniversalResponse validateCoreAccount(ContributionPaymentDto dto, MemberWrapper member) {
        if (!dto.getCoreAccount().isBlank() && member.getLinkedAccounts() == null)
            return new UniversalResponse("fail", getResponseMessage("memeberHasNoLinkedAccounts"));

        if (!dto.getCoreAccount().isBlank() && member.getLinkedAccounts() != null && Arrays.stream(member.getLinkedAccounts().split(",")).noneMatch(s -> s.equals(dto.getCoreAccount())))
            return new UniversalResponse("fail", getResponseMessage("coreAccountDoesNotBelongToMember"));
        return null;
    }

    private void checkBalance(ContributionPaymentDto dto, MemberWrapper member) {
        String accountToCheckBalance = dto.getCoreAccount().isBlank() ? member.getEsbwalletaccount() : dto.getCoreAccount();
        Optional<UniversalResponse> optionalBalanceInquiryRes = esbService.balanceInquiry(accountToCheckBalance).blockOptional();
        optionalBalanceInquiryRes.ifPresent(balanceInquiryRes -> {
            if (balanceInquiryRes.getStatus().equals("fail"))
                throw new IllegalArgumentException(getResponseMessage("balanceInquiryFailed"));

            BalanceInquiry balanceInquiry = (BalanceInquiry) balanceInquiryRes.getData();

            log.info("available balance is ----{}",balanceInquiry.getAvailableBal());
            log.info("amount  is ----{}",dto.getAmount());
            if (Double.parseDouble(balanceInquiry.getAvailableBal()) <= dto.getAmount())
                throw new IllegalArgumentException(getResponseMessage("insufficientBalance"));
        });
    }

    @Override
    public Mono<UniversalResponse> payForContributionPenalty(ContributionPaymentDto dto, String username) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(username);

            if (member == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Penalty penalty = penaltyRepository.findFirstByIdAndSchedulePaymentId(dto.getPenaltyId(), dto.getSchedulePaymentId());

            if (penalty == null)
                return new UniversalResponse("fail", getResponseMessage("penaltyNotFound"));

            if (dto.getAmount() < 1)
                return new UniversalResponse("fail", getResponseMessage("amountNotValid"));

            if (dto.getCoreAccount().length() < 14)
                dto.setCoreAccount("");

            UniversalResponse coreAccountValidationRes = validateCoreAccount(dto, member);
            if (coreAccountValidationRes != null) return coreAccountValidationRes;

            checkBalance(dto, member);

            Optional<Contributions> contributions = contributionsRepository.findById(penalty.getContributionId());

            if (contributions.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));

            Contributions contrib = contributions.get();
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contrib.getMemberGroupId());

            ContributionSchedulePayment csp = contributionSchedulePaymentRepository.findByContributionScheduledId(penalty.getSchedulePaymentId());

            if (csp == null)
                return new UniversalResponse("fail", getResponseMessage("contributionSchedulePaymentFound"));

            String transactionId = TransactionIdGenerator.generateTransactionId("CNTP");
            Map<String, String> esbRequest = constructBody(
                    groupWrapper.getCsbAccount(),
                    dto.getCoreAccount().isBlank() ? username : dto.getCoreAccount(),
                    dto.getAmount(),
                    csp.getId(),
                    Optional.empty(),
                    transactionId,
                    dto.getCoreAccount().isBlank() ? "MC" : "MCC"
            );
            String body = gson.toJson(esbRequest);
            ContributionPayment contributionPayment = ContributionPayment.builder()
                    .contributionId(contrib.getId())
                    .groupAccountId(contrib.getMemberGroupId())
                    .schedulePaymentId(penalty.getSchedulePaymentId())
                    .amount(dto.getAmount())
                    .transactionId(transactionId)
                    .isCombinedPayment(false)
                    .paymentStatus(PaymentEnum.PAYMENT_PENDING.name())
                    .isPenalty(true)
                    .penaltyId(penalty.getId())
                    .phoneNumber(penalty.getPaymentPhoneNumber())
                    .build();
            contributionsPaymentRepository.save(contributionPayment);
            // Do a funds transfer from Member wallet/core account to Group core account.
            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(res -> {
                        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);

                        if (jsonObject.get("48").getAsString().equals("fail")) {
                            log.info("Contribution payment failure reason... {}", jsonObject.get("54").getAsString());
                            return new UniversalResponse("fail", getResponseMessage("penaltyPaymentCannotBeMade"));
                        }

                        return new UniversalResponse("success", getResponseMessage("requestReceived"));
                    })
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("serviceNotAvailable")))
                    .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> makeContributionForOtherMember(ContributionPaymentDto dto, String walletAccount) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(dto.getGroupId());
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(walletAccount);

            if (dto.getAmount() < 1)
                return new UniversalResponse("fail", getResponseMessage("amountNotValid"));

            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            if (dto.getCoreAccount().length() < 14)
                dto.setCoreAccount("");

            UniversalResponse coreAccountValidationRes = validateCoreAccount(dto, member);
            if (coreAccountValidationRes != null) return coreAccountValidationRes;

            checkBalance(dto, member);

            ContributionSchedulePayment contributionSchedulePayment =
                    contributionSchedulePaymentRepository.findByContributionScheduledId(dto.getSchedulePaymentId());

            if (contributionSchedulePayment == null)
                return new UniversalResponse("fail", getResponseMessage("contributionSchedulePaymentFound"));

            MemberWrapper beneficiary = chamaKycService.searchMonoMemberByPhoneNumber(dto.getBeneficiary());

            if (beneficiary == null) return new UniversalResponse("faill", getResponseMessage("beneficiaryNotFound"));

            GroupMemberWrapper membership =
                    chamaKycService.getMonoGroupMembershipByGroupIdAndMemberId(groupWrapper.getId(), beneficiary.getId());

            if (membership == null) return new UniversalResponse("fail", getResponseMessage("memberIsNotPartOfGroup"));

            Optional<Contributions> contributions =
                    contributionsRepository.findByIdAndMemberGroupId(contributionSchedulePayment.getContributionId(), dto.getGroupId());

            if (contributions.isEmpty()) {
                return new UniversalResponse("fail", "Contribution not found");
            }

            if (!groupWrapper.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsDeactivated"));

            Contributions contrib = contributions.get();
            if (contrib.getAmountType().getName().equalsIgnoreCase("fixed amount") &&
                    dto.getAmount() > contrib.getContributionAmount()) {
                return new UniversalResponse("fail", "You cannot contribute more than Tsh. " + contrib.getContributionAmount());
            }

            String transactionId = TransactionIdGenerator.generateTransactionId("CNT");
            Map<String, String> esbRequest = constructBody(
                    groupWrapper.getCsbAccount(),
                    dto.getCoreAccount().isBlank() ? walletAccount : dto.getCoreAccount(),
                    dto.getAmount(),
                    contributionSchedulePayment.getId(),
                    Optional.of(dto.getBeneficiary()),
                    transactionId,
                    dto.getCoreAccount().isBlank() ? "CO" : "COC"
            );
            String body = gson.toJson(esbRequest);

            ContributionPayment contributionPayment = ContributionPayment.builder()
                    .contributionId(contrib.getId())
                    .groupAccountId(contrib.getMemberGroupId())
                    .schedulePaymentId(contributionSchedulePayment.getContributionScheduledId())
                    .amount(dto.getAmount())
                    .transactionId(transactionId)
                    .isCombinedPayment(false)
                    .paymentStatus(PaymentEnum.PAYMENT_PENDING.name())
                    .isPenalty(false)
                    .phoneNumber(beneficiary.getPhonenumber())
                    .build();
            // Do a funds transfer from Member wallet to Group core account.
            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .publishOn(Schedulers.boundedElastic())
                    .map(res -> {
                        JsonObject jsonObject = gson.fromJson(res, JsonObject.class);

                        if (jsonObject.get("48").getAsString().equals("fail")) {
                            log.info("Contribution payment failure reason... {}", jsonObject.get("54").getAsString());
                            return new UniversalResponse("fail", getResponseMessage("contributionCannotBeMade"));
                        }

                        ContributionPayment savedTransactionLog = contributionsPaymentRepository.save(contributionPayment);
                        List<Accounts> groupAccounts = accountsRepository.findByGroupIdAndActive(groupWrapper.getId(), true);

                        saveTransactionLog(savedTransactionLog, beneficiary, contrib, groupAccounts.get(0), transactionId);

                        return new UniversalResponse("success", getResponseMessage("requestReceived"));
                    })
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("serviceNotAvailable")))
                    .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    /**
     * Record a user's withdrawal request and await for approvals from the
     * responsible parties.
     *
     * @param requestWithdrawalWrapper that contains the withdrawal info
     * @param createdBy                the member requesting the withdrawal
     * @return as success or failure message
     */
    @Override
    public Mono<UniversalResponse> recordWithdrawal(RequestwithdrawalWrapper requestWithdrawalWrapper, String createdBy) {
        return Mono.fromCallable(() -> {
            Optional<Accounts> accountOptional = accountsRepository.findById(requestWithdrawalWrapper.getDebitaccountid());
            Optional<Contributions> contributionOptional = contributionsRepository.findById(requestWithdrawalWrapper.getContributionid());
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(requestWithdrawalWrapper.getCreditaccount());
            if (accountOptional.isEmpty()) return new UniversalResponse("fail", getResponseMessage("accountNotFound"));
            if (contributionOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));
            if (member == null) return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
            if (requestWithdrawalWrapper.getCoreAccount() != null && requestWithdrawalWrapper.getCoreAccount().length() <= 12)
                requestWithdrawalWrapper.setCoreAccount("");
            if (requestWithdrawalWrapper.getCoreAccount() != null && !requestWithdrawalWrapper.getCoreAccount().isBlank()
                    && member.getLinkedAccounts() != null &&
                    Arrays.stream(member.getLinkedAccounts().split(",")).noneMatch(s -> s.equals(requestWithdrawalWrapper.getCoreAccount())))
                return new UniversalResponse("fail", getResponseMessage("coreAccountDoesNotBelongToMember"));

            Contributions contribution = contributionOptional.get();

            WithdrawalsPendingApproval withdrawalsPendingApproval = new WithdrawalsPendingApproval();
            withdrawalsPendingApproval.setAmount(requestWithdrawalWrapper.getAmount());
            withdrawalsPendingApproval.setPending(true);
            withdrawalsPendingApproval.setCoreAccount(requestWithdrawalWrapper.getCoreAccount());
            withdrawalsPendingApproval.setPhonenumber(requestWithdrawalWrapper.getCreditaccount());
            withdrawalsPendingApproval.setGroupId(contribution.getMemberGroupId());
            withdrawalsPendingApproval.setApprovedby(new JsonObject().toString());
            withdrawalsPendingApproval.setApprovalCount(0);
            withdrawalsPendingApproval.setAccount(accountOptional.get());
            withdrawalsPendingApproval.setContribution(contribution);
            withdrawalsPendingApproval.setStatus(getResponseMessage("withdrawalAwaitingApprovalFrom"));
            withdrawalsPendingApproval.setWithdrawal_narration(String.format(getResponseMessage("withdrawalMessage"),
                    member.getFirstname().concat(" ").concat(member.getLastname()), requestWithdrawalWrapper.getAmount(), contribution.getName()));
            withdrawalsPendingApproval.setWithdrawalreason(requestWithdrawalWrapper.getWithdrawalreason());

            withdrawalspendingapprovalRepo.save(withdrawalsPendingApproval);

            sendWithdrawalRequestMessageToOfficials(member, contribution.getMemberGroupId(), requestWithdrawalWrapper.getAmount());
            return new UniversalResponse("success", getResponseMessage("withdrawalAwaitingApproval"));
        }).publishOn(Schedulers.boundedElastic());
    }

    /**
     * Sends notification message to officials for a member withdrawal request.
     *
     * @param member  the member requesting a withdrawal
     * @param groupId the group id
     * @param amount  the amount to withdraw
     */
    @Async
    public void sendWithdrawalRequestMessageToOfficials(MemberWrapper member, long groupId, double amount) {
        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupId);
        if (groupWrapper == null) {
            log.info("Group not found when sending SMS to group officials on withdraw request....");
            return;
        }
        String memberName = String.format("%s %s", member.getFirstname(), member.getLastname());
        chamaKycService.getFluxGroupMembers()
                .filter(gm -> gm.getTitle().equalsIgnoreCase("chairperson") || gm.getTitle().equalsIgnoreCase("secretary") || gm.getTitle().equalsIgnoreCase("treasurer"))
                .map(gm -> chamaKycService.getMemberDetailsById(gm.getMemberId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .subscribe(memberWrapper -> notificationService.sendMemberWithdrawRequestText(memberName, amount, groupWrapper.getName(), memberWrapper.getPhonenumber(), memberWrapper.getLanguage()));
    }

    /**
     * Handles callbacks from the DCB ESB.
     * Will be used to update the contribution payment and loan disbursement.
     *
     * @return nothing
     */
    @Bean
    @Override
    public Consumer<String> fundsTransferCallback() {
        return body -> {
            Mono.fromRunnable(() -> {
                log.info("Funds transfer response... {}", body);
                JsonObject jsonObject = new Gson().fromJson(body, JsonObject.class);
                String account = jsonObject.get("2").getAsString();
                String transactionId = jsonObject.get("37").getAsString();

                Optional<EsbRequestLog> esbRequestLog = esbLoggingService.findByTransactionId(transactionId);

                esbRequestLog.ifPresentOrElse(esbLog -> {
                    if (esbLog.isCallbackReceived()) {
                        log.info("Callback with trx id {} is already handled...", transactionId);
                        return;
                    }

                    MemberWrapper member;
                    if (account.length() > 12) { //means it is a core account
                        member = chamaKycService.searchMonoMemberByCoreAccount(account);
                    } else { //means it is a phone number
                        member = chamaKycService.searchMonoMemberByPhoneNumber(account);
                    }

                    if (member == null) {
                        log.info("Member not found... {}", jsonObject.get("102").getAsString());
                        return;
                    }
                    switch (jsonObject.get("25").getAsString()) {
                        case "MC":// member contribution with wallet
                        case "MCC": // member contribution with core account
                            memberContributionResponse(jsonObject, member);
                            break;
                        case "LD": // loan disbursal
                            loanDisbursementResponse(jsonObject, member);
                            break;
                        case "LR": // loan repayment
                            loanRepaymentResponse(jsonObject, member);
                            break;
                        case "MW": // member withdrawal
                            memberWithdrawalResponse(jsonObject, member);
                            break;
                        case "CO": // contribute for other
                            contributeForOtherResponse(jsonObject);
                            break;
                        default:
                            break;
                    }
                    // Update Callback Received Status
                    esbLoggingService.updateCallbackReceived(esbLog);
                    log.info("Callback processed successfully....");
                }, () -> log.info("Transaction log not found... {}", transactionId));
            }).subscribeOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).subscribe(res -> log.info("Done executing FT callback..."));
        };
    }

    private void contributeForOtherResponse(JsonObject jsonObject) {
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(jsonObject.get("102").getAsString());
        MemberWrapper beneficiary = chamaKycService.searchMonoMemberByPhoneNumber(jsonObject.get("66").getAsString());

        if (memberWrapper == null || beneficiary == null) {
            log.info("One of the members is not present!");
            return;
        }

        Optional<ContributionPayment> contributionPayment =
                contributionsPaymentRepository.findContributionByTransactionId(jsonObject.get("37").getAsString());

        if (contributionPayment.isEmpty()) {
            log.info("Contribution payment with id not found {}", jsonObject);
            return;
        }

        if (jsonObject.get("48").getAsString().equalsIgnoreCase("fail")) {
            // Send SMS to user to let them know the contribution was not successful
            // This is for the member performing payment.
            notificationService.sendContributionFailureMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());

            ContributionPayment cp = contributionPayment.get();
            cp.setPaymentStatus(PaymentEnum.PAYMENT_FAILED.name());
            ContributionPayment savedContributionPayment = contributionsPaymentRepository.save(cp);
            Optional<Contributions> optionalContribution = contributionsRepository.findById(savedContributionPayment.getContributionId());
            List<Accounts> groupAccount = accountsRepository.findByGroupIdAndActive(savedContributionPayment.getGroupAccountId(), true);
            // save transaction log
            optionalContribution.ifPresent(contrib -> saveTransactionLog(savedContributionPayment, memberWrapper, contrib, groupAccount.get(0), jsonObject.get("37").getAsString()));
            return;
        }

        int amount = jsonObject.get("4").getAsInt();

        contributionSchedulePaymentRepository.findById(jsonObject.get("61").getAsLong()).ifPresentOrElse(csp -> {
            Optional<Contributions> contribution = contributionsRepository.findById(csp.getContributionId());

            contribution.ifPresentOrElse(contrib -> {
                // check if there is an outstanding contribution payment
                Optional<OutstandingContributionPayment> outstandingContributionPayment =
                        outstandingContributionPaymentRepository.findByContributionIdAndMemberId(contrib.getId(), beneficiary.getId());

                if (outstandingContributionPayment.isEmpty() || outstandingContributionPayment.get().getDueAmount() == 0) {
                    updateContributionPayment(jsonObject, beneficiary, amount, csp, contrib, contributionPayment.get());
                    return;
                }

                // subtract remainder amount and complete the contribution
                OutstandingContributionPayment ocp = outstandingContributionPayment.get();

                GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contrib.getMemberGroupId());

                if (groupWrapper == null) {
                    log.info("Contribution not found... {}", jsonObject);
                    return;
                }

                handleOutstandingContributionPayment(jsonObject, beneficiary, csp, contrib, ocp, groupWrapper, contributionPayment.get());
            }, () -> log.info("Group not found... on contribution payment"));
        }, () -> log.info("Could not find the contribution schedule payment... {}", jsonObject));
    }

    private void memberContributionResponse(JsonObject jsonObject, MemberWrapper memberWrapper) {
        String transactionId = jsonObject.get("37").getAsString();

        Optional<ContributionPayment> contributionPayment =
                contributionsPaymentRepository.findContributionByTransactionId(transactionId);

        if (contributionPayment.isEmpty()) {
            log.info("Could not find contribution payment with id {}", jsonObject);
            return;
        }

        Optional<TransactionsLog> optionalTransactionLog = transactionlogsRepo.findFirstByUniqueTransactionId(transactionId);

        if (optionalTransactionLog.isEmpty()) {
            log.info("Could not find transaction log with id {}", jsonObject);
            return;
        }

        ContributionPayment cp = contributionPayment.get();
        TransactionsLog transactionLog = optionalTransactionLog.get();
        if (jsonObject.get("48").getAsString().equalsIgnoreCase("Failed")) {
            // Send SMS to user to let them know the contribution was not successful
            log.info("Contribution failed...");
            if (cp.getIsPenalty())
                notificationService.sendPenaltyFailureMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());
            else
                notificationService.sendContributionFailureMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());

            cp.setPaymentStatus(PaymentEnum.PAYMENT_FAILED.name());
            cp.setLastModifiedDate(new Date());
            cp.setLastModifiedBy(GeneralEnums.ESB_CALLBACK.name());
            transactionLog.setStatus(PaymentEnum.PAYMENT_FAILED.name());
            transactionLog.setLastModifiedDate(new Date());
            transactionLog.setLastModifiedBy(GeneralEnums.ESB_CALLBACK.name());

            contributionsPaymentRepository.save(cp);
            transactionlogsRepo.save(transactionLog);
        }

        int amount = jsonObject.get("4").getAsInt();

        contributionSchedulePaymentRepository.findById(jsonObject.get("61").getAsLong()).ifPresentOrElse(csp -> {
            Optional<Contributions> contribution = contributionsRepository.findById(csp.getContributionId());

            contribution.ifPresentOrElse(contrib -> {
                // check if there is an outstanding contribution payment
                Optional<OutstandingContributionPayment> outstandingContributionPayment =
                        outstandingContributionPaymentRepository.findByContributionIdAndMemberId(contrib.getId(), memberWrapper.getId());

                if (outstandingContributionPayment.isEmpty() || outstandingContributionPayment.get().getDueAmount() == 0) {
                    updateContributionPayment(jsonObject, memberWrapper, amount, csp, contrib, cp);
                    return;
                }

                // subtract remainder amount and complete the contribution
                OutstandingContributionPayment ocp = outstandingContributionPayment.get();

                GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contrib.getMemberGroupId());

                if (groupWrapper == null) {
                    log.info("Group not found... on contribution payment");
                    return;
                }

                handleOutstandingContributionPayment(jsonObject, memberWrapper, csp, contrib, ocp, groupWrapper, cp);
            }, () -> log.info("Could not find the contribution schedule payment... {}", jsonObject));
        }, () -> log.info("Contribution not found... {}", jsonObject));
    }

    private void handleOutstandingContributionPayment(JsonObject jsonObject, MemberWrapper memberWrapper,
                                                      ContributionSchedulePayment csp, Contributions contrib,
                                                      OutstandingContributionPayment ocp,
                                                      GroupWrapper group, ContributionPayment contributionPayment) {
        int contributedAmount = jsonObject.get("4").getAsInt();
        int remainder = 0;
        int dueAmount = ocp.getDueAmount();

        if (contributedAmount <= ocp.getDueAmount()) {
            // debit all the money to the outstanding payment
            remainder = ocp.getDueAmount() - contributedAmount;

            int paidAmount = ocp.getPaidAmount() + contributedAmount;

            ocp.setDueAmount(remainder);
            ocp.setPaidAmount(paidAmount);
            ocp.setLastModifiedDate(new Date());
            contributionPayment.setPaymentStatus(PaymentEnum.SENT_TO_OUTSTANDING_CONTRIBUTION.name());

            outstandingContributionPaymentRepository.save(ocp);
            contributionsPaymentRepository.save(contributionPayment);

            notificationService.sendOutstandingPaymentConfirmation(
                    memberWrapper.getPhonenumber(),
                    memberWrapper.getFirstname(),
                    dueAmount, group.getName(), remainder, csp.getContributionScheduledId(),
                    memberWrapper.getLanguage());

            return;
        }

        remainder = contributedAmount - ocp.getDueAmount();

        ocp.setDueAmount(0);
        ocp.setPaidAmount(ocp.getDueAmount() + ocp.getPaidAmount());
        ocp.setLastModifiedDate(new Date());

        outstandingContributionPaymentRepository.save(ocp);

        notificationService.sendOutstandingPaymentConfirmation(
                memberWrapper.getPhonenumber(),
                memberWrapper.getFirstname(),
                dueAmount, group.getName(), 0, csp.getContributionScheduledId(),
                memberWrapper.getLanguage());

        updateContributionPayment(jsonObject, memberWrapper, remainder, csp, contrib, contributionPayment);
    }

    private void updateContributionPayment(JsonObject jsonObject, MemberWrapper memberWrapper, int amount,
                                           ContributionSchedulePayment csp,
                                           Contributions contrib,
                                           ContributionPayment contributionPayment) {
        int amountContributed = jsonObject.get("4").getAsInt();
        String esbTrxCode = jsonObject.get("37").getAsString();

        checkContributionOverPayment(memberWrapper,esbTrxCode, contrib, amountContributed, csp);

        contributionPayment.setPaymentStatus(PaymentEnum.PAYMENT_SUCCESS.name());
        contributionsPaymentRepository.save(contributionPayment);

//        ContributionPayment savedPayment = contributionsPaymentRepository.save(contributionPayment);
//        List<Accounts> groupAccount = accountsRepository.findByGroupIdAndActive(savedPayment.getGroupAccountId(), true);

        //send text to member that has contributed i.e., beneficiary
        if (contributionPayment.getIsPenalty()) {
            notificationService.sendPenaltySuccessMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), amount, memberWrapper.getLanguage());
            Penalty penalty = penaltyRepository.findFirstByIdAndSchedulePaymentId(contributionPayment.getPenaltyId(), contributionPayment.getSchedulePaymentId());
            if (penalty != null) {
                penalty.setPaid(true);
                penaltyRepository.save(penalty);
            }
        } else {
            notificationService.sendContributionSuccessMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), amount, memberWrapper.getLanguage());
        }
        // send SMS to all members in the group
        sendContributionTextToMembers(memberWrapper, contrib.getMemberGroupId(), amount, contributionPayment.getIsPenalty());
    }

    private void checkContributionOverPayment(MemberWrapper memberWrapper,
                                              String esbTrxCode,
                                              Contributions contrib, int amountContributed,
                                              ContributionSchedulePayment csp) {


        Integer totalContributed =
                contributionsPaymentRepository.getTotalMemberContributionsForScheduledPayment(csp.getContributionScheduledId(), memberWrapper.getPhonenumber());
        log.info("Upcoming contribution{}",totalContributed);

        totalContributed += amountContributed;
        log.info("Upcoming contribution{}",contrib.getContributionAmount());
        log.info("amountContributed{}",amountContributed);
        log.info("totalContributed += amountContributed{}",totalContributed);


        if (totalContributed > contrib.getContributionAmount()) {
            log.info("Over contribution detected...");

            createOrUpdateContributionOverPayment(memberWrapper,esbTrxCode, contrib, amountContributed, totalContributed);
        } else {
            log.info("Over contribution not detected...");
        }
    }

    private void createOrUpdateContributionOverPayment(MemberWrapper memberWrapper,
                                                       String esbTrxCode,
                                                       Contributions contrib,
                                                       int amountContributed, Integer totalContributed) {

        Optional<OverpaidContribution> overpaidContributionOptional =
                overpaidContributionRepository.findByMemberIdAndContributionId(memberWrapper.getId(), contrib.getId());
//        if(overpaidContributionOptional.isPresent() && overpaidContributionOptional.get().getLastEsbTransactionCode().equals(esbTrxCode)){
//            log.info("dbEsbTrxCode--------",esbTrxCode);
//        }else
            if (overpaidContributionOptional.isEmpty()) {
            // create a new overpaid contribution
            OverpaidContribution overpaidContribution = new OverpaidContribution();
            overpaidContribution.setContributionId(contrib.getId());
            overpaidContribution.setMemberId(memberWrapper.getId());
            overpaidContribution.setGroupId(contrib.getMemberGroupId());
            overpaidContribution.setLastEsbTransactionCode(esbTrxCode);
            overpaidContribution.setPhoneNumber(memberWrapper.getPhonenumber());
            overpaidContribution.setAmount((double) (totalContributed - contrib.getContributionAmount()));

            overpaidContributionRepository.save(overpaidContribution);
        } else {
            log.info("Updating over contribution... Amount => TZS {}", (totalContributed - contrib.getContributionAmount()));
            OverpaidContribution overpaidContribution = overpaidContributionOptional.get();
            double newAmount = totalContributed - (double) contrib.getContributionAmount();
            log.info("Updating over contribution... Amount => TZS {}", newAmount);
            overpaidContribution.setAmount(newAmount);
            overpaidContribution.setLastModifiedDate(new Date());
            overpaidContribution.setLastEsbTransactionCode(esbTrxCode);

            overpaidContributionRepository.save(overpaidContribution);
        }
    }

    @Async
    public void saveTransactionLog(ContributionPayment savedPayment, MemberWrapper memberWrapper, Contributions contribution, Accounts accounts, String transactionId) {
        TransactionsLog transactionsLog = TransactionsLog.builder()
                .contributionNarration(String.format("Member contribution by %s %s of amount Tsh. %d", memberWrapper.getFirstname(), memberWrapper.getLastname(), savedPayment.getAmount()))
                .contributions(contribution)
                .status(savedPayment.getPaymentStatus())
                .transamount(savedPayment.getAmount())
                .creditaccounts(accounts)
                .debitphonenumber(memberWrapper.getPhonenumber())
                .uniqueTransactionId(transactionId)
                .transactionType(TransactionType.CREDIT.name())
                .build();
        transactionlogsRepo.save(transactionsLog);
    }

    @Async
    public void loanDisbursementResponse(JsonObject jsonObject, MemberWrapper memberWrapper) {
        Optional<LoanApplications> loanApplicationOptional = loanapplicationsRepo.findById(jsonObject.get("61").getAsLong());

        loanApplicationOptional.ifPresentOrElse(application -> {

            notificationService.sendLoanDisbursedMessage(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), application.getAmount(), memberWrapper.getLanguage());

            GroupWrapper group = chamaKycService.getMonoGroupById(application.getLoanProducts().getGroupId());
            if (group == null) {
                log.info("Loaned group member not found... {}", jsonObject);
                return;
            }

            LoanProducts loanProducts = application.getLoanProducts();
            log.info("loan product grace period ----{}",loanProducts.getGracePeriod());
            log.info("loan product paymentPeriodType ----{}",loanProducts.getPaymentperiodtype());
            Calendar cal = Calendar.getInstance();

            Optional<LoansDisbursed> optionalLoansDisbursed = loansdisbursedRepo.findByLoanApplications(application);

            optionalLoansDisbursed.ifPresentOrElse(loanDisbursed -> {
                cal.add(Calendar.DATE, loanProducts.getGracePeriod());
                loanDisbursed.setPaymentStartDate(cal.getTime());

                if (loanProducts.getPaymentperiodtype().contains("week")  || loanProducts.getPaymentperiodtype().contains("daily")) {
                    cal.add(Calendar.WEEK_OF_YEAR, loanProducts.getPaymentperiod());
                } else  {
                    cal.add(Calendar.MONTH, loanProducts.getPaymentperiod());
                }
                log.info("cal.getTime()-------{}",cal.getTime());

                loanDisbursed.setDuedate(cal.getTime());
                log.info("loan Due date is ----------{}",cal.getTime());
                loanDisbursed.setLoanApplications(application);
                loanDisbursed.setGroupId(loanProducts.getGroupId());
                loanDisbursed.setMemberId(application.getMemberId());
                loanDisbursed.setPaymentPeriodType(loanProducts.getPaymentperiodtype());
                loanDisbursed.setDueamount(application.getAmount());
                loanDisbursed.setStatus(PaymentEnum.DISBURSED.name());

                LoansDisbursed savedLoanDisbursement = loansdisbursedRepo.save(loanDisbursed);

                log.info("Loan disbursed::: {}", gson.toJson(savedLoanDisbursement));

                Accounts accounts = application.getLoanProducts().getDebitAccountId();
                loanService.saveWithdrawalLog(application, accounts, memberWrapper);

                sendLoanDisbursementTextToMembers(memberWrapper, group.getId(), application.getAmount());
            }, () -> log.info("Loan disbursed not found::: on loan disbursement callback...."));
        }, () -> log.info("Loan application not found... {}", jsonObject));
    }


    //    @Transactional
    public void loanRepaymentResponse(JsonObject jsonObject, MemberWrapper memberWrapper) {
        Optional<LoansDisbursed> loanDisbursedOptional = loansdisbursedRepo.findById(jsonObject.get("61").getAsLong());

        loanDisbursedOptional.ifPresentOrElse(loanDisbursed -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(loanDisbursed.getGroupId());

            if (groupWrapper == null) {
                log.info("Group could not be found... On loan repayment");
                return;
            }

            Optional<LoansRepayment> loanRepayment = loansrepaymentRepo.findFirstByReceiptnumber(jsonObject.get("37").getAsString());

            loanRepayment.ifPresentOrElse(lr -> {
                if (jsonObject.get("48").getAsString().equalsIgnoreCase("fail")) {
                    // Send SMS to user to let them know the contribution was not successful and the reason why
                    notificationService.sendLoanRepaymentFailureText(memberWrapper.getPhonenumber(),
                            memberWrapper.getFirstname(), groupWrapper.getName(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());
                    lr.setStatus(PaymentEnum.PAYMENT_FAILED.name());
                    LoansRepayment savedLoanRepayment = loansrepaymentRepo.save(lr);
                    saveLoanRepaymentTransactionLog(savedLoanRepayment, memberWrapper, groupWrapper.getId(), jsonObject.get("37").getAsString());
                    return;
                }

                lr.setStatus(PaymentEnum.PAYMENT_SUCCESS.name());
                loanDisbursed.setLastModifiedDate(new Date());
                loanDisbursed.setDueamount(loanDisbursed.getDueamount() - lr.getAmount());
                loanDisbursed.setStatus(loanDisbursed.getDueamount() <= 0 ? PaymentEnum.PAID.name() : PaymentEnum.NOT_FULLY_PAID.name());
                loansrepaymentRepo.save(lr);
                loansdisbursedRepo.save(loanDisbursed);

                String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());

                sendLoanRepaymentSmsToMembers(groupWrapper.getId(), memberWrapper.getPhonenumber(), groupWrapper.getName(), memberName, jsonObject.get("4").getAsInt());
                saveLoanRepaymentTransactionLog(lr, memberWrapper, groupWrapper.getId(), jsonObject.get("37").getAsString());
            }, () -> log.info("Could not find loan repayment with receipt number... {}", jsonObject));
        }, () -> log.info("Loan disbursement not found... {}", jsonObject));
    }

    private void saveLoanRepaymentTransactionLog(LoansRepayment loansRepayment, MemberWrapper memberWrapper, long groupId, String transactionId) {
        List<Accounts> groupAccount = accountsRepository.findByGroupIdAndActive(groupId, true);

        if (groupAccount.isEmpty()) return;

        Optional<Contributions> contributions = contributionsRepository.findByMemberGroupId(groupId);

        if (contributions.isEmpty()) return;
        TransactionsLog transactionsLog = TransactionsLog.builder()
                .contributionNarration(String.format("Loan repayment by %s %s of amount Tsh. %.2f", memberWrapper.getFirstname(), memberWrapper.getLastname(), loansRepayment.getAmount()))
                .contributions(contributions.get())
                .transamount(loansRepayment.getAmount())
                .creditaccounts(groupAccount.get(0))
                .uniqueTransactionId(transactionId)
                .debitphonenumber(memberWrapper.getPhonenumber())
                .build();
        transactionlogsRepo.save(transactionsLog);
    }

    @Async
    public void sendLoanRepaymentSmsToMembers(Long groupId, String memberPhoneNumber, String groupName, String memberName, int amountPaid) {
        Flux<Pair<String, String>> fluxMembersLanguageAndPhonesInGroup = chamaKycService.getFluxMembersLanguageAndPhonesInGroup(groupId);

        fluxMembersLanguageAndPhonesInGroup.toStream()
                .filter(pair -> !Objects.equals(pair.getFirst(), memberPhoneNumber))
                .forEach(pair -> notificationService.sendLoanRepaymentSuccessText(pair.getFirst(), memberName, groupName, amountPaid, pair.getSecond()));
    }

    private void memberWithdrawalResponse(JsonObject jsonObject, MemberWrapper memberWrapper) {
        String withdrawalTransactionId = jsonObject.get("37").getAsString();

        Optional<WithdrawalsPendingApproval> withdrawalsPendingApproval = withdrawalspendingapprovalRepo.findById(jsonObject.get("37").getAsLong());

        withdrawalsPendingApproval.ifPresentOrElse(withdrawal -> {

            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(withdrawal.getGroupId());

            if (groupWrapper == null) {
                log.info("Group not found... On member withdrawal response");
                return;
            }

            Optional<WithdrawalLogs> optionalWithdrawalLog = withdrawallogsRepo.findFirstByUniqueTransactionId(withdrawalTransactionId);

            optionalWithdrawalLog.ifPresentOrElse(withdrawalLog -> {

                if (jsonObject.get("48").getAsString().equalsIgnoreCase("Failed")) {
                    // Send SMS to user to let them know the contribution was not successful and the reason why
                    notificationService.sendContributionWithdrawalFailure(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), groupWrapper.getName(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());

                    withdrawalLog.setTransferToUserStatus(PaymentEnum.PAYMENT_FAILED.name());
                    withdrawalLog.setLastModifiedBy(GeneralEnums.ESB_CALLBACK.name());
                    withdrawalLog.setLastModifiedDate(new Date());

                    withdrawallogsRepo.save(withdrawalLog);
                    return;
                }

                withdrawalLog.setTransferToUserStatus(PaymentEnum.PAYMENT_SUCCESS.name());
                withdrawalLog.setLastModifiedBy(GeneralEnums.ESB_CALLBACK.name());
                withdrawalLog.setLastModifiedDate(new Date());

                withdrawallogsRepo.save(withdrawalLog);

                notificationService.sendContributionWithdrawalSuccess(memberWrapper.getPhonenumber(), memberWrapper.getFirstname(), groupWrapper.getName(), jsonObject.get("4").getAsInt(), memberWrapper.getLanguage());
                String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());

                sendWithdrawalMessageToGroup(memberWrapper.getPhonenumber(), memberName, groupWrapper, withdrawal.getAmount());
            }, () -> log.info("Withdrawal log not found... On member withdrawal response"));
        }, () -> log.info("Withdrawal not found... On member withdrawal response"));
    }

    private void saveWithdrawalLog(WithdrawalsPendingApproval withdrawal, GroupWrapper group, MemberWrapper memberWrapper, String transactionId, String paymentStatus) {
        List<Accounts> groupAccount = accountsRepository.findByGroupIdAndActive(group.getId(), true);

        if (groupAccount.isEmpty()) return;

        Optional<Contributions> contributions = contributionsRepository.findByMemberGroupId(group.getId());

        if (contributions.isEmpty()) return;
        WithdrawalLogs withdrawalLogs = WithdrawalLogs.builder()
                .contribution_narration(String.format("Member contribution withdrawal by %s %s of amount Tsh. %s", memberWrapper.getFirstname(), memberWrapper.getLastname(), withdrawal.getAmount()))
                .transamount(withdrawal.getAmount())
                .memberGroupId(group.getId())
                .debitAccounts(groupAccount.get(0))
                .uniqueTransactionId(transactionId)
                .creditphonenumber(memberWrapper.getPhonenumber())
                .contributions(contributions.get())
                .transferToUserStatus(paymentStatus)
                .build();
        log.info("Saving withdrawal log...");
        withdrawallogsRepo.save(withdrawalLogs);
    }

    @Async
    public void sendWithdrawalMessageToGroup(String phonenumber, String memberName, GroupWrapper group, double amount) {
        log.info("Sending withdrawal text to members...");

        chamaKycService.getFluxMembersLanguageAndPhonesInGroup(group.getId())
                .filter(pair -> !Objects.equals(pair.getFirst(), phonenumber))
                .subscribe(pair -> {
                    log.info("Member phone and language -> {} {}", pair.getFirst(), pair.getSecond());
                    notificationService.sendContributionWithdrawalToGroup(
                            pair.getFirst(), memberName, group.getName(), amount, pair.getSecond());
                });
    }

    @Async
    public void sendContributionTextToMembers(MemberWrapper memberWrapper, Long groupId, Integer amount, Boolean isPenalty) {
        log.info("Sending contribution text to members...");
        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        String groupName = chamaKycService.getMonoGroupNameByGroupId(groupId);

        if (groupName == null) {
            log.info("Group name not found....");
            return;
        }

        chamaKycService.getFluxMembersLanguageAndPhonesInGroup(groupId)
                .filter(pair -> !Objects.equals(pair.getFirst(), memberWrapper.getPhonenumber()))
                .subscribe(pair -> {
                    if (isPenalty)
                        notificationService.sendPenaltySuccessMessageToGroup(pair.getFirst(), memberName, groupName, amount, pair.getSecond());
                    else
                        notificationService.sendContributionSuccessMessageToGroup(pair.getFirst(), memberName, groupName, amount, pair.getSecond());
                });
    }

    /**
     * Send texts to group members to notify them of
     * a loan disbursement.
     *
     * @param memberWrapper that has member data
     * @param groupId       the group that gave the loan
     * @param amount        the amount of the loan
     */
    @Async
    public void sendLoanDisbursementTextToMembers(MemberWrapper memberWrapper, Long groupId, Double amount) {
        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        String groupName = chamaKycService.getMonoGroupNameByGroupId(groupId);

        if (groupName == null) {
            log.info("Group name not found...");
            return;
        }
        chamaKycService.getFluxMembersLanguageAndPhonesInGroup(groupId)
                .toStream()
                .filter(pair -> !Objects.equals(pair.getFirst(), memberWrapper.getPhonenumber()))
                .forEach(pair -> notificationService.sendLoanDisbursementTextToGroup(pair.getFirst(), memberName, groupName, amount, pair.getSecond()));
    }

    /**
     * Creates a group's account from Kafka Event
     *
     * @param accountInfo that contains the id, name and available balance
     */
    @Override
    public void createGroupAccount(String accountInfo) {
        log.info("Group account info... {}", accountInfo);
        JsonObject jsonObject = gson.fromJson(accountInfo, JsonObject.class);

        Optional<AccountType> accountType = accountTypeRepository.findById(2L);

        if (accountType.isEmpty()) {
            log.info("Account type not found!");
            return;
        }

        Accounts accounts = new Accounts();
        accounts.setGroupId(jsonObject.get("id").getAsLong());
        accounts.setName(jsonObject.get("name").getAsString());
        accounts.setAccountbalance(jsonObject.get("availableBalance").getAsDouble());
        accounts.setActive(true);
        accounts.setCreatedOn(new Date());
        accounts.setLastModifiedDate(new Date());
        accounts.setLastModifiedBy("KYC");
        accounts.setAccountType(accountType.get());
        JsonObject accountDetails = new JsonObject();
        accountDetails.addProperty("account_number", jsonObject.get("accountNumber").getAsString());
        accounts.setAccountdetails(accountDetails.toString());

        createAccount(accounts);
    }

    @Override
    public void createGroupContribution(String contributionInfo) {
        log.info("Group Contribution Info... {}", contributionInfo);
        JsonObject jsonObject = gson.fromJson(contributionInfo, JsonObject.class);
        if (contributionsRepository.countByMemberGroupId(jsonObject.get("groupId").getAsLong()) > 0)
            return ;

        Optional<AmountType> amountTypeOptional = amounttypeRepo.findById(jsonObject.get("amountType").getAsLong());
        Optional<ScheduleTypes> scheduleTypeOptional =
                scheduleTypesRepository.findById(jsonObject.get("scheduleType").getAsLong());
        Optional<ContributionType> contributionTypeOptional =
                contributionTypesRepository.findById(jsonObject.get("contributionType").getAsLong());

        amountTypeOptional.ifPresentOrElse(amountType -> scheduleTypeOptional.ifPresentOrElse(scheduleType -> {
            contributionTypeOptional.ifPresentOrElse(contributionType -> {
                Contributions contributions = new Contributions();
                contributions.setMemberGroupId(jsonObject.get("groupId").getAsLong());
                contributions.setName(jsonObject.get("contributionName").getAsString());
                contributions.setCreatedBy(jsonObject.get("createdBy").getAsString());
                contributions.setAmountType(amountType);
                contributions.setPenalty(jsonObject.get("penalty").getAsDouble());
                contributions.setScheduleType(scheduleType);
                contributions.setReminder(2); // set a default of 2 days reminder
                contributions.setDuedate(LocalDate.now());
                contributions.setStartDate(new Date());
                contributions.setIspercentage(false);
                contributions.setContributiondetails(jsonObject.get("contributionDetails").getAsString());
                contributions.setContributionAmount(0L);
                contributions.setContributionType(contributionType);
                contributions.setStartDate(new Date());

                Contributions savedContribution = createContribution(contributions);
                String initialAmount = jsonObject.get("initialAmount").getAsString();
                if (initialAmount != null && !initialAmount.isBlank()) {
                    // Make the first contribution using the system
                    createInitialContribution(savedContribution, initialAmount);
                }
            }, () -> log.info("Contribution type not found... creating contribution"));
        }, () -> log.info("Schedule type is empty... creating contribution")), () -> log.info("Amount type not found... creating contribution"));
    }

    private void createInitialContribution(Contributions savedContribution, String initialAmount) {
        ContributionPayment contributionPayment = ContributionPayment.builder()
                .contributionId(savedContribution.getId())
                .amount(Integer.parseInt(initialAmount))
                .groupAccountId(0L)
                .isCombinedPayment(false)
                .paymentStatus(PaymentEnum.PAYMENT_SUCCESS.name())
                .phoneNumber("071111111111")
                .build();
        contributionPayment.setCreatedBy("SYSTEM");

        contributionsPaymentRepository.save(contributionPayment);
    }

    @Override
    public Mono<UniversalResponse> approveWithdrawalRequest(long requestId, boolean approve, String approvedBy) {
        return Mono.fromCallable(() -> {
            MemberWrapper approver = chamaKycService.searchMonoMemberByPhoneNumber(approvedBy);

            if (approver == null) return new UniversalResponse("fail", getResponseMessage("approverNotFound"));

            Optional<WithdrawalsPendingApproval> withdrawalPendingApproval = withdrawalspendingapprovalRepo.findByIdAndPendingTrue(requestId);

            if (withdrawalPendingApproval.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("withdrawalNotPendingOrNotFound"));

            WithdrawalsPendingApproval withdrawal = withdrawalPendingApproval.get();
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(withdrawal.getPhonenumber());

            if (member == null) return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            if (Objects.equals(approver.getPhonenumber(), member.getPhonenumber()))
                return new UniversalResponse("fail", getResponseMessage("cannotApproveOwnWithdrawal"));

            GroupMemberWrapper groupMembership = chamaKycService.getMonoGroupMembershipByGroupIdAndMemberId(withdrawal.getGroupId(), approver.getId());
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(withdrawal.getGroupId());

            if (groupMembership == null)
                return new UniversalResponse("fail", getResponseMessage("memberIsNotPartOfGroup"));
            if (groupWrapper == null) return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            String memberName = String.format("%s %s", member.getFirstname(), member.getLastname());

            if (!approve) {
                notificationService.sendWithdrawalRequestDeclineText(member.getPhonenumber(), memberName, withdrawal.getAmount(), member.getLanguage());
                withdrawal.setPending(false);
                withdrawalspendingapprovalRepo.save(withdrawal);
                return new UniversalResponse("success", getResponseMessage("successfullyDeclinedWithdrawal"));
            }

            JsonObject approvals = gson.fromJson(withdrawal.getApprovedby(), JsonObject.class);
            if (!approvals.has(groupMembership.getTitle())) {
                approvals.addProperty(groupMembership.getTitle(), approver.getId());
                withdrawal.setApprovalCount(withdrawal.getApprovalCount() + 1);
            }

            withdrawal.setApprovedby(approvals.toString());

            if (withdrawal.getApprovalCount() > 1) {
                Double amount = withdrawal.getAmount();
                String accountToDeposit = withdrawal.getCoreAccount() == null || withdrawal.getCoreAccount().isBlank() ? withdrawal.getPhonenumber() : withdrawal.getCoreAccount();
                withdrawal.setApproved(true);
                withdrawal.setStatus("Approved");
                withdrawal.setPending(false);
                WithdrawalsPendingApproval saveWithdrawalLog = withdrawalspendingapprovalRepo.save(withdrawal);
                withdrawContributions(withdrawalPendingApproval.get(), member, groupWrapper, accountToDeposit, amount.intValue(), withdrawal.getId());
                notificationService.sendWithdrawalRequestApprovedText(member.getPhonenumber(), memberName, withdrawal.getAmount(), member.getLanguage());
                return new UniversalResponse("success", getResponseMessage("approvalSuccessful"));
            }

            String awaitingApproveFrom = groupMembership.getTitle().equals("Chairperson") ? "Treasurer" : "Chairperson";
            String message = String.format(getResponseMessage("awaitingApprovalFrom"), awaitingApproveFrom);
            withdrawal.setStatus(message);
            withdrawalspendingapprovalRepo.save(withdrawal);

            return new UniversalResponse("success", String.format(getResponseMessage("approvalSuccessfulExt"), message));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void withdrawContributions(WithdrawalsPendingApproval withdrawalPendingApproval, MemberWrapper member, GroupWrapper groupWrapper, String accountToDeposit, int amount, Long id) {
        String groupCbsAccount = groupWrapper.getCsbAccount();
        log.info("Withdraw contributions... To account => {} From => {}", accountToDeposit, groupCbsAccount);
        String transactionId = TransactionIdGenerator.generateTransactionId("CWT");

        Map<String, String> esbRequest;
        if (accountToDeposit.length() == 12) // using wallet
            esbRequest = constructBody(groupCbsAccount, accountToDeposit, amount, id, Optional.empty(), transactionId, "MW");
        else // using core accou
            esbRequest = constructBody(groupCbsAccount, accountToDeposit, amount, id, Optional.empty(), transactionId, "MWC");

        String body = gson.toJson(esbRequest);

        saveWithdrawalLog(withdrawalPendingApproval, groupWrapper, member, transactionId, PaymentEnum.PAYMENT_PENDING.name());

        webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> esbLoggingService.logESBRequest(esbRequest))
                .subscribe(res -> log.info("Response for CM transfer... {}", res));
    }

    @Override
    public Mono<UniversalResponse> getUserContributionPayments(String phoneNumber) {
        return Mono.fromCallable(() -> contributionsPaymentRepository.findContributionPaymentByPhoneNumber(phoneNumber))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("userContributionPayments"), res));
    }

    @Override
    public Mono<UniversalResponse> getUserContributionPayments(String phoneNumber, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return Mono.fromCallable(() -> contributionsPaymentRepository.findContributionPaymentByPhoneNumber(phoneNumber, pageable))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> UniversalResponse.builder()
                        .status("Success")
                        .message(getResponseMessage("userContributionPayments"))
                        .data(res.getContent())
                        .metadata(Map.of("currentPage", res.getNumber(), "numOfRecords", res.getNumberOfElements(), "totalPages", res.getTotalPages()))
                        .timestamp(new Date())
                        .build());
    }

    @Override
    public Mono<UniversalResponse> getGroupContributionPayments(Long contributionId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(Math.abs(page), Math.min(size, 10));
        return Mono.fromCallable(() -> contributionsPaymentRepository.findByContributionIdOrderByCreatedOn(contributionId, pageable))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", "Member contributions", res.getContent()));
    }

    @Override
    public Mono<UniversalResponse> getUssdGroupContributionPayments(Long contributionId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(Math.abs(page), Math.min(size, 10));
        return Mono.fromCallable(() -> contributionsPaymentRepository.findByContributionIdOrderByCreatedOn(contributionId, pageable))
                .publishOn(Schedulers.boundedElastic())
                .map(pagedData -> {
                    List<ContributionPayment> contributionPayments = pagedData.getContent();
                    return contributionPayments.parallelStream()
                            .filter(cp -> cp.getPaymentStatus().equals(PaymentEnum.PAYMENT_SUCCESS.name()) && !Objects.isNull(cp.getPhoneNumber()))
                            .map(cp -> {
                                        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                                        MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(cp.getPhoneNumber());
                                        return formatter.format(cp.getCreatedOn()) +
                                                " " +
                                                String.format("%s %s", member.getFirstname(), member.getLastname()) +
                                                "gave " +
                                                "TZS " +
                                                numberFormat.format(cp.getAmount());
                                    }
                            ).collect(Collectors.joining("|"));
                }).map(res -> new UniversalResponse("success", getResponseMessage("groupContributionPayments"), res));
    }

    @Override
    public Mono<UniversalResponse> getUserUpcomingPayments(String phoneNumber, long groupId) {
        return Mono.fromCallable(() -> {
            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);

            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            List<UserUpcomingContributionsWrapper> scheduledPayments = getScheduledPayments(memberWrapper, true, groupId);
            return new UniversalResponse("success", getResponseMessage("userUpcomingContributions"), scheduledPayments);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getUserUpcomingPayments(String phoneNumber) {
        Optional<MemberWrapper> memberWrapperOptional = chamaKycService.searchMemberByPhoneNumber(phoneNumber);

        Flux<GroupWrapper> fluxGroupsMemberBelongs = chamaKycService.getFluxGroupsMemberBelongs(phoneNumber);

        return memberWrapperOptional.map(memberWrapper -> fluxGroupsMemberBelongs
                        .map(GroupWrapper::getId)
                        .doOnNext(gId -> log.info("Group Id: " + gId))
                        .publishOn(Schedulers.boundedElastic())
                        .map(gId -> getScheduledPayments(memberWrapper, true, gId))
                        .flatMap(Flux::fromIterable)
                        .collectList()
                        .map(sp -> UniversalResponse.builder()
                                .status("success")
                                .message("User upcoming payments")
                                .data(sp)
                                .build()))
                .orElseGet(() -> Mono.just(new UniversalResponse("fail", getResponseMessage("memberNotFound"))));
    }

    @Override
    public Mono<UniversalResponse> getAllUserUpcomingPayments(String phoneNumber) {
        return Mono.fromCallable(() -> {
            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(phoneNumber);

            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            List<UpcomingContributionsProjection> userUpcomingContributions = contributionSchedulePaymentRepository.findAllUserUpcomingContributions(memberWrapper.getId())
                    .stream()
                    .filter(upcoming -> upcoming.getRemainder() > 0)
                    .collect(Collectors.toList());

            return new UniversalResponse("success", getResponseMessage("userUpcomingContributions"), userUpcomingContributions);
        }).publishOn(Schedulers.boundedElastic());
    }

    private List<UserUpcomingContributionsWrapper> getScheduledPayments(MemberWrapper memberWrapper, boolean isUpcoming, long groupId) {
        Pageable pageable = PageRequest.of(0, 15);
        List<UserUpcomingContributionsWrapper> userUpcomingContributionsWrappers = new ArrayList<>();

        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(groupId);

        if (groupWrapper == null)
            return Collections.emptyList();

        if (!groupWrapper.isActive())
            return Collections.emptyList();

        List<Contributions> contributions = contributionsRepository.findByMemberGroupId(groupId, pageable);

        contributions.forEach(contribution -> {
            List<ContributionSchedulePayment> contributionSchedulePayments =
                    contributionSchedulePaymentRepository.findUpcomingContributionById(contribution.getId());

            contributionSchedulePayments.forEach(contributionSchedulePayment -> {
                Optional<OutstandingContributionPayment> anyOutstandingContributionPayment = outstandingContributionPaymentRepository.findByContributionIdAndMemberId(contribution.getId(), memberWrapper.getId());

                UserUpcomingContributionsWrapper userUpcomingContributionsWrapper = new UserUpcomingContributionsWrapper();

                List<ContributionPayment> contributionPayment = contributionsPaymentRepository.findPaidScheduledContributions(
                        memberWrapper.getPhonenumber(), contributionSchedulePayment.getContributionScheduledId());

                int totalPayment = contributionPayment.stream().mapToInt(ContributionPayment::getAmount).sum();

                if (totalPayment >= contribution.getContributionAmount()) {
                    log.info("Member has contributed fully...");
                    return;
                }

                if (!isUpcoming) {
                    Penalty penalty = penaltyRepository.findByUserIdAndSchedulePaymentId(memberWrapper.getId(), contributionSchedulePayment.getContributionScheduledId());
                    if (penalty != null) {
                        userUpcomingContributionsWrapper.setHasPenalty(true);
                        userUpcomingContributionsWrapper.setPenaltyAmount((int) penalty.getAmount());
                        userUpcomingContributionsWrapper.setPenaltyId(penalty.getId());
                    }
                }

                userUpcomingContributionsWrapper.setGroupId(groupWrapper.getId());
                userUpcomingContributionsWrapper.setAmount(contribution.getContributionAmount().intValue());
                userUpcomingContributionsWrapper.setRemaining(contribution.getContributionAmount().intValue() - totalPayment);
                //
                userUpcomingContributionsWrapper.setContributionName(contribution.getName());
                userUpcomingContributionsWrapper.setSchedulePaymentId(contributionSchedulePayment.getContributionScheduledId());
                userUpcomingContributionsWrapper.setExpectedPaymentDate(contributionSchedulePayment.getExpectedContributionDate());
                anyOutstandingContributionPayment.ifPresent(outstandingPayment -> userUpcomingContributionsWrapper.setOutstandingAmount(outstandingPayment.getDueAmount()));

                userUpcomingContributionsWrappers.add(userUpcomingContributionsWrapper);
            });
        });

        return userUpcomingContributionsWrappers;
    }

    @Async
    @Override
    public void enableGroupContributions(String groupInfo) {
        JsonObject jsonObject = gson.fromJson(groupInfo, JsonObject.class);

        long groupId = jsonObject.get("groupId").getAsLong();
        String modifier = jsonObject.get("modifier").getAsString();

        List<Contributions> groupContributions = contributionsRepository.findAllByMemberGroupId(groupId);

        List<Contributions> updatedContributions = groupContributions.stream()
                .map(c -> {
                    c.setActive(false);
                    c.setLastModifiedDate(new Date());
                    c.setLastModifiedBy(modifier);
                    return c;
                }).collect(Collectors.toList());

        contributionsRepository.saveAll(updatedContributions);
    }

    @Async
    @Override
    public void disableGroupContributions(String groupInfo) {
        JsonObject jsonObject = gson.fromJson(groupInfo, JsonObject.class);

        long groupId = jsonObject.get("groupId").getAsLong();
        String modifier = jsonObject.get("modifier").getAsString();

        List<Contributions> groupContributions = contributionsRepository.findAllByMemberGroupId(groupId);

        List<Contributions> updatedContributions = groupContributions.stream()
                .map(c -> {
                    c.setActive(false);
                    c.setLastModifiedDate(new Date());
                    c.setLastModifiedBy(modifier);
                    return c;
                }).collect(Collectors.toList());

        contributionsRepository.saveAll(updatedContributions);
    }

    @Override
    public Mono<UniversalResponse> updateContribution(ContributionDetailsWrapper contributionWrapper, String modifier) {
        return Mono.fromCallable(() -> {
            Optional<Contributions> contributionOptional = contributionsRepository.findById(contributionWrapper.getId());

            if (contributionOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));

            Optional<ScheduleTypes> scheduleTypeOptional = scheduleTypesRepository.findById(contributionWrapper.getScheduletypeid());

            if (scheduleTypeOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("scheduleTypeNotFound"));

            Optional<AmountType> amountTypeOptional = amounttypeRepo.findById(contributionWrapper.getAmounttypeid());

            if (amountTypeOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("amountTypeNotFound"));

            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contributionWrapper.getGroupid());

            if (groupWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Optional<ContributionType> contributionTypeOptional = contributionTypesRepository.findById(contributionWrapper.getContributiontypeid());

            if (contributionTypeOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionTypeNotFound"));

            Contributions contribution = contributionOptional.get();
            ContributionType contributionType = contributionTypeOptional.get();

            if (contribution.getMemberGroupId() != groupWrapper.getId())
                return new UniversalResponse("fail", getResponseMessage("contributionNotForGroup"));

            contribution.setContributionType(contributionType);
            contribution.setAmountType(amountTypeOptional.get());
            contribution.setScheduleType(scheduleTypeOptional.get());
            contribution.setStartDate(contributionWrapper.getStartdate() == null ? contribution.getStartDate() : contributionWrapper.getStartdate());
            contribution.setActive(true);
            contribution.setContributionAmount(contributionWrapper.getAmountcontributed());
            LocalDate dueDate = LocalDate.parse(contributionWrapper.getDueDate());
            contribution.setDuedate(dueDate);
            contribution.setLastModifiedDate(new Date());
            contribution.setLastModifiedBy(modifier);
            Contributions savedContribution = contributionsRepository.save(contribution);

            return UniversalResponse.builder()
                    .status("success")
                    .message("Updated contribution successfully")
                    .metadata(savedContribution)
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getUserContributionsPerGroup(String phoneNumber) {
        return Mono.fromCallable(() -> {
                    List<UserGroupContributions> userContributions = contributionsPaymentRepository.findUserContributions(phoneNumber);

                    Map<String, List<UserGroupContributions>> collect = userContributions.stream()
                            .collect(groupingBy(UserGroupContributions::getGroupName));

                    Set<ContributionAnalyticsWrapper> contributionsByUserForGroups = new HashSet<>();
                    collect.forEach((s, userGroupContributions) -> {
                        int sum = userGroupContributions.stream().mapToInt(UserGroupContributions::getAmount).sum();
                        contributionsByUserForGroups.add(new ContributionAnalyticsWrapper(s, sum));
                    });

                    return contributionsByUserForGroups;
                })
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("totalUserContributionsPerGroup"), res));
    }

    @Override
    public Mono<UniversalResponse> getAllMemberPenalties(String username) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(username);

            if (member == null)
                return new UniversalResponse("fail", "Could not find member");

            List<Penalty> userPenalties = penaltyRepository.findAllByUserId(member.getId());
            List<PenaltyWrapper> penaltyWrappers = new ArrayList<>();
            userPenalties.parallelStream()
                    .filter(p -> !p.isPaid())
                    .forEach(penalty -> mapToPenaltyWrapper(penaltyWrappers, penalty));
            return new UniversalResponse("success", getResponseMessage("allMemberPenalties"), penaltyWrappers);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupContributionPenalties(Long groupId, int page, int size) {
        Flux<MemberWrapper> fluxGroupMembers = chamaKycService.getFluxGroupMembers(groupId);
        return findGroupContribution(groupId)
                .zipWith(fluxGroupMembers.collectList())
                .map(tuple -> {
                    Contributions contribution = tuple.getT1();
                    List<MemberWrapper> groupMembers = tuple.getT2();
                    List<PenaltyWrapper> penaltyWrappers = new ArrayList<>();
                    groupMembers.parallelStream()
                            .map(gm -> penaltyRepository.findByUserIdAndContributionId(gm.getId(), contribution.getId()))
                            .flatMap(List::stream)
                            .filter(penalty -> !penalty.isPaid())
                            .forEach(penalty -> mapToPenaltyWrapper(penaltyWrappers, penalty));
                    return new UniversalResponse("success", getResponseMessage("allMemberPenalties"), penaltyWrappers);
                }).publishOn(Schedulers.boundedElastic());
    }

    private void mapToPenaltyWrapper(List<PenaltyWrapper> penaltyWrappers, Penalty penalty) {
        ContributionSchedulePayment contributionSchedulePayment =
                contributionSchedulePaymentRepository.findByContributionScheduledId(penalty.getSchedulePaymentId());
        if (contributionSchedulePayment == null)
            return;

        Contributions contributions = contributionsRepository.findById(contributionSchedulePayment.getContributionId()).get();
        List<ContributionPayment> contributionPaymentList = contributionsPaymentRepository.findPenaltyContributions(contributions.getId(), penalty.getId());
        penalty.setContributionName(contributions.getName());
        penalty.setPaymentStatus(contributionPaymentList.isEmpty() ? "NOT_PAID" : contributionPaymentList.get(0).getPaymentStatus());
        penalty.setExpectedPaymentDate(contributionSchedulePayment.getExpectedContributionDate());
        penalty.setContributionId(contributions.getId());
        penalty.setGroupId(contributions.getMemberGroupId());
        PenaltyWrapper penaltyWrapper = mapPenaltyToWrapper().apply(penalty);
        penaltyWrappers.add(penaltyWrapper);
    }

    private Mono<Contributions> findGroupContribution(Long groupId) {
        return Mono.fromCallable(() -> contributionsRepository.findByMemberGroupId(groupId).orElse(null))
                .publishOn(Schedulers.boundedElastic())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Contribution for group not found")));
    }

    private Function<Penalty, PenaltyWrapper> mapPenaltyToWrapper() {
        return penalty -> {
            Optional<MemberWrapper> memberWrapperOptional = chamaKycService.searchMemberByUserId(penalty.getUserId());
            if (memberWrapperOptional.isEmpty()) return null;
            MemberWrapper memberWrapper = memberWrapperOptional.get();
            return PenaltyWrapper.builder()
                    .id(penalty.getId())
                    .userId(penalty.getUserId())
                    .schedulePaymentId(penalty.getSchedulePaymentId())
                    .contributionName(penalty.getContributionName())
                    .paymentStatus(penalty.getPaymentStatus())
                    .expectedPaymentDate(penalty.getExpectedPaymentDate())
                    .contributionId(penalty.getContributionId())
                    .groupId(penalty.getGroupId())
                    .amount(penalty.getAmount())
                    .memberNames(String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname()))
                    .transactionId(penalty.getTransactionId())
                    .isPaid(penalty.isPaid())
                    .build();
        };
    }

    @Override
    public Mono<UniversalResponse> editContribution(ContributionDetailsWrapper contributionDetailsWrapper, String username) {
        return Mono.fromCallable(() -> {
            GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contributionDetailsWrapper.getGroupid());
            if (groupWrapper == null) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), new ArrayList<>());
            }

            Pageable pageable = PageRequest.of(0, 10);
            List<Contributions> contributionsList = contributionsRepository.findByMemberGroupId(groupWrapper.getId(), pageable);

            if (contributionsList.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("noContributionsAssociatedWithGroup"), new ArrayList<>());
            }

            Contributions contributions = contributionsList.get(0); //weekly
            contributions.setContributionAmount(contributionDetailsWrapper.getAmountcontributed());
            ScheduleTypes scheduleTypes = scheduleTypesRepository.findByName(contributionDetailsWrapper.getScheduletypename());

            if (scheduleTypes == null) {
                return new UniversalResponse("fail", getResponseMessage("frequencyNotCateredFor"), new ArrayList<>());
            }

            contributions.setScheduleType(scheduleTypes);
            contributions.setPenalty(contributionDetailsWrapper.getPenalty() == null ? 0 : contributionDetailsWrapper.getPenalty());
            contributions.setIspercentage(contributionDetailsWrapper.getIsPercentage() != null && contributionDetailsWrapper.getIsPercentage());
            contributions.setDuedate(LocalDate.parse(contributionDetailsWrapper.getDueDate()));
            contributions.setLastModifiedDate(new Date());
            contributions.setLastModifiedBy(username);
            contributions.setActive(true);
            contributions.setContributionAmount(contributionDetailsWrapper.getAmountcontributed() == 0 ? contributions.getContributionAmount() : contributionDetailsWrapper.getAmountcontributed());

            contributions = contributionsRepository.saveAndFlush(contributions);

            sendContributionEditText(contributions, username);
            return new UniversalResponse("success", getResponseMessage("successfulContributionEdit"), contributions);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendContributionEditText(Contributions contributions, String username) {
        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contributions.getMemberGroupId());
        MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(username);

        if (groupWrapper == null) {
            log.info("Group not found on contribution edit sending SMS...");
            return;
        }

        if (memberWrapper == null) {
            log.info("Member not found on contribution edit sending SMS...");
            return;
        }

        String memberName = String.format("%s %s", memberWrapper.getFirstname(), memberWrapper.getLastname());
        DayOfWeek dayOfWeek = contributions.getDuedate().getDayOfWeek();
        String displayName = dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault());
        chamaKycService.getFluxGroupMembers(groupWrapper.getId())
                .subscribe(member -> notificationService.sendContributionEditText(memberName, contributions.getName(),
                        groupWrapper.getName(), contributions.getContributionAmount(), displayName, member.getPhonenumber(), member.getLanguage()));
    }

    @Override
    public Mono<UniversalResponse> getGroupContributions(Long groupId) {
        return Mono.fromCallable(() -> {
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            List<ContributionWrapper> groupContributions = contributionsRepository.findAllByMemberGroupId(groupId)
                    .stream().map(c -> ContributionWrapper.builder()
                    .id(c.getId())
                    .groupId(c.getMemberGroupId())
                    .name(c.getName())
                    .amountType(c.getAmountType().getName())
                    .contributionAmount(c.getContributionAmount())
                    .contributionTypeName(c.getContributionType().getName())
                    .scheduleTypeName(c.getScheduleType().getName())
                    .active(c.isActive())
                    .ispercentage(c.getIspercentage())
                    .memberGroupId(c.getMemberGroupId())
                    .reminder(c.getReminder())
                    .dueDate(c.getDuedate())
                    .penalty(c.getPenalty())
                    .startDate(formatter.format(c.getStartDate()))
                    .build())
//                    .sorted(Comparator.comparing(ContributionWrapper::getStartDate).reversed())
//                    .limit(5)
                    .collect(Collectors.toList());
            return new UniversalResponse("success", getResponseMessage("groupContributions"), groupContributions);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupContribution(Long contributionId) {
        return Mono.fromCallable(() -> {
            Optional<Contributions> contributions = contributionsRepository.findById(contributionId);

            if (contributions.isEmpty())
                return UniversalResponse.builder()
                        .status("fail")
                        .message("Could not find contribution with id")
                        .timestamp(new Date())
                        .build();

            Contributions contribution = contributions.get();
            ContributionWrapper contributionWrapper = ContributionWrapper.builder()
                    .id(contribution.getId())
                    .groupId(contribution.getMemberGroupId())
                    .name(contribution.getName())
                    .amountType(contribution.getAmountType().getName())
                    .contributionAmount(contribution.getContributionAmount())
                    .contributionTypeName(contribution.getContributionType().getName())
                    .scheduleTypeName(contribution.getScheduleType().getName())
                    .active(contribution.isActive())
                    .penalty(contribution.getPenalty())
                    .dueDate(contribution.getDuedate())
                    .ispercentage(contribution.getIspercentage())
                    .memberGroupId(contribution.getMemberGroupId())
                    .reminder(contribution.getReminder())
                    .startDate(contribution.getStartDate().toString())
                    .build();
            return UniversalResponse.builder()
                    .status("success")
                    .message("Contribution details")
                    .data(contributionWrapper)
                    .timestamp(new Date())
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupAccountsMemberBelongsTo(String username) {
        return Mono.fromCallable(() -> {
            Flux<GroupWrapper> fluxGroupsMemberBelongs = chamaKycService.getFluxGroupsMemberBelongs(username);

            List<Map<String, Object>> groupAccountData = new ArrayList<>();

            fluxGroupsMemberBelongs.toIterable().forEach(group -> {
                List<Accounts> groupAccounts = accountsRepository.findByGroupIdAndActive(group.getId(), true);

                Map<String, Object> metadata;
                if (groupAccounts.isEmpty()) {
                    metadata = Map.of(
                            "groupId", group.getId(),
                            "groupName", group.getName(),
                            "accountNumber", group.getCsbAccount(),
                            "accountBal", 0,
                            "availableBal", 0);
                } else {
                    metadata = Map.of(
                            "groupId", group.getId(),
                            "groupName", group.getName(),
                            "accountNumber", group.getCsbAccount(),
                            "accountBal", groupAccounts.get(0).getAccountbalance(),
                            "availableBal", groupAccounts.get(0).getAvailableBal());
                }

                groupAccountData.add(metadata);

            });
            return new UniversalResponse("success", getResponseMessage("userGroupAccountDetails"), groupAccountData);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupTransactions(Long groupId, Integer page, Integer size) {
        return Mono.fromCallable(() -> getTransactionsbyGroup(groupId, PageRequest.of(page, size)))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("groupTransactions"), res));
    }

    @Override
    public Mono<UniversalResponse> getUserTransactions(String username, Integer page, Integer size) {
        return Mono.fromCallable(() -> getTransactionsbyUser(username, PageRequest.of(page, size)))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("userTransactions"), res));
    }

    @Override
    public Mono<UniversalResponse> getUserTransactionsByContribution(String username, Long contributionId, Integer page, Integer size) {
        return Mono.fromCallable(() -> {
            Optional<Contributions> contributionsOptional = contributionsRepository.findById(contributionId);

            if (contributionsOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"));

            List<TransactionLogWrapper> transactions =
                    getTransactionsbyUserandContributions(username, contributionsOptional.get(), PageRequest.of(page, size));

            return new UniversalResponse("success", getResponseMessage("userTransactionsByContribution"), transactions);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getUserTransactionsByGroup(String username, Long groupId, Integer page, Integer size) {
        return Mono.fromCallable(() -> {
            GroupWrapper group = chamaKycService.getMonoGroupById(groupId);

            if (group == null)
                return new UniversalResponse("fail", getResponseMessage("userTransactionsByGroup"));

            List<TransactionLogWrapper> transactions = getTransactionsbyUserandGroupId(username, groupId, PageRequest.of(page, size));

            return new UniversalResponse("success", getResponseMessage("userTransactionsByGroup"), transactions);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getUserSummary(String phone, Long contributionId) {
        return Mono.fromCallable(() -> {
            MemberWrapper member = chamaKycService.searchMonoMemberByPhoneNumber(phone);

            if (member == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            // total contributions
            int totalContributions = contributionsPaymentRepository.findUsersContribution(contributionId, phone)
                    .parallelStream()
                    .filter(cp -> Objects.equals(cp.getPaymentStatus(), PaymentEnum.PAYMENT_SUCCESS.name()))
                    .mapToInt(ContributionPayment::getAmount)
                    .sum();

            // total loans
            Double totalLoansDisbursed = loansdisbursedRepo.findByMemberIdOrderByCreatedOnDesc(member.getId())
                    .parallelStream()
                    .filter(ld -> ld.getDueamount() != 0)
                    .mapToDouble(ld -> ld.getDueamount() + ld.getInterest())
                    .sum();
            // total contributions penalties
            Double totalPenalties = penaltyRepository.findAllByUserId(member.getId())
                    .parallelStream()
                    .filter(p -> !p.isPaid())
                    .mapToDouble(Penalty::getAmount)
                    .sum();
            // total loan penalties
            Double totalLoanPenalties = loanPenaltyRepository.findAllByMemberId(member.getId())
                    .parallelStream()
                    .filter(lp -> Objects.equals(lp.getPaymentStatus(), PaymentEnum.PAYMENT_PENDING.name()))
                    .mapToDouble(LoanPenalty::getPenaltyAmount)
                    .sum();
            Map<String, ? extends Number> memberSummary = Map.of(
                    "totalContributions", totalContributions,
                    "totalLoansDisbursed", totalLoansDisbursed,
                    "totalContributionsPenalties", totalPenalties,
                    "totalLoansPenalties", totalLoanPenalties
            );
            return new UniversalResponse("success", getResponseMessage("memberAccountingSummary"), memberSummary);
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> getGroupAccounts(Long groupId) {
        return Mono.fromCallable(() -> {
                    List<Accounts> accountsList = accountsRepository.findByGroupIdAndActive(groupId, true);

                    return accountsList.parallelStream()
                            .map(a -> AccountDto.builder()
                                    .accountId(a.getId())
                                    .name(a.getName())
                                    .active(a.isActive())
                                    .accountbalance(a.getAccountbalance())
                                    .availableBal(a.getAvailableBal())
                                    .build())
                            .collect(Collectors.toList());
                }).publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("groupAccounts"), res));
    }

    @Override
    public Mono<UniversalResponse> fetchGroupAccountAndContributions(Long groupId) {
        Mono<UniversalResponse> groupContributions = getGroupContributions(groupId);
        Mono<UniversalResponse> groupAccounts = getGroupAccounts(groupId);

        Mono<Tuple2<UniversalResponse, UniversalResponse>> zip = groupContributions.zipWith(groupAccounts);

        return zip.map(result -> {
            UniversalResponse universalResponse = new UniversalResponse();
            Map<String, Object> data = Map.of("contributions", result.getT1().getData(), "accounts", result.getT2().getData());
            universalResponse.setStatus("success");
            universalResponse.setMessage(getResponseMessage("groupAccountsAndContributions"));
            universalResponse.setData(data);
            return universalResponse;
        });
    }

    @Override
    public Mono<UniversalResponse> addContributionReceiptPayment(MakecontributionWrapper makecontributionWrapper, FilePart file, String username) {
        return Mono.fromCallable(() -> {
            Optional<Accounts> accountsOptional = accountsRepository.findById(makecontributionWrapper.getGroupaccountid());
            if (accountsOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("accountNotFound"), new ArrayList<>());

            Accounts accounts = accountsOptional.get();

            MemberWrapper memberWrapper = chamaKycService.searchMonoMemberByPhoneNumber(username);

            if (memberWrapper == null)
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), new ArrayList<>());

            Pageable pageable = PageRequest.of(0, 10);
            List<Contributions> contributionsList = contributionsRepository.findByMemberGroupId(accounts.getGroupId(), pageable);

            if (contributionsList.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("contributionNotFound"), new ArrayList<>());

            ContributionSchedulePayment contributionSchedulePayment = contributionSchedulePaymentRepository.findByContributionScheduledId(makecontributionWrapper.getSchedulePaymentId());
            if (contributionSchedulePayment == null)
                return new UniversalResponse("fail", getResponseMessage("contributionScheduleNotFound"), new ArrayList<>());

            Contributions contributions = contributionsList.get(0);
            Long contributionId = contributions.getId();
            String imageUrl = fileHandlerService.uploadFile(file);

            ContributionPayment payment = new ContributionPayment();
            payment.setAmount((int) makecontributionWrapper.getAmount());
            payment.setContributionId(contributionId);
            payment.setPhoneNumber(username);
            payment.setTransactionId(makecontributionWrapper.getReceiptNumber());
            payment.setPaymentType("RECEIPT");
            payment.setPaymentStatus(PaymentEnum.PAYMENT_PENDING.name());
            payment.setReceiptImageUrl(imageUrl);
            payment.setIsCombinedPayment(makecontributionWrapper.getIsCombinedPayment());
            payment.setTransactionId(TransactionIdGenerator.generateTransactionId("CNT"));
            payment.setIsPenalty(makecontributionWrapper.getIsPenaltyPayment());
            payment.setPenaltyId(makecontributionWrapper.getIsPenaltyPayment() ? makecontributionWrapper.getPenaltyId() : 0L);
            payment.setGroupAccountId(accounts.getId());
            payment.setSchedulePaymentId(makecontributionWrapper.getSchedulePaymentId());

            payment = contributionsPaymentRepository.saveAndFlush(payment);
            TransactionsPendingApproval transactionsPendingApproval = new TransactionsPendingApproval();
            transactionsPendingApproval.setAccount(accounts);
            transactionsPendingApproval.setAmount(makecontributionWrapper.getAmount());
            transactionsPendingApproval.setCapturedby(username);
            transactionsPendingApproval.setContribution(contributions);
            transactionsPendingApproval.setPhonenumber(username);
            transactionsPendingApproval.setContribution_narration(String.format("%s deposit amount %.2f for %s has been been made successfully",
                    memberWrapper.getFirstname().concat(" ").concat(memberWrapper.getLastname()), makecontributionWrapper.getAmount(), contributions.getName()));
            transactionsPendingApproval.setPending(true);
            transactionsPendingApproval.setContributionPaymentId(payment.getId());

            transactionspendingaApprovalRepo.save(transactionsPendingApproval);
            return new UniversalResponse("success", transactionsPendingApproval.getContribution_narration(), new ArrayList<>());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public void writeOffLoansAndPenalties(String memberInfo) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = gson.fromJson(memberInfo, JsonObject.class);

            // get member id and group id
            long memberId = jsonObject.get("memberId").getAsLong();
            long groupId = jsonObject.get("groupId").getAsLong();

            // find penalties (Loan and Contribution)
            Optional<GroupWrapper> groupWrapperOptional = chamaKycService.getGroupById(groupId);

            groupWrapperOptional.ifPresentOrElse(group -> {

                List<Contributions> groupContributions = contributionsRepository.findAllByMemberGroupId(group.getId());

                // clear contribution penalties
                groupContributions.parallelStream()
                        .map(contribution -> penaltyRepository.findByUserIdAndContributionId(memberId, contribution.getId()))
                        .flatMap(List::stream)
                        .forEach(penalty -> {
                            penalty.setPaid(true);
                            penalty.setSoftDelete(true);
                            penalty.setPaymentStatus(PaymentEnum.PAYMENT_WRITTEN_OFF.name());
                            penaltyRepository.save(penalty);
                        });

                // clear loans and loan penalties
                loansdisbursedRepo.findAllByGroupIdAndMemberId(group.getId(), memberId)
                        .parallelStream()
                        .forEach(loanDisbursed -> {
                            loanDisbursed.setDueamount(0.0);
                            loanDisbursed.setSoftDelete(true);
                            loansdisbursedRepo.save(loanDisbursed);

                            loanPenaltyRepository.findAllByMemberIdAndLoansDisbursed(memberId, loanDisbursed)
                                    .parallelStream()
                                    .forEach(loanPenalty -> {
                                        loanPenalty.setDueAmount(0.0);
                                        loanPenalty.setSoftDelete(true);
                                        loanPenalty.setPaymentStatus(PaymentEnum.PAYMENT_WRITTEN_OFF.name());
                                    });
                        });
            }, () -> log.info("Group not found... On Writing off loans and penalties"));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void editContributionName(String contributionNameUpdate) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = gson.fromJson(contributionNameUpdate, JsonObject.class);

            long groupId = jsonObject.get("groupId").getAsLong();
            String contributionName = jsonObject.get("contributionName").getAsString();
            String modifiedBy = jsonObject.get("modifiedBy").getAsString();

            Optional<GroupWrapper> optionalGroupWrapper = chamaKycService.getGroupById(groupId);

            optionalGroupWrapper.ifPresentOrElse(group -> {
                Optional<Contributions> optionalContribution = contributionsRepository.findFirstByMemberGroupIdOrderById(group.getId());

                optionalContribution.ifPresentOrElse(contribution -> {
                    contribution.setName(contributionName);
                    contribution.setLastModifiedDate(new Date());
                    contribution.setLastModifiedBy(modifiedBy);

                    contributionsRepository.save(contribution);
                }, () -> log.info("Contribution not found with group id {} ... on contribution name edit.", groupId));
            }, () -> log.info("Group not found with id {} ... on updating group contribution name!", groupId));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public void updateGroupCoreAccount(String groupCoreAccountInfo) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = gson.fromJson(groupCoreAccountInfo, JsonObject.class);

            long groupId = jsonObject.get("groupId").getAsLong();
            String cbsAccount = jsonObject.get("account").getAsString();
            String initialBalance = jsonObject.get("initialBalance").getAsString();
            String modifiedBy = jsonObject.get("modifiedBy").getAsString();

            List<Accounts> groupAccounts = accountsRepository.findByGroupIdOrderByCreatedOnAsc(groupId);

            Accounts account = groupAccounts.get(0);

            account.setActive(true);
            JsonObject accountDetails = new JsonObject();
            accountDetails.addProperty("account_number", cbsAccount);
            account.setAccountdetails(accountDetails.toString());
            account.setAvailableBal(Double.parseDouble(initialBalance));
            account.setAccountbalance(Double.parseDouble(initialBalance));
            account.setLastModifiedBy(modifiedBy);
            accountsRepository.save(account);

            Optional<Contributions> optionalContribution = contributionsRepository.findByMemberGroupId(account.getGroupId());
            optionalContribution.ifPresentOrElse(contribution -> createInitialContribution(contribution, initialBalance),
                    () -> log.info("Group contribution not found... On enabling group"));

            log.info("Updated Group Core Account with ID {}", groupId);
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }

    @Override
    public Mono<UniversalResponse> getOverpaidContributions(String username) {
        return Mono.fromCallable(() -> {
                    List<OverpaidContribution> overpaidContributions = overpaidContributionRepository.findByPhoneNumber(username);

                    if (overpaidContributions.isEmpty()) {
                        return new ArrayList<>();
                    }

                    return overpaidContributions.parallelStream()
                            .map(oc -> {
                                String groupName = groupRepository.findById(oc.getGroupId()).orElseThrow(() -> new RuntimeException("Group not found")).getName();
                                return Map.of("username", oc.getPhoneNumber(),"groupName",groupName, "amount", oc.getAmount());
                            })
                            .collect(Collectors.toList());
                })
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("overpaidContributions"), res));
    }

//    @Override
//    public Mono<UniversalResponse> getOutstandingContributio(String s) {
//        return null;
//    }


}
