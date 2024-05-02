package com.eclectics.chamapayments.repository;


import com.eclectics.chamapayments.model.Investments;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


/**
 * interface name: InvestmentsRepo
 * Creater: wgicheru
 * Date:4/6/2020
 */
public interface InvestmentsRepo extends JpaRepository<Investments,Long> {
    int countByNameAndGroupId(String name, long groupId);
    int countByGroupId(long groupId);
    List<Investments> findByGroupId(long groupId , Pageable pageable);
}
