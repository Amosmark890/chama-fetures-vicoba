package com.ekenya.chamakyc.service.impl;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.repository.chama.GroupRepository;
import com.ekenya.chamakyc.repository.chama.MemberRepository;
import com.ekenya.chamakyc.repository.users.UserRepository;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.DashboardValuesService;
import com.ekenya.chamakyc.service.Interfaces.FileHandlerService;
import com.ekenya.chamakyc.service.impl.functions.MapperFunction;
import com.ekenya.chamakyc.wrappers.SearchDetails;
import com.ekenya.chamakyc.wrappers.broker.GroupReportWrapper;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.response.GroupSearchResponse;
import com.ekenya.chamakyc.wrappers.response.UsersDetails;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardValuesService {

    private final Gson gson;
    private WebClient webClient;
    private final GroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final FileHandlerService fileHandlerService;
    private final UserRepository userRepository;
    private final MapperFunction mapperFunction;

    private final ChamaGroupService chamaGroupService;


    @Value("${base.services-url}")
    private String baseUrl;

    @PostConstruct
    private void init() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    static Map<String, TemporalAdjuster> timeAdjusters() {
        Map<String, TemporalAdjuster> adjusterHashMap = new HashMap<>();
        adjusterHashMap.put("days", TemporalAdjusters.ofDateAdjuster(d -> d)); // identity
        adjusterHashMap.put("weeks", TemporalAdjusters.previousOrSame(DayOfWeek.of(1)));
        adjusterHashMap.put("months", TemporalAdjusters.firstDayOfMonth());
        adjusterHashMap.put("years", TemporalAdjusters.firstDayOfYear());
        return adjusterHashMap;
    }

    static Comparator<Map<Object, Object>> mapComparator() {
        return new Comparator<Map<Object, Object>>() {
            @SneakyThrows
            public int compare(Map<Object, Object> m1, Map<Object, Object> m2) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                Date date1 = sdf.parse(m1.get("dateofday").toString());
                Date date2 = sdf.parse(m2.get("dateofday").toString());
                return date1.compareTo(date2);
            }
        };
    }

    @Override
    public List<Map<Object, Object>> groupRegistrationTrend(Date startDate, Date endDate, String period, String groupName, String country) {
        List<Map<Object, Object>> response = new ArrayList<>();
        List<Map<Object, Object>> groupsResponse = new ArrayList<>();
        List<Map<Object, Object>> membersResponse = new ArrayList<>();
        List<Group> groupsList = groupRepository.findAllByCreatedOnBetweenOrderByCreatedOnAsc(startDate, endDate);
        List<Member> membersList = memberRepository.findAllByCreatedOnBetweenOrderByCreatedOnAsc(startDate, endDate);
        groupName = groupName.trim();
        if (!groupName.trim().equalsIgnoreCase("all")) {
            membersList = memberRepository.findMembersByGroupAndCreatedOnBetweenAndOrderByAsc(groupName, startDate, endDate);
        }

        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        Map<Object, Long> groupsData = groupsList.parallelStream()
                .collect(Collectors.groupingBy(group -> formatter.format(group.getCreatedOn()), Collectors.counting()));
        groupsData.forEach((key, value) -> groupsResponse.add(Map.of("dateofday", key, "objects", value)));

        Map<Object, Long> membersData = membersList.parallelStream()
                .collect(Collectors.groupingBy(member -> formatter.format(member.getCreatedOn()), Collectors.counting()));
        membersData.forEach((key, value) -> membersResponse.add(Map.of("dateofday", key, "objects", value)));

        if (groupName.trim().equalsIgnoreCase("all")) {
            response.add(Map.of("groups", groupsResponse));
        } else {
            response.add(Map.of("groups", Collections.emptyList()));
        }
        response.add(Map.of("individuals", membersResponse));
        return response;
    }

    @Override
    public Mono<UniversalResponse> groupRegistrationTrends(Date startDate, Date endDate, String period, String groupName, String country) {
        return Mono.fromCallable(() -> groupRegistrationTrend(startDate, endDate, period, groupName, country))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", "group registration trend", res));
    }

    @Override
    public Mono<UniversalResponse> getPortalGroupValues() {
        return Mono.fromCallable(() -> {
            // number of groups
            long totalGroups = groupRepository.countAllBySoftDeleteFalse();
            // count active groups
            long activeGroups = groupRepository.countByActiveAndSoftDeleteFalse(true);
            // count inactive groups
            long inactiveGroups = groupRepository.countByActiveAndSoftDeleteFalse(false);
            // count all users
            long memberCount = memberRepository.count();
            //count active chama users
            long activeMemberCount = memberRepository.countByActiveAndSoftDeleteFalse(true);
            // count inactive chama users
            long inactiveMemberCount = memberRepository.countByActiveAndSoftDeleteFalse(false);

            String accountingInfo = webClient.get()
                    .uri("/portal/payments/dash/accounting")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> map = gson.fromJson(accountingInfo, Map.class);
            map.put("totalgroups", totalGroups);
            map.put("inactivegroups", inactiveGroups);
            map.put("activegroups", activeGroups);
            map.put("totalmembers", memberCount);
            map.put("activemembercount", activeMemberCount);
            map.put("inactivemembercount", inactiveMemberCount);

            return new UniversalResponse("success", "group dash data", map);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<Group> groupsList = groupRepository.findGroupsByActiveAndSoftDeleteAndCreatedOnBetween(status, false, startDate, endDate, pageable);

            Map<String, List<GroupReportWrapper>> groupsData = groupsList.parallelStream()
                    .map(group -> GroupReportWrapper.builder()
                            .groupId(group.getId())
                            .name(group.getName())
                            .location(group.getLocation())
                            .description(group.getDescription())
                            .isActive(group.isActive())
                            .createdBy(String.format(" %s %s", group.getCreator().getUsers().getFirstName(), group.getCreator().getUsers().getLastName()))
                            .creatorPhone(group.getCreator().getImsi())
                            .hasWallet(group.isWalletexists())
                            .groupImage(fileHandlerService.getFileUrl(group.getGroupImageUrl()))
                            .purpose(group.getPurpose())
                            .createdOn(group.getCreatedOn())
                            .build())
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

            List<Map<Object, Object>> groupsResponse = new ArrayList<>();
            groupsData.forEach((key, value) -> groupsResponse.add(Map.of("dateofday", key, "objects", value)));
            groupsResponse.sort(mapComparator());
            return UniversalResponse.builder()
                    .status("success")
                    .message(String.format("Groups list by status %s", status ? "active" : "inactive"))
                    .data(groupsResponse)
                    .timestamp(new Date())
                    .metadata(Map.of("numofrecords", groupsList.size()))
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getMemberQueryByType(Date startDate, Date endDate, String period, boolean status, String group, Pageable pageable) {
        return Mono.fromCallable(() -> {
            List<Member> membersList = memberRepository.findAllByCreatedOnBetweenAndSoftDeleteAndActive(startDate, endDate, false, status);
            if (!group.equals("all")) {
                Group groups = groupRepository.findGroupByActiveAndSoftDeleteAndNameLike(true, false, group).orElse(null);
                if (groups == null) {
                    return new UniversalResponse("fail", String.format("Group search with name %s failed", group), new ArrayList<>());
                } else {
                    membersList = memberRepository.findMemberByGroupNameAndCreatedOnBetweenAndActiveAndSoftDeleteOrderByAsc(group, startDate, endDate, status, false);
                }
            }

            Map<String, List<Member>> membersData = membersList.stream()
                    .collect(Collectors.groupingBy((t -> t.getCreatedOn().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(timeAdjusters().get(period)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))));

            List<Map<Object, Object>> membersResponse = new ArrayList<>();
            membersData.forEach((key, value) -> membersResponse.add(Map.of("dateofday", key, "objects", value)));
            membersResponse.sort(mapComparator());
            return new UniversalResponse("success", String.format("members list by status %s", status ? "active" : "inactive"), membersResponse);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> searchForAppUsers(String firstName, String otherNames, String gender, String email, String phoneNumber, String status, int page, int size) {
        return Mono.fromCallable(() -> {
            SearchDetails searchDetails = userRepository.searchAppUser(firstName, otherNames, email, phoneNumber, status, gender, page, size);
            List<UsersDetails> usersDetails = getUsersDetails(searchDetails);
            return UniversalResponse.builder()
                    .status("success")
                    .message("App users")
                    .data(usersDetails)
                    .metadata(Map.of("numofrecords", searchDetails.getTotalCount()))
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @NonNull
    private List<UsersDetails> getUsersDetails(SearchDetails searchDetails) {
        Type gsonType = new TypeToken<List<UsersDetails>>() {
        }.getType();

        return gson.fromJson(searchDetails.getResData(), gsonType);
    }

    @Override
    public Mono<UniversalResponse> searchForPortalUsers(String firstName, String otherNames, String gender, String email, String phoneNumber, String status, int page, int size) {
        return Mono.fromCallable(() -> {
            SearchDetails searchDetails = userRepository.searchPortalUser(firstName, otherNames, email, phoneNumber, status, gender, page, size);

            List<UsersDetails> usersDetails = getUsersDetails(searchDetails);
            return UniversalResponse.builder()
                    .status("success")
                    .message("Portal users")
                    .data(usersDetails)
                    .metadata(Map.of("numofrecords", searchDetails.getTotalCount()))
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> searchForGroup(String groupName, String createdBy, String creatorPhone, String cbsAccount, String status, String createdOnStart, String createdOnEnd, int page, int size) {
        return Mono.fromCallable(() -> {
            SearchDetails searchDetails = groupRepository.searchGroup(groupName, createdBy, creatorPhone, cbsAccount, status, createdOnStart, createdOnEnd, page, size);
            Type gsonType = new TypeToken<List<GroupSearchResponse>>() {
            }.getType();

            List<GroupSearchResponse> groupList = gson.fromJson(searchDetails.getResData(), gsonType);

            return UniversalResponse.builder()
                    .status("success")
                    .message("Groups in search")
                    .data(groupList)
                    .metadata(Map.of("numofrecords", searchDetails.getTotalCount()))
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }
}
