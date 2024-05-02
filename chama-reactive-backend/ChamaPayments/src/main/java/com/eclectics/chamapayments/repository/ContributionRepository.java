package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.ScheduleTypes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * @author Alex Maina
 * @created 07/12/2021
 */
public interface ContributionRepository extends JpaRepository<Contributions, Long> {
    int countByNameAndMemberGroupId(String name, long groups);

    List<Contributions> findByMemberGroupId(long groupId, Pageable pageable);

    Optional<Contributions> findByMemberGroupId(long groupId);

    int countByMemberGroupId(long groupId);

    int countByActiveTrue();

    int countByActiveFalse();

    List<Contributions> findAllByMemberGroupId(long groupId);

    List<Contributions> findAllByScheduleType(ScheduleTypes scheduleTypes);

    List<Contributions> findAllByScheduleTypeAndActiveTrue(ScheduleTypes scheduleTypes);

    Optional<Contributions> findByIdAndMemberGroupId(Long groupId, Long memberGroupId);

    Optional<Contributions> findByAmountTypeId(long amountTypeId);

    Optional<Contributions> findFirstByMemberGroupIdOrderById(long groupId);
}
