package com.eclectics.chamapoll.service;

import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.wrappers.GroupMemberWrapper;
import com.eclectics.chamapoll.wrappers.GroupWrapper;
import com.eclectics.chamapoll.wrappers.MemberWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Alex Maina
 * @created 28/12/2021
 */
public interface ChamaKycService {

    Optional<String> getMemberGroupNameById(long memberGroupId);

    Optional<MemberWrapper> searchMemberByPhoneNumber(String phoneNumber);

    Optional<String> getGroupNameByGroupId(long id);

    List<GroupWrapper> findGroupsCreatedBetweenOrderAsc(Date startDate, Date endDate);

    Optional<GroupWrapper> findGroupsByActiveAndSoftDeleteAndNameLike(boolean active, boolean softDelete, String group);

    Optional<MemberWrapper> getMemberDetailsById(long memberId);

    Optional<GroupWrapper> getGroupById(long groupId);

    List<GroupWrapper> findGroupsByActiveAndSoftDeleteAndCreatedOnBetween(boolean status, boolean b, Date startDate, Date endDate, Pageable pageable);

    List<MemberWrapper> findAllByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean b, boolean status);

    Optional<GroupMemberWrapper> getGroupMembershipByGroupIdAndMemberId(long groupId, long id);

    Optional<GroupWrapper> getGroupByName(String group);

    Optional<String> getMemberPermission(int targetId, String phoneNumber);

    List<String> getMembersPhonesInGroup(long groupId);

    List<GroupMemberWrapper> findAllByGroupsAndActiveMembership(long memberGroupId, boolean active);

    List<MemberWrapper> getGroupMembers(long memberGroupId);

    List<GroupWrapper> getGroups();

    boolean getMemberGroupByMemberIdAndGroupId(long id, long groupId);

    Set<Long> getMemberGroupIdsByMemberId(long id);

    Long countGroupMembers(long groupId);

    List<Pair<String, String>> findAllGroupMembersPhonesAndLanguage(long id);

    Optional<GroupWrapper> getGroupByIdAndStatus(Long groupId, Status active);

}
