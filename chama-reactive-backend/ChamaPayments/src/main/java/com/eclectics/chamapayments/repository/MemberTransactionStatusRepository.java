package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.MemberTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberTransactionStatusRepository extends JpaRepository<MemberTransactionStatus, Long> {
    Optional<MemberTransactionStatus> findFirstByWalletAccount(String walletAccount);

    @Query(nativeQuery = true, value = "select * from member_transaction_status mts where mts.last_transacted < now() and mts.status = 'CANNOT_TRANSACT'")
    List<MemberTransactionStatus> findAllByLastTransacted();

}