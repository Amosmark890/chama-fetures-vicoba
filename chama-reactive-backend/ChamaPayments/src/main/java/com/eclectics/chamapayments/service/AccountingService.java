package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.model.*;
import com.eclectics.chamapayments.wrappers.request.AccountDto;
import com.eclectics.chamapayments.wrappers.request.ContributionPaymentDto;
import com.eclectics.chamapayments.wrappers.request.MakecontributionWrapper;
import com.eclectics.chamapayments.wrappers.request.RequestwithdrawalWrapper;
import com.eclectics.chamapayments.wrappers.response.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface AccountingService {
    List<AccountType> findAccountTypes();

    AccountType getAccounttypebyName(String name);

    boolean checkIfGrouphasWalletAccount(long groupId);

    Optional<AccountType> getAccounttypebyID(long accounttypeid);

    boolean checkAccounttype(String name);

    void saveAccounttype(AccountType accountType);

    boolean checkIfAccountExists(String accountnumber, long groupId);

    void createAccount(Accounts accounts);

    List<AccountDto> getAccountbyGroup(long groupId);

    List<Accounts> getAccountsbyGroupandType(long groupId, AccountType accountType);

    List<ContributionType> getContributiontypes();

    void saveContributiontype(ContributionType contributionType);

    boolean checkContributiontype(String name);

    List<ScheduleTypes> getScheduletypes();

    boolean checkScheduletype(String name);

    void saveScheduleType(ScheduleTypes scheduleTypes);

    boolean checkAmounttype(String name);

    void createAmounttype(AmountType amountType);

    List<AmountType> getAmounttypes();

    Optional<AmountType> getAmounttypebyID(long id);

    Optional<ContributionType> getContributiontypebyID(long contributiontypeid);

    double getBalancebyContributions(Contributions contributions);

    Optional<ScheduleTypes> getScheduletypebyID(long schedultypeid);

    int countContributionsbyNameandGroup(String name, long groupId);

    Contributions createContribution(Contributions contributions);

    List<Contributions> getContributionsbyGroup(long groupId, Pageable pageable);

    int countcontributionsByGroup(long groupId);

    Optional<Accounts> getAccountbyId(long accountid);

    Optional<Contributions> getContributionbyId(long contributionid);

    Optional<TransactionsPendingApproval> getPendingPaymentapprovalbyId(long id);

    Optional<WithdrawalsPendingApproval> getPendingWithdrawalapprovalbyId(long id);

    long countAllContributions();

    int countActiveContributions();

    int countInactiveContributions();

    double getSumbycontributions(Contributions contributions);

    Mono<UniversalResponse> approveContributionPayment(long paymentId, boolean approved, String approvedBy);

    List<PaymentApproval> getPendingPaymentApprovalByGroupId(long groupid);

    List<PaymentApproval> getPendingPaymentApprovalByUser(String phonenumber);

    List<PaymentApproval> getPendingPaymentApprovalByContribution(Contributions contributions);

    PageDto getPendingWithdrawalRequestByGroupId(long groupId, int page, int size);

    List<WithdrawalApproval> getPendingWithdrawalRequestbyUser(String phonenumber);

    List<WithdrawalApproval> getPendingWithdrawalRequestByContribution(Contributions contributions);

    List<TransactionLogWrapper> getTransactionsbyGroup(long groupid, Pageable pageable);

    UniversalResponse getTransactionsByGroup(long groupid, Pageable pageable);

    int countTransactionsByGroup(long groupid);

    int countWithdrawalsByGroup(long groupid);

    List<TransactionLogWrapper> getWithdrawalsbyGroup(long groupid, Pageable pageable);

    List<TransactionLogWrapper> getTransactionsbyUser(String phonenumber, Pageable pageable);

    UniversalResponse getTransactionsByUser(String phonenumber, Pageable pageable);

    int countTransactionsbyUser(String phonenumber);

    int countWithdrawalsbyUser(String phonenumber);

    List<TransactionLogWrapper> getWithdrawalsbyUser(String phonenumber, Pageable pageable);

    List<TransactionLogWrapper> getTransactionByContributions(Long contributionId, Pageable pageable);

    UniversalResponse getTransactionsByContributions(Long contributionId, Pageable pageable);

    int countTransactionByContributions(Contributions contributions);

    int countWithdrawalsbyContribution(Contributions contributions);

    List<TransactionLogWrapper> getWithdrawalsbyContribution(Long contributionId, Pageable pageable);

    List<TransactionLogWrapper> getTransactionsbyUserandContributions(String phonenumber, Contributions contributions, Pageable pageable);

    int countTransactionsbyUserandContributions(String phonenumber, Contributions contributions);

    int countTransactionsbyUserandGroupId(String phonenumber, long groupId);

    List<TransactionLogWrapper> getTransactionsbyUserandGroupId(String phonenumber, long groupId, Pageable pageable);

    int countTransactionsbyAccount(Accounts accounts);

    List<TransactionLogWrapper> getTransactionsbyAccount(Long accountId, Pageable pageable);

    UniversalResponse getTransactionsByAccount(Long accountId, Pageable pageable);

    int countWithdrawalsbyAccount(Accounts accounts);

    List<TransactionLogWrapper> getWithdrawalsbyAccount(Long accountId, Pageable pageable);

    double getAverageheldinAccounts();

    double getSumheldinAccounts();

    long getAccountsCount();

    long getPaymentsCount();

    double getPaymentsTotal();

    double getPaymentsAvg();

    long getWithdrawalsCount();

    double getWithdrawalsTotal();

    double getWithdrawalsAvg();

    void recordInvestment(Investments investments);

    boolean checkInvestmentRecord(String name, long groupId);

    List<InvestmentWrapper> getInvestmentrecordsbyGroup(long groupId, Pageable pageable);

    int countInvestmentrecordsbyGroup(long groupId);

    Optional<Investments> getInvestmentbyID(long id);

    List<Map<String, Object>> groupTransactionsDetailed(Date startDate, Date endDate, String period, String group);

    List<Map<String, Object>> groupTransactionsbyDate(Date startDate, Date endDate, String period, String group, String country);

    List<Map<String, Object>> groupRegistrationTrend(Date startDate, Date endDate, String period, String group, String country);

    UniversalResponse getGroupTransactionByType(Date startDate, Date endDate, String period, String transactionType, String group, String additional, Pageable pageable);

    UniversalResponse getGroupQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable);

    UniversalResponse getMemberQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable, String group);

    UniversalResponse getGroupAccountsByType(long groupId, Long accountType);

    UniversalResponse createNewAccountType(String name, String prefix, List<String> requiredFields);

    UniversalResponse getContributionTypes();

    UniversalResponse getScheduleTypes();

    Mono<UniversalResponse> recordWithdrawal(RequestwithdrawalWrapper requestwithdrawalWrapper, String createdBy);

    Mono<UniversalResponse> checkLoanLimit(String phoneNumber, long groupId, Long contributionId, Long productId);

    Mono<UniversalResponse> userWalletBalance();

    Mono<UniversalResponse> groupAccountBalance(Long groupId);

    Mono<UniversalResponse> addContribution(ContributionDetailsWrapper wrapper);

    Mono<UniversalResponse> makeContribution(ContributionPaymentDto contributionPayment, String walletAccount);

    Mono<UniversalResponse> makeContributionForOtherMember(ContributionPaymentDto contributionPayment, String walletAccount);

    Consumer<String> fundsTransferCallback();

    void createGroupAccount(String accountInfo);

    void createGroupContribution(String contributionInfo);

    Mono<UniversalResponse> approveWithdrawalRequest(long requestId, boolean approve, String approvedBy);

    Mono<UniversalResponse> getUserContributionPayments(String phoneNumber);

    Mono<UniversalResponse> getUserContributionPayments(String phoneNumber, Integer page, Integer size);

    Mono<UniversalResponse> getGroupContributionPayments(Long contributionId, Integer page, Integer size);

    Mono<UniversalResponse> getUssdGroupContributionPayments(Long contributionId, Integer page, Integer size);

    Mono<UniversalResponse> getUserUpcomingPayments(String phoneNumber, long groupId);

    Mono<UniversalResponse> getUserUpcomingPayments(String phoneNumber);

    Mono<UniversalResponse> getAllUserUpcomingPayments(String phoneNumber);

    void enableGroupContributions(String groupInfo);

    void disableGroupContributions(String groupInfo);

    Mono<UniversalResponse> updateContribution(ContributionDetailsWrapper contributionWrapper, String modifier);

    void groupAccountBalanceInquiry();

    Mono<UniversalResponse> getUserContributionsPerGroup(String phoneNumber);

    Mono<UniversalResponse> getAllMemberPenalties(String username);

    Mono<UniversalResponse> getGroupContributionPenalties(Long groupId, int page, int size);

    Mono<UniversalResponse> editContribution(ContributionDetailsWrapper contributionDetailsWrapper, String username);

    Mono<UniversalResponse> getGroupContributions(Long groupId);

    Mono<UniversalResponse> getGroupContribution(Long contributionId);

    Mono<UniversalResponse> getGroupAccountsMemberBelongsTo(String username);

    Mono<UniversalResponse> getGroupTransactions(Long groupId, Integer page, Integer size);

    Mono<UniversalResponse> getUserTransactions(String userame, Integer page, Integer size);

    Mono<UniversalResponse> getUserTransactionsByContribution(String username, Long groupId, Integer page, Integer size);

    Mono<UniversalResponse> getUserTransactionsByGroup(String username, Long groupId, Integer page, Integer size);

    Mono<UniversalResponse> getUserSummary(String phone, Long contributionId);

    Mono<UniversalResponse> payForContributionPenalty(ContributionPaymentDto dto, String username);

    Mono<UniversalResponse> fetchGroupAccountAndContributions(Long groupId);

    Mono<UniversalResponse> addContributionReceiptPayment(MakecontributionWrapper makecontributionWrapper, FilePart file, String username);

    void writeOffLoansAndPenalties(String memberInfo);

    void editContributionName(String contributionNameUpdate);

    void updateGroupCoreAccount(String groupCoreAccountInfo);

    Mono<UniversalResponse> getOverpaidContributions(String username);
//
//    Mono<UniversalResponse> getOutstandingContributio(String s);
}
