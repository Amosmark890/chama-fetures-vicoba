package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.wrappers.response.GroupMemberWrapper;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Alex Maina
 * @created 22/12/2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChamaKycServiceImpl implements ChamaKycService {

    @Value("${spring.redis.host}")
    private String redisHost;
    @Value("${spring.redis.port}")
    private int redisPort;
    private final Gson gson;
    private final ReactiveStringRedisTemplate redisOperations;
    private final StringRedisTemplate stringRedisTemplate;
    private ReactiveHashOperations<String, String, String> hashOperations;
    private HashOperations<String, String, String> syncHashOperations;

    private static final String CACHE_NAME = "chama-cache";

    @PostConstruct
    private void init() {
        hashOperations = redisOperations.opsForHash();
        syncHashOperations = stringRedisTemplate.opsForHash();
    }

    public List<MemberWrapper> getGroupMembers() {
        String jsonData = syncHashOperations.get(CACHE_NAME, "member-data");
        List<String> memberData = gson.fromJson(jsonData, new TypeToken<List<String>>() {
        }.getType());

        if (memberData == null) return Collections.emptyList();

        return memberData
                .parallelStream()
                .map(json -> gson.fromJson(json, MemberWrapper.class))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MemberWrapper> getMemberDetailsById(long memberId) {
        List<MemberWrapper> memberList = getGroupMembers();
        return memberList.stream()
                .filter(member -> member.getId() == memberId)
                .findFirst();
    }

    public Optional<MemberWrapper> getGroupMemberDetails(long id) {
        return getGroupMembers()
                .parallelStream()
                .filter(m -> m.getId() == id && m.isActive())
                .findFirst();
    }

    @Override
    public Flux<MemberWrapper> getFluxMembers() {
        return hashOperations.get(CACHE_NAME, "member-data")
                .flatMapIterable(memberDataJson -> {
                    List<String> memberData = gson.fromJson(memberDataJson, new TypeToken<List<String>>() {
                    }.getType());

                    return memberData
                            .stream()
                            .map(json -> gson.fromJson(json, MemberWrapper.class))
                            .collect(Collectors.toList());
                });
    }

    @Override
    public List<MemberWrapper> getMembers() {
        String membersData = syncHashOperations.get(CACHE_NAME, "member-data");

        List<String> memberData = gson.fromJson(membersData, new TypeToken<List<String>>() {
        }.getType());

        if (memberData == null) return new ArrayList<>();

        return memberData
                .stream()
                .map(json -> gson.fromJson(json, MemberWrapper.class))
                .collect(Collectors.toList());
    }

    @Override
    public Flux<GroupWrapper> getFluxGroups() {
        return hashOperations.get(CACHE_NAME, "group-data")
                .flatMapIterable(groupDataJson -> {
                    List<String> groupData = gson.fromJson(groupDataJson, new TypeToken<List<String>>() {
                    }.getType());

                    return groupData
                            .stream()
                            .map(String.class::cast)
                            .map(json -> gson.fromJson(json, GroupWrapper.class))
                            .collect(Collectors.toList());
                });
    }

    @Override
    public List<GroupWrapper> getGroups() {
        String groupsData = syncHashOperations.get(CACHE_NAME, "group-data");

        List<String> groupData = gson.fromJson(groupsData, new TypeToken<List<String>>() {
        }.getType());

        if (groupData == null) return new ArrayList<>();

        return groupData
                .stream()
                .map(String.class::cast)
                .map(json -> gson.fromJson(json, GroupWrapper.class))
                .collect(Collectors.toList());
    }

    @Override
    public Flux<GroupMemberWrapper> getFluxGroupMembers() {
        return hashOperations.get(CACHE_NAME, "group-members")
                .flatMapIterable(groupMembershipData -> {
                    List<String> groupMembership = gson.fromJson(groupMembershipData, new TypeToken<List<String>>() {
                    }.getType());

                    return groupMembership
                            .stream()
                            .map(json -> gson.fromJson(json, GroupMemberWrapper.class))
                            .collect(Collectors.toList());
                });
    }

    @Override
    public String getMonoMemberGroupNameById(long memberGroupId) {
        return getFluxGroupMembers()
                .filter(memberGroup -> memberGroup.getGroupId() == memberGroupId)
                .map(GroupMemberWrapper::getGroupName)
                .blockFirst();
    }

    @Override
    public MemberWrapper searchMonoMemberByPhoneNumber(String phoneNumber) {
        return getFluxMembers()
                .filter(member -> member.getPhonenumber().trim().equalsIgnoreCase(phoneNumber.trim()))
                .blockFirst();
    }

    @Override
    public Optional<MemberWrapper> searchMemberByPhoneNumber(String phoneNumber) {
        return getMembers()
                .parallelStream()
                .filter(member -> member.getPhonenumber().trim().equalsIgnoreCase(phoneNumber.trim()))
                .findFirst();
    }

    @Override
    public Optional<MemberWrapper> searchMemberByUserId(long userId) {
        return getMembers()
                .parallelStream()
                .filter(member -> member.getId() == userId)
                .findFirst();
    }

    @Override
    public String getMonoGroupNameByGroupId(long id) {
        return getFluxGroups()
                .filter(group -> group.getId() == id)
                .map(GroupWrapper::getName)
                .blockFirst();
    }

    @Override
    public Optional<String> getGroupNameByGroupId(long id) {
        List<GroupWrapper> groups = getGroups();
        return groups
                .parallelStream()
                .filter(group -> group.getId() == id)
                .map(GroupWrapper::getName)
                .findFirst();
    }

    @Override
    public Optional<GroupWrapper> getGroupById(long id) {
        List<GroupWrapper> groups = getGroups();
        return groups
                .parallelStream()
                .filter(group -> group.getId() == id)
                .findFirst();
    }

    @Override
    public Flux<GroupWrapper> findMonoGroupsCreatedBetweenOrderAsc(Date startDate, Date endDate) {
        return getFluxGroups()
                .filter(group -> group.getCreatedOn().after(startDate) && group.getCreatedOn().before(endDate))
                .sort();
    }

    @Override
    public GroupWrapper findMonoGroupByActiveAndSoftDeleteAndNameLike(boolean active, boolean softDelete, String groupName) {
        return getFluxGroups()
                .filter(group -> group.isActive() == active)
                .filter(group -> group.isDeleted() == softDelete)
                .filter(group -> group.getName().trim().equalsIgnoreCase(groupName.trim()))
                .blockFirst();
    }

    @Override
    public MemberWrapper getMonoMemberDetailsById(long memberId) {
        return getFluxMembers()
                .filter(memberWrapper -> memberWrapper.getId() == memberId)
                .blockFirst();
    }

    @Override
    public GroupWrapper getMonoGroupById(long groupId) {
        return getGroups()
                .parallelStream()
                .filter(group -> group.getId() == groupId)
                .findFirst()
                .orElse(null);
    }

    public Mono<GroupWrapper> getGroup(long groupId) {
        return getFluxGroups()
                .filter(group -> {
                    log.info("Finding group: {}", group.getId() == groupId);
                    return group.getId() == groupId;
                }).take(1).single();
    }

    @Override
    public Flux<GroupWrapper> findFluxGroupsByActiveAndSoftDeleteAndCreatedOnBetween(boolean status, boolean softDelete, Date startDate, Date endDate, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int skipCount = (page - 1) * size;
        return getFluxGroups()
                .filter(group -> group.isActive() == status && group.isDeleted() == softDelete)
                .filter(group -> group.getCreatedOn().after(startDate) && group.getCreatedOn().before(endDate))
                .skip(skipCount)
                .take(size);
    }

    public Flux<MemberWrapper> findFluxByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean softDelete, boolean status) {
        return getFluxMembers()
                .filter(member -> member.isActive() == status && member.isSoftDelete() == softDelete
                        && member.getCreatedOn().after(startDate) && member.getCreatedOn().before(endDate));
    }

    @Override
    public GroupMemberWrapper getMonoGroupMembershipByGroupIdAndMemberId(long groupId, long memberId) {
        return getFluxGroupMembers()
                .filter(memberGroup -> memberGroup.getGroupId() == groupId && memberGroup.getMemberId() == memberId)
                .blockFirst();
    }

    @Override
    public GroupWrapper getMonoGroupByName(String groupName) {
        return getFluxGroups()
                .filter(group -> group.getName().trim().equalsIgnoreCase(groupName.trim()))
                .blockFirst();
    }

    @Override
    public Optional<String> getMemberPermission(int groupId, String phoneNumber) {
        String membershipDataJson = syncHashOperations.get(CACHE_NAME, "group-members");

        List<String> membershipData = gson.fromJson(membershipDataJson, new TypeToken<List<String>>() {
        }.getType());

        if (membershipData == null) return Optional.empty();

        return membershipData
                .stream()
                .map(group -> gson.fromJson(group, GroupMemberWrapper.class))
                .filter(membership -> membership.getGroupId() == groupId && Objects.equals(membership.getPhoneNumber(), phoneNumber))
                .map(GroupMemberWrapper::getPermissions)
                .findFirst();
    }

    @Override
    public Flux<String> getFluxMembersPhonesInGroup(long groupId) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == groupId)
                .map(GroupMemberWrapper::getPhoneNumber);
    }

    @Override
    public Flux<Pair<String, String>> getFluxMembersLanguageAndPhonesInGroup(long groupId) {
        Flux<GroupMemberWrapper> fluxGroupMembers = findFluxByGroupsAndActiveMembership(groupId, true);

        return fluxGroupMembers
                .flatMapSequential(gm -> {
                    Optional<MemberWrapper> memberWrapper = getGroupMemberDetails(gm.getMemberId());
                    if (memberWrapper.isEmpty()) return Mono.just(Pair.of(gm.getPhoneNumber(), "Kiswahili"));

                    return Mono.just(Pair.of(gm.getPhoneNumber(), memberWrapper.get().getLanguage()));
                });
    }

    public Flux<GroupMemberWrapper> findFluxByGroupsAndActiveMembership(long memberGroupId, boolean active) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == memberGroupId && gm.isActivemembership() == active);
    }

    @Override
    public Flux<MemberWrapper> getFluxGroupMembers(long memberGroupId) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == memberGroupId)
                .map(GroupMemberWrapper::getMemberId)
                .map(this::getMemberDetailsById)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Flux<MemberWrapper> getGroupOfficials(long memberGroupId) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == memberGroupId && !gm.getTitle().equalsIgnoreCase("member"))
                .map(GroupMemberWrapper::getMemberId)
                .map(this::getMemberDetailsById)
                .filter(Optional::isPresent)
                .map(Optional::get);
    }

    @Override
    public Flux<GroupWrapper> getFluxGroupsMemberBelongs(String username) {
        return getFluxGroupMembers()
                .filter(gm -> {
                    log.info("Group membership -> {}  +  eval {}", gm.getGroupId(), gm.getPhoneNumber().equals(username));
                    return gm.getPhoneNumber().equals(username);
                })
                .flatMap(gm -> {
                    log.info("Group membership -> {}", gm.getGroupId());
                    return getGroup(gm.getGroupId());
                })
                .filter(Objects::nonNull);
    }

    @Override
    public GroupMemberWrapper memberIsPartOfGroup(long groupId, String username) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == groupId && Objects.equals(gm.getPhoneNumber(), username) && gm.isActivemembership())
                .blockFirst();
    }

    @Override
    public GroupWrapper findGroupsByActiveAndSoftDeleteAndName(boolean active, boolean deleted, String group) {
        return getFluxGroups()
                .filter(g -> g.isActive() && !g.isDeleted() && g.getName().contains(group))
                .blockFirst();
    }

    @Override
    public MemberWrapper searchMonoMemberByCoreAccount(String account) {
        return getFluxMembers()
                .filter(m -> Objects.nonNull(m.getLinkedAccounts()))
                .filter(m -> Arrays.asList(m.getLinkedAccounts().split(",")).contains(account))
                .blockFirst();
    }

    @Override
    public Flux<MemberWrapper> findAllByCreatedOnBetweenAndSoftDeleteAndActive(Date startDate, Date endDate, boolean deleted, boolean status) {
        return getFluxMembers()
                .filter(m -> m.getCreatedOn().after(startDate) && m.getCreatedOn().before(endDate)
                        && m.isSoftDelete() == deleted && m.isActive() == status);
    }

    @Override
    public Flux<GroupMemberWrapper> findAllByGroupsAndActiveMembership(long memberGroupId, boolean active) {
        return getFluxGroupMembers()
                .filter(gm -> gm.getGroupId() == memberGroupId && gm.isActivemembership() == active);
    }

}
