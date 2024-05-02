package com.eclectics.chamapayments.repository;



import com.eclectics.chamapayments.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * The interface Account type repository.
 */
public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {

    List<AccountType> findAllById(long id);
    boolean existsAccountTypeByAccountName(String accountname);
    AccountType findByAccountNameContains(String accountname);
}
