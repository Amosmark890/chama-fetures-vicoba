package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.Accounts;
import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.TransactionsLog;
import com.eclectics.chamapayments.model.jpaInterfaces.TransactionTotalAgg;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TransactionlogsRepo extends JpaRepository<TransactionsLog, Long> {
    @Query(nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid",
            value = "SELECT * FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid ORDER BY atl.created_on DESC")
    Page<TransactionsLog> getTransactionsbygroup(@Param("groupid") long groupid, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT count(*) FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid")
    int countTransactionsbygroup(@Param("groupid") long groupid);

    /**
     * Find by debit phonenumber list.
     *
     * @param phonenumber the phonenumber
     * @param pageable    the pageable
     * @return the list
     */
    Page<TransactionsLog> findByDebitphonenumberOrderByCreatedOnDesc(String phonenumber, Pageable pageable);

    int countByDebitphonenumber(String phonenumber);

    Page<TransactionsLog> findByContributionsOrderByCreatedOnDesc(Contributions contributions, Pageable pageable);

    int countByContributions(Contributions contributions);

    List<TransactionsLog> findByDebitphonenumberAndContributionsOrderByCreatedOnDesc(String phonenumber, Contributions contributions, Pageable pageable);

    int countByDebitphonenumberAndContributions(String phonenumber, Contributions contributions);

    @Query(nativeQuery = true,
            countQuery = "SELECT COUNT(*) FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid  AND debitphonenumber=:phonenumber",
            value = "SELECT * FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid AND debitphonenumber=:phonenumber ORDER BY atl.created_on DESC")
    List<TransactionsLog> getTransactionsbygroupandmember(@Param("groupid") long groupid, @Param("phonenumber") String phonenumber, Pageable pageable);

    @Query(nativeQuery = true,
            value = "SELECT count(*) FROM " +
                    "account_transactions_log atl JOIN contributions_tbl ct " +
                    "ON atl.contribution_id=ct.id " +
                    "WHERE ct.member_group_id=:groupid AND debitphonenumber=:phonenumber")
    int countTransactionsbygroupandmember(@Param("groupid") long groupid, @Param("phonenumber") String phonenumber);

    Page<TransactionsLog> findByCreditaccountsOrderByCreatedByDesc(Accounts accounts, Pageable pageable);

    int countByCreditaccounts(Accounts accounts);

    @Query("SELECT SUM(c.transamount) FROM TransactionsLog c WHERE c.contributions=:contribution")
    double getTotalbyContribution(Contributions contribution);

    @Query("SELECT SUM(tl.transamount) FROM TransactionsLog tl")
    double getSumContributed();

    @Query("SELECT AVG(tl.transamount) FROM TransactionsLog tl")
    double getAvgContributed();

    @Query(nativeQuery = true, value = "select sum(transamount) as objects, date(created_on) " +
            "as dateofday from account_transactions_log where created_on between :date1 and :date2" +
            " GROUP BY dateofday")
    List<TransactionTotalAgg> groupTransactionsbyDate(@Param("date1") Date startdate, @Param("date2") Date enddate);

    List<TransactionsLog> findAllByCreatedOnBetweenOrderByCreatedOnDesc(Date startdate, Date enddate);

    List<TransactionsLog> findAllByCreatedOnBetweenOrderByCreatedOnDesc(Date startdate, Date enddate, Pageable pageable);

    Optional<TransactionsLog> findFirstByUniqueTransactionId(String transactionId);
}
