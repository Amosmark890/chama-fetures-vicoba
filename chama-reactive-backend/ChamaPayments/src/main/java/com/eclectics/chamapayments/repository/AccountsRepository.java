package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.AccountType;
import com.eclectics.chamapayments.model.Accounts;
import com.eclectics.chamapayments.model.jpaInterfaces.AccountsTotals;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountsRepository extends JpaRepository<Accounts, Long> {
    int countByAccountdetailsAndGroupId(String accountdetails, long groupId);

    List<Accounts> findByGroupIdAndActive(long groupId, boolean isActive);

    List<Accounts> findByGroupIdOrderByCreatedOnAsc(long groupId);

    List<Accounts> findByGroupIdAndAccountTypeAndActive(long groupId, AccountType accountType, boolean isActive);

    @Query("SELECT COALESCE(SUM(a.accountbalance), 0) FROM  Accounts a")
    double getTotalheldinAccounts();

    @Query("SELECT COALESCE( AVG(a.accountbalance),0) FROM Accounts a")
    double getAverageheldinAccounts();

    int countByGroupIdAndAccountType(long groupId, AccountType accountType);

    @Query(nativeQuery = true, value = "select * from accounts where account_type= 1 and groupid=:groupId")
    Optional<Accounts> findByGroupIdWallet(long groupId);

    Optional<Accounts> findFirstByGroupId(long groupId);

    @Query(nativeQuery = true, value = "select ceil(sum(a.available_bal)) as groupbalances, sum(a.accountbalance) as coreaccountbalances from accounts a")
    AccountsTotals getGroupAccountBalances();

}
