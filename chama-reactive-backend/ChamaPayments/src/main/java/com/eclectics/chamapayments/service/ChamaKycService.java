package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.wrappers.response.GroupMemberWrapper;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Alex Maina
 * @created 07/12/2021
 * Micro service comm
 */
public interface ChamaKycService {
    List<MemberWrapper> getMembers();

    Flux<GroupWrapper> getFluxGroups();

    List<GroupWrapper> getGroups();

    Flux<GroupMemberWrapper> getFluxGroupMembers();

    String getMonoMemberGroupNameById(long memberGroupId);

    MemberWrapper searchMonoMemberByPhoneNumber(String phoneNumber);

    Optional<MemberWrapper> searchMemberByPhoneNumber(String phoneNumber);

    Optional<MemberWrapper> searchMemberByUserId(long userId);

    String getMonoGroupNameByGroupId(long id);

    Optional<String> getGroupNameByGroupId(long id);

    Optional<GroupWrapper> getGroupById(long id);

    Flux<GroupWrapper> findMonoGroupsCreatedBetweenOrderAsc(Date startDate, Date endDate);

    GroupWrapper findMonoGroupByActiveAndSoftDeleteAndNameLike(boolean active, boolean softDelete, String groupName);

    MemberWrapper getMonoMemberDetailsById(long memberId);

    GroupWrapper getMonoGroupById(long groupId);

    Flux<GroupWrapper> findFluxGroupsByActiveAndSoftDeleteAndCreatedOnBetween(boolean status, boolean softDelete, Date startDate, Date endDate, Pageable pageable);

    GroupMemberWrapper getMonoGroupMembershipByGroupIdAndMemberId(long groupId, long memberId);

    GroupWrapper getMonoGroupByName(String groupName);

    Optional<String> getMemberPermission(int targetId, String phoneNumber);

    Flux<String> getFluxMembersPhonesInGroup(long groupId);

    Flux<Pair<String, String>> getFluxMembersLanguageAndPhonesInGroup(long groupId);

    Optional<MemberWrapper> getMemberDetailsById(long memberId);

    Flux<MemberWrapper> getFluxMembers();

    Flux<MemberWrapper> getFluxGroupMembers(long memberGroupId);

    Flux<MemberWrapper> getGroupOfficials(long memberGroupId);

    Flux<GroupWrapper> getFluxGroupsMemberBelongs(String username);

    GroupMemberWrapper memberIsPartOfGroup(long groupId, String username);

    GroupWrapper findGroupsByActiveAndSoftDeleteAndName(boolean active, boolean deleted, String group);

    MemberWrapper searchMonoMemberByCoreAccount(String account);

    Flux<MemberWrapper> findAllByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean active, boolean status);

    Flux<GroupMemberWrapper> findAllByGroupsAndActiveMembership(long memberGroupId, boolean active);
}
