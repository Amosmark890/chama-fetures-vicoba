package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.ScheduleTypes;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
public interface ScheduleTypeRepository extends JpaRepository<ScheduleTypes,Long> {
    int countByName(String name);
    ScheduleTypes findByName(String name);
}
