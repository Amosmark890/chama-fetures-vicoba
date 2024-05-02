package com.eclectics.chamapoll.service.impl;

import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.service.ChamaKycService;
import com.eclectics.chamapoll.wrappers.GroupMemberWrapper;
import com.eclectics.chamapoll.wrappers.GroupWrapper;
import com.eclectics.chamapoll.wrappers.MemberWrapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alex Maina
 * @created 28/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChamaKycServiceImpl implements ChamaKycService {

    private final Gson gson;
    private final JedisConnectionFactory jedisConnectionFactory;

    private Jedis jedis;

    private static final String CACHE_NAME = "chama-cache";

    public List<MemberWrapper> getMembers() {
        jedis = new Jedis(jedisConnectionFactory.getHostName(),jedisConnectionFactory.getPort(),10000);
        String memberDataJson = jedis.hget(CACHE_NAME, "member-data");
        List<String> memberData = gson.fromJson(memberDataJson, new TypeToken<List<String>>() {
        }.getType());

        return memberData
                .parallelStream()
                .map(json -> gson.fromJson(json, MemberWrapper.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<GroupWrapper> getGroups() {
        jedis = new Jedis(jedisConnectionFactory.getHostName(),jedisConnectionFactory.getPort(),10000);
        String groupDataJson = jedis.hget(CACHE_NAME, "group-data");
        List<String> groupData = gson.fromJson(groupDataJson, new TypeToken<List<String>>() {
        }.getType());

        return groupData
                .stream()
                .map(String.class::cast)
                .map(json -> gson.fromJson(json, GroupWrapper.class))
                .collect(Collectors.toList());
    }

    private List<GroupMemberWrapper> getGroupMembers() {
        jedis = new Jedis(jedisConnectionFactory.getHostName(),jedisConnectionFactory.getPort(),10000);
        String groupMembershipData = jedis.hget(CACHE_NAME, "group-members");
        List<String> groupMembership = gson.fromJson(groupMembershipData, new TypeToken<List<String>>() {
        }.getType());

        return groupMembership
                .stream()
                .map(json -> gson.fromJson(json, GroupMemberWrapper.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getMemberGroupNameById(long memberGroupId) {
        List<GroupMemberWrapper> groupMember = getGroupMembers();
        return groupMember.stream()
                .filter(memberGroup -> memberGroup.getGroupId() == memberGroupId)
                .map(GroupMemberWrapper::getGroupName)
                .findAny();
    }

    @Override
    public Optional<MemberWrapper> searchMemberByPhoneNumber(String phoneNumber) {
        List<MemberWrapper> memberList = getMembers();
        return memberList.stream()
                .filter(member -> member.getPhonenumber().trim().equalsIgnoreCase(phoneNumber.trim()))
                .findAny();
    }

    @Override
    public Optional<String> getGroupNameByGroupId(long id) {
        List<GroupWrapper> groupList = getGroups();
        return groupList.stream()
                .filter(group -> group.getId() == id)
                .map(GroupWrapper::getName)
                .findAny();
    }

    @Override
    public List<GroupWrapper> findGroupsCreatedBetweenOrderAsc(Date startDate, Date endDate) {
        List<GroupWrapper> groupsList = getGroups();
        return groupsList.stream()
                .filter(group -> group.getCreatedOn().after(startDate) && group.getCreatedOn().before(endDate))
                .sorted()
                .collect(Collectors.toList());

    }

    @Override
    public Optional<GroupWrapper> findGroupsByActiveAndSoftDeleteAndNameLike(boolean active, boolean softDelete, String groupName) {
        List<GroupWrapper> groupsList = getGroups();
        return groupsList.stream()
                .filter(group -> group.isActive() == active)
                .filter(group -> group.isDeleted() == softDelete)
                .filter(group -> group.getName().trim().equalsIgnoreCase(groupName.trim()))
                .findFirst();
    }

    @Override
    public Optional<MemberWrapper> getMemberDetailsById(long memberId) {
        List<MemberWrapper> memberList = getMembers();
        return memberList.stream()
                .filter(member -> member.getId() == memberId)
                .findFirst();
    }

    @Override
    public Optional<GroupWrapper> getGroupById(long groupId) {
        List<GroupWrapper> groupList = getGroups();
        return groupList.stream()
                .filter(group -> group.getId() == groupId)
                .findAny();
    }

    @Override
    public List<GroupWrapper> findGroupsByActiveAndSoftDeleteAndCreatedOnBetween(boolean status, boolean softDelete, Date startDate, Date endDate, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int skipCount = (page - 1) * size;
        return getGroups()
                .stream()
                .filter(group -> group.isActive() == status)
                .filter(group -> group.isDeleted() == softDelete)
                .filter(group -> group.getCreatedOn().after(startDate) && group.getCreatedOn().before(endDate))
                .skip(skipCount)
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberWrapper> findAllByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean softDelete, boolean status) {
        return getMembers()
                .stream()
                .filter(member -> member.isActive() == status)
                .filter(member -> member.isSoftDelete() == softDelete)
                .filter(member -> member.getCreatedOn().after(startDate) && member.getCreatedOn().before(endDate))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GroupMemberWrapper> getGroupMembershipByGroupIdAndMemberId(long groupId, long memberId) {
        return getGroupMembers()
                .stream()
                .filter(memberGroup -> memberGroup.getGroupId() == groupId)
                .filter(memberGroup -> memberGroup.getGroupId() == memberId)
                .findFirst();
    }

    @Override
    public Optional<GroupWrapper> getGroupByName(String groupName) {
        return getGroups().stream()
                .filter(group -> group.getName().trim().equalsIgnoreCase(groupName.trim()))
                .findFirst();
    }

    @Override
    public Optional<String> getMemberPermission(int groupId, String phoneNumber) {
        jedis = new Jedis(jedisConnectionFactory.getHostName(),jedisConnectionFactory.getPort(),10000);
        String membershipDataJson = jedis.hget(CACHE_NAME, "group-members");
        List<String> membershipData = gson.fromJson(membershipDataJson, new TypeToken<List<String>>() {
        }.getType());

        return membershipData
                .parallelStream()
                .map(group -> gson.fromJson(group, GroupMemberWrapper.class))
                .filter(membership -> membership.getGroupId() == groupId && Objects.equals(membership.getPhoneNumber(), phoneNumber))
                .map(GroupMemberWrapper::getPermissions)
                .findFirst();
    }

    @Override
    public List<String> getMembersPhonesInGroup(long groupId) {
        return getGroupMembers()
                .parallelStream()
                .filter(gm -> gm.getGroupId() == groupId)
                .map(GroupMemberWrapper::getPhoneNumber)
                .collect(Collectors.toList());

    }

    @Override
    public List<GroupMemberWrapper> findAllByGroupsAndActiveMembership(long memberGroupId, boolean active) {
        return getGroupMembers()
                .stream()
                .filter(gm -> gm.getGroupId() == memberGroupId && Boolean.TRUE.equals(active))
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberWrapper> getGroupMembers(long memberGroupId) {
        List<GroupMemberWrapper> groupMemberShip = getGroupMembers();
        return groupMemberShip
                .stream()
                .filter(gm -> gm.getGroupId() == memberGroupId)
                .mapToLong(GroupMemberWrapper::getMemberId)
                .mapToObj(this::getMemberDetailsById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    @Override
    public boolean getMemberGroupByMemberIdAndGroupId(long id, long groupId) {
        long count = getGroupMembers().stream()
                .filter(gm -> gm.getMemberId() == id && gm.getGroupId() == groupId)
                .count();
        return count == 1;
    }

    @Override
    public Set<Long> getMemberGroupIdsByMemberId(long id) {
        return getGroupMembers()
                .stream()
                .filter(membership -> membership.getMemberId() == id)
                .map(GroupMemberWrapper::getGroupId)
                .collect(Collectors.toSet());
    }

    @Override
    public Long countGroupMembers(long groupId) {
        return getGroupMembers().parallelStream()
                .filter(member -> member.getGroupId() == groupId && member.isActivemembership())
                .count();
    }

    @Override
    public List<Pair<String, String>> findAllGroupMembersPhonesAndLanguage(long id) {
        return getGroupMembers(id)
                .stream()
                .map(gm ->Pair.of(gm.getPhonenumber(), gm.getLanguage()))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<GroupWrapper> getGroupByIdAndStatus(Long groupId, Status active) {
        List<GroupWrapper> groupList = getGroups();
        return groupList.stream()
                .filter(group -> group.getId() == groupId && group.isActive()==true)
                .findAny();
    }

}
