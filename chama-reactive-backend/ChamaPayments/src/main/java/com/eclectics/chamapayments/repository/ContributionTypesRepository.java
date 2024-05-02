package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.ContributionType;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
public interface ContributionTypesRepository extends JpaRepository<ContributionType,Long> {
    int countByName(String name);
}
