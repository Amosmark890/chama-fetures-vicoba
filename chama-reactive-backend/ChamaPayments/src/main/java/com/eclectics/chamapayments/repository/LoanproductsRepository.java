package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.LoanProducts;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Date;
import java.util.List;


public interface LoanproductsRepository extends JpaRepository<LoanProducts,Long> {
    int countByGroupIdAndProductname(long groupId, String productname);

    List<LoanProducts> findAllByGroupId(long groupId);
    List<LoanProducts> findAllByGroupIdAndIsActive(long groupId,Boolean isActive,Pageable pageable);
    List<LoanProducts> findAllByIsActiveAndSoftDeleteAndCreatedOnBetween(boolean isActive, boolean softDelete, Date startDate , Date endDate, Pageable pageable);
    int countLoanProductsByIsActiveAndSoftDeleteAndCreatedOnBetween(boolean isActive, boolean softDelete, Date startDate , Date endDate);
    List<LoanProducts> findAllBySoftDeleteAndCreatedOnBetween(boolean softDelete, Date startDate, Date endDate,Pageable pageable);
    int countAllBySoftDeleteAndCreatedOnBetween(boolean softDelete, Date startDate, Date endDate);
    List<LoanProducts> findAllByGroupIdAndSoftDelete(long groupId,boolean softDelete,Pageable pageable);
    int countAllByGroupIdAndSoftDelete(long groupId, boolean softDelete);
    List<LoanProducts> findAllByGroupIdAndIsActiveAndCreatedOnBetween(long groupId,Boolean isActive,Date startDate, Date endDate,Pageable pageable);
    int countByGroupIdAndIsActiveAndCreatedOnBetween(long groupId, boolean isActive, Date startDate, Date endDate);
    List<LoanProducts> findAllByGroupIdAndIsActiveAndCreatedOnBetween(long groupId, boolean b, Date startDate, Date endDate, Pageable pageable);

    List<LoanProducts> findAllByGroupIdAndIsActive(Long groupId, boolean isActive);
}
