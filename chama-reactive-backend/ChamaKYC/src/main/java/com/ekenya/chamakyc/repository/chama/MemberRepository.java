package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query(nativeQuery = true, value = "SELECT * from members_tbl where user_id = :userId ")
    Optional<Member> findByUserId(@Param("userId") long userId);

    Optional<Member> findByImsi(String username);
//    Optional<Member> findMemberByImsi(String username);
    Optional<Member> findMemberByEsbwalletaccount(String username);


    List<Member> findAllByCreatedOnBetweenOrderByCreatedOnAsc(Date startDate, Date endDate);

    @Query(nativeQuery = true, value = "select * from members_tbl mt " +
            "    inner join group_membership_tbl gm on mt.id = gm.members_id " +
            "    inner join groups_tbl gt on gm.group_id = gt.id " +
            "    where gt.name= :group and mt.created_on between :startDate and :endDate order by mt.created_on")
    List<Member> findMembersByGroupAndCreatedOnBetweenAndOrderByAsc(@Param("group") String groupName, Date startDate, Date endDate);

    long countByActive(boolean active);

    long countByActiveAndSoftDeleteFalse(boolean active);

    List<Member> findAllByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean b, boolean status);

    @Query(nativeQuery = true, value = "select * from members_tbl mt " +
            "    inner join group_membership_tbl gm on mt.id = gm.members_id " +
            "    inner join groups_tbl gt on gm.group_id= gt.id " +
            "    where gt.name= :group and mt.created_on between :startDate and :endDate  and gm.activemembership= :status and gm.soft_delete=:softDelete order by mt.created_on ")
    List<Member> findMemberByGroupNameAndCreatedOnBetweenAndActiveAndSoftDeleteOrderByAsc(String group, Date startDate, Date endDate, boolean status, boolean softDelete);
}
