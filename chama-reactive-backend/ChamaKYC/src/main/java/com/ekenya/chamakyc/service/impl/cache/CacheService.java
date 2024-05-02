package com.ekenya.chamakyc.service.impl.cache;

import com.ekenya.chamakyc.repository.chama.GroupMembersRepository;
import com.ekenya.chamakyc.repository.chama.GroupRepository;
import com.ekenya.chamakyc.repository.chama.MemberRepository;
import com.ekenya.chamakyc.service.impl.functions.MapperFunction;
import com.ekenya.chamakyc.wrappers.broker.GroupMemberWrapper;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Alex Maina
 * @created 20/03/2022
 **/
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {
        private final MapperFunction mapperFunction;
        private final GroupRepository groupRepository;
        private final MemberRepository memberRepository;
        private final ReactiveStringRedisTemplate redisOperations;
        private final StringRedisTemplate stringRedisTemplate;
        private final GroupMembersRepository groupMembersRepository;
        private ReactiveHashOperations<String, String, String> hashOperations;
        private HashOperations<String, String, String> syncHashOperations;

        @PostConstruct
        private void init() {
                hashOperations = redisOperations.opsForHash();
                syncHashOperations = stringRedisTemplate.opsForHash();
        }

        private final Gson gson = new Gson();
        private static final String CACHE_NAME = "chama-cache";

        public Optional<String> getMemberPermission(long groupId, String phoneNumber) {
                String memberData = syncHashOperations.get(CACHE_NAME, "group-members");
                List<String> membershipData = gson.fromJson(memberData, new TypeToken<List<String>>() {
                }.getType());

                if (membershipData == null)
                        return Optional.empty();

                return membershipData.stream()
                                .map(res -> gson.fromJson(res, GroupMemberWrapper.class))
                                .filter(membership -> membership.getGroupId() == groupId
                                                && Objects.equals(membership.getPhoneNumber(), phoneNumber))
                                .map(GroupMemberWrapper::getPermissions)
                                .findFirst();
        }

        @Scheduled(fixedDelay = 30000)
        @SchedulerLock(name = "publishGroupInfoToRedis", lockAtMostFor = "5m")
        public void publishGroupInfoToRedis() {
                Mono.fromRunnable(() -> {
                        List<String> groupData = groupRepository.findAll()
                                        .stream()
                                        .filter(group -> !group.isSoftDelete())
                                        .map(mapperFunction.mapGroupToGroupWrapper())
                                        .map(gson::toJson)
                                        .collect(Collectors.toList());
                        if (!groupData.isEmpty()){
                                hashOperations.put(CACHE_NAME, "group-data", gson.toJson(groupData))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                        }

                }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }

        @Scheduled(fixedDelay = 20000)
        @SchedulerLock(name = "publishGroupMembershipInfoToRedis", lockAtMostFor = "3m")
        public void publishGroupMembershipInfoToRedis() {
                Mono.fromRunnable(() -> {
                        List<String> groupMembership = groupMembersRepository
                                        .findAll()
                                        .stream()
                                        .filter(gm -> !gm.getGroup().isSoftDelete())
                                        .map(mapperFunction.mapMemberGroupToGroupMemberWrapper())
                                        .map(gson::toJson)
                                        .collect(Collectors.toList());
                        if (!groupMembership.isEmpty()){
                                hashOperations.put(CACHE_NAME, "group-members", gson.toJson(groupMembership))
                                        .subscribeOn(Schedulers.boundedElastic())
                                        .subscribe();
                        }

                }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }

        @Scheduled(fixedDelay = 35000)
        @SchedulerLock(name = "publishMemberInfoToRedis", lockAtMostFor = "3m")
        public void publishMemberInfoToRedis() {
                Mono.fromRunnable(() -> {
                                List<String> members = memberRepository.findAll()
                                        .stream()
                                        .map(mapperFunction.mapMemberToMemberWrappers())
                                        .map(gson::toJson)
                                        .collect(Collectors.toList());

                                if (!members.isEmpty()){
                                        hashOperations.put(CACHE_NAME, "member-data", gson.toJson(members))
                                                .subscribeOn(Schedulers.boundedElastic())
                                                .subscribe();
                                }


                }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
        }



}
