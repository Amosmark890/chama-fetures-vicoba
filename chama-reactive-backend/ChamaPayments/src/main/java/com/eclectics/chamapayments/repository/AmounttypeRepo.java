package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.AmountType;
import org.springframework.data.jpa.repository.JpaRepository;


public interface AmounttypeRepo extends JpaRepository<AmountType,Long> {
    int countByName(String name);
}
