package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.OverpaidContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverpaidContributionRepository extends JpaRepository<OverpaidContribution, Long> {

    Optional<OverpaidContribution> findByMemberIdAndContributionId(Long memberId, Long contributionId);

    List<OverpaidContribution> findByPhoneNumber(String username);

//    Optional<OverpaidContribution> findByLastEsbTransactionCode();
}