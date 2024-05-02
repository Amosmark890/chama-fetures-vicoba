package com.eclectics.chamapayments.repository;


import com.eclectics.chamapayments.model.TransactionTypes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface TransactionTypeRepository extends JpaRepository<TransactionTypes, Long> {
    List<TransactionTypes> findAllById(long id);
}
