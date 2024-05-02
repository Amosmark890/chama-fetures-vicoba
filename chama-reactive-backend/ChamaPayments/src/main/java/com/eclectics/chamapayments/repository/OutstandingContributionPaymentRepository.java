package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.OutstandingContributionPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OutstandingContributionPaymentRepository extends JpaRepository<OutstandingContributionPayment, Long> {

    Optional<OutstandingContributionPayment> findByContributionIdAndMemberId(long contributionId, long memberId);
}