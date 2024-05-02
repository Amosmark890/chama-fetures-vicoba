package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.wrappers.SearchDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findByCreatorAndNameLike(Member creator, String name);

    Optional<Group> findByNameOrCbsAccount(String name, String cbsAccount);

    long countAllByCreator(Member creator);

    Optional<Group> findByCbsAccount(String cbsAccount);

    List<Group> findAllByCreatedOnBetweenOrderByCreatedOnAsc(Date startDate, Date endDate);

    long countByActive(boolean active);

    long countByActiveAndSoftDeleteFalse(boolean active);

    List<Group> findGroupsByActiveAndSoftDeleteAndCreatedOnBetween(boolean active, boolean softDelete, Date startDate, Date endDate, Pageable pageable);

    Optional<Group> findGroupByActiveAndSoftDeleteAndNameLike(boolean b, boolean b1, String group);

    @Query(nativeQuery = true, value = "SELECT t.ct as totalCount, t.res as resData from search_group(:groupName, :cbsAccount, :createdBy, :creatorPhone, :status, :createdOnStart, :createdOnEnd, :page, :size) as t(ct, res)")
    SearchDetails searchGroup(String groupName, String createdBy, String creatorPhone, String cbsAccount, String status, String createdOnStart, String createdOnEnd, int page, int size);

    long countBySoftDeleteFalse();

    long countAllBySoftDeleteFalse();

    Page<Group> findAllBySoftDeleteFalseOrderByCreatedOnDesc(Pageable pageable);
}
