package com.ekenya.chamakyc.service.impl;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.dao.chama.*;
import com.ekenya.chamakyc.dao.config.MessageTemplates;
import com.ekenya.chamakyc.dao.error.FailedOperations;
import com.ekenya.chamakyc.dao.error.Operation;
import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.exception.GroupNotFoundException;
import com.ekenya.chamakyc.exception.MemberNotFoundException;
import com.ekenya.chamakyc.repository.chama.*;
import com.ekenya.chamakyc.repository.config.CountryRepository;
import com.ekenya.chamakyc.repository.config.MessagetemplatesRepo;
import com.ekenya.chamakyc.repository.error.FailedOperationsRepository;
import com.ekenya.chamakyc.repository.users.UserRepository;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.FileHandlerService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.service.impl.constants.PositionName;
import com.ekenya.chamakyc.service.impl.constants.SMS_TYPES;
import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
import com.ekenya.chamakyc.service.impl.functions.MapperFunction;
import com.ekenya.chamakyc.wrappers.broker.*;
import com.ekenya.chamakyc.wrappers.request.*;
import com.ekenya.chamakyc.wrappers.response.CountryInfo;
import com.github.benmanes.caffeine.cache.Cache;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.ekenya.chamakyc.util.Utils.mapToRequestBody;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChamaGroupServiceImpl implements ChamaGroupService {
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final GroupRepository groupRepository;
    private final PublishingService publishingService;
    private final GroupMembersRepository groupMemberRepo;
    private final GroupTitlesRepository groupTitleRepo;
    private final Gson gson;
    private final FileHandlerService fileHandlerService;
    private final GroupDocumentsRepository groupDocumentsRepository;
    private WebClient webClient;
    private final GroupInvitesRepository groupInvitesRepository;
    private final GroupCategoryRepository groupCategoryRepository;
    private final FailedOperationsRepository failedOperationsRepository;
    private final MapperFunction mapperFunction;
    private final MessagetemplatesRepo messagetemplatesRepo;
    private final NotificationServiceImpl notificationService;
    private final CountryRepository countryRepository;
    private final ResourceBundleMessageSource source;

    @Value("${vicoba.url}")
    private String vicobaUrl;
    private WebClient vicobaWebClient;
    @Value("${base.services-url}")
    private String baseUrl;
    private static final String DEFAULT_ACCOUNT = "000000000000000";

    /**
     * Initialize the Webclient with the Vicoba url
     * Initialize the gateway webclient
     */
    @PostConstruct
    public void init() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create().secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        vicobaWebClient = WebClient.builder()
                .baseUrl(vicobaUrl)
                .build();
        webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(baseUrl)
                .build();
    }

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    /**
     * Check if a Group exists locally and in the DCB system.
     *
     * @param account the CBS account
     * @param name    the creator's name
     * @return a status, message and any other data as required
     */
    @Override
    public Mono<UniversalResponse> groupAccountLookup(String account, String name) {
        return Mono.fromCallable(() -> {
            if (!account.equals(DEFAULT_ACCOUNT)) {
                Optional<Group> groupOptional = groupRepository.findByCbsAccount(account);
                if (groupOptional.isPresent()) {
                    Group group = groupOptional.get();
                    VicobaGroupRequest vicobaGroupRequest = VicobaGroupRequest.builder()
                            .accountName(group.getCbsAccountName())
                            .groupName(group.getName())
                            .active(group.isActive())
                            .groupId(group.getId())
                            .exists(true)
                            .build();

                    return new UniversalResponse("success", getResponseMessage("groupLookup"), vicobaGroupRequest);
                }
            }

            return groupAccountLookupOnCBS(account);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public UniversalResponse groupAccountLookupOnCBS(String account) {
        Map<String, String> groupLookup = mapToRequestBody("groupLookup", account);

        String body = new Gson().toJson(groupLookup);

        VicobaGroupRequest groupRequest = VicobaGroupRequest.builder()
                .exists(false)
                .availableBalance("0.0")
                .accountName("Pseudo Account")
                .isActive("N")
                .actualBalance("0.0")
                .groupName("Pseudo Name")
                .build();
        return vicobaWebClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .doOnNext(res -> log.info("Core Account Lookup::: {}", res))
                .map(res -> {
                    JsonObject jsonObject = gson.fromJson(res, JsonObject.class);
                    if (jsonObject.get("48").getAsString().equals("Successful")) {
                        VicobaGroupRequest vicobaGroupRequest = new Gson().fromJson(jsonObject.get("54").getAsJsonObject(), VicobaGroupRequest.class);
                        vicobaGroupRequest.setExists(false);
                        return new UniversalResponse("success", getResponseMessage("groupLookup"), vicobaGroupRequest);
                    }

                    return new UniversalResponse("fail", jsonObject.get("54").getAsString());
                })
                .onErrorReturn(new UniversalResponse("fail", getResponseMessage("pseudoAccountDesc"), groupRequest))
                .block();
    }

    /**
     * Save existing Vicoba Group
     *
     * @param vicobaGroupRequest has Group information
     * @param account            the CBS account
     * @param creator            the user that initiates the request to look up
     */
    public void saveGroup(VicobaGroupRequest vicobaGroupRequest, String account, String creator) {
        Optional<Member> member = findChamaMemberByUserPhone(creator);
        if (member.isEmpty()) return;

        String memberName = String.format("%s %s", member.get().getUsers().getFirstName(), member.get().getUsers().getLastName());
        Group group = new Group();
        group.setName(vicobaGroupRequest.getAccountName());
        group.setCbsAccount(account);
        group.setWalletexists(true);
        group.setCreator(member.get());
        String groupStatus = vicobaGroupRequest.getIsActive();
        group.setActive(groupStatus.equals("Y"));

        Group savedGroup = groupRepository.save(group);
        addGroupMember(savedGroup.getCreator(), savedGroup, "chairperson");
        addAccount(savedGroup.getId(), group.getName(), account, Double.parseDouble(vicobaGroupRequest.getAvailableBalance()));
        createContribution(savedGroup.getId(), savedGroup.getName(), memberName, vicobaGroupRequest.getAvailableBalance());
    }

    /**
     * Send event to create a default Contribution.
     *
     * @param id               the group id
     * @param name             the group name
     * @param memberName       the name of the group creator
     * @param availableBalance
     */
    private void createContribution(long id, String name, String memberName, String availableBalance) {
        ChamaContribution contribution = ChamaContribution.builder()
                .contributionname(name)
                .startdate(new Date())
                .contributiondetails(new HashMap<>())
                .contributiontypeid(1)
                .scheduletypeid(3)
                .groupid(id)
                .amounttypeid(2)
                .penalty(0L)
                .reminders(0)
                .initialAmount(availableBalance)
                .createdby(memberName)
                .build();

        log.info("Sending contribution creation details...");
        publishingService.createGroupContribution(contribution);
    }

    /**
     * Send event to create a group account.
     *
     * @param id               the group id
     * @param name             the name of the group
     * @param availableBalance the available balance in the group
     */
    private void addAccount(long id, String name, String accountNumber, double availableBalance) {
        log.info("Adding group account...");
        publishingService.createGroupAccount(id, name, accountNumber, availableBalance);
    }

    /**
     * Creates Group
     *
     * @param groupname
     * @param location
     * @param description
     * @param createdby_account
     * @param categoryId
     * @param purpose
     * @param filepart
     * @return
     */
    @Override
    public Mono<UniversalResponse> createGroup(String groupname, String location, String description, String createdby_account, Long categoryId, String purpose, FilePart filepart) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findChamaMemberByUserPhone(createdby_account);
                    if (optionalMember.isEmpty())
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("memberNotFound")).build();
                    Member creator = optionalMember.get();
                    //check if user has reached max group image
                    var groupMax = groupRepository.countAllByCreator(creator);
                    if (groupMax > 3L) {
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("groupCreationLimit")).build();
                    }
                    Optional<Group> optionalGroup = groupRepository.findByCreatorAndNameLike(creator, groupname);
                    if (optionalGroup.isPresent())
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("groupDuplicateDetailsDetected")).build();
                    Group group = Group.builder()
                            .name(groupname)
                            .description(description)
                            .active(true)
                            .creator(creator)
                            .location(location)
                            .categoryId(categoryId)
                            .build();
                    String filetype = Objects.requireNonNull(filepart.headers().getContentType()).getType().split("/")[0];
                    if (filetype.equals("image")) {
                        String urlPath = fileHandlerService.uploadFile(filepart);
                        group.setGroupImageUrl(urlPath);
                    }
                    Group savedGroup = groupRepository.save(group);
                    ChamaContribution contribution = ChamaContribution.builder()
                            .contributionname(groupname)
                            .startdate(new Date())
                            .contributiondetails(new HashMap<>())
                            .contributiontypeid(1)
                            .scheduletypeid(1)
                            .groupid(savedGroup.getId())
                            .amounttypeid(1)
                            .penalty(0L)
                            .reminders(0)
                            .createdby(createdby_account)
                            .build();
                    RegisterGroupEsb registerReq = RegisterGroupEsb.builder().groupId(group.getId()).groupName(group.getName()).build();
                    String creatorFullName = String.format("%s %s", creator.getUsers().getFirstName(), creator.getUsers().getLastName());
//                    publishingService.createChamaContribution().apply(savedGroup, creatorFullName);
//                    publishingService.registerGroupWallet().apply(savedGroup.getId(), savedGroup.getName());
                    publishingService.createGroupContribution(contribution);
                    addGroupMember(creator, group, "creator");
                    UniversalResponse response = new UniversalResponse("success", getResponseMessage("groupCreatedSuccessfully"), Collections.emptyList());
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("groupid", group.getId());
                    response.setMetadata(metadata);
                    return response;
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> createGroup(GroupDetailsWrapper groupDetailsWrapper, String createdBy) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(createdBy);
            if (optionalMember.isEmpty())
                return UniversalResponse.builder().status("fail").message(getResponseMessage("memberNotFound")).build();
            Member creator = optionalMember.get();
            //check if user has reached max group image
            var groupMax = groupRepository.countAllByCreator(creator);
            if (groupMax > 10L)
                return new UniversalResponse("fail", getResponseMessage("groupCreationLimit"));

            if (groupDetailsWrapper.getCbsAccount() == null || groupDetailsWrapper.getCbsAccount().isBlank())
                groupDetailsWrapper.setCbsAccount(null);

            if (Objects.equals(groupDetailsWrapper.getSecretaryPhoneNumber(), createdBy) || Objects.equals(groupDetailsWrapper.getTreasurerPhoneNumber(), createdBy))
                return new UniversalResponse("fail", getResponseMessage("creatorCannotSecretaryOrTreasurer"));

            if (Objects.equals(groupDetailsWrapper.getSecretaryPhoneNumber(), groupDetailsWrapper.getTreasurerPhoneNumber()))
                return new UniversalResponse("fail", getResponseMessage("treasurerCannotBeSecretary"));

            Optional<Group> optionalGroup = groupRepository.findByNameOrCbsAccount(groupDetailsWrapper.getGroupname(), groupDetailsWrapper.getCbsAccount());
            if (optionalGroup.isPresent()) {
                return new UniversalResponse("fail", getResponseMessage("groupDuplicateDetailsDetected"));
            }

            Group group = Group.builder()
                    .name(groupDetailsWrapper.getGroupname())
                    .description(groupDetailsWrapper.getDescription())
                    .active(false)
                    .cbsAccount(groupDetailsWrapper.getCbsAccount() == null || groupDetailsWrapper.getCbsAccount().isBlank() ? DEFAULT_ACCOUNT : groupDetailsWrapper.getCbsAccount())
                    .cbsAccountName(groupDetailsWrapper.getCbsAccountName())
                    .creator(creator)
                    .categoryId(groupDetailsWrapper.getCategoryId())
                    .build();

            Group savedGroup = groupRepository.save(group);

            createGroupUtilsAndSendInvites(groupDetailsWrapper, creator, group, savedGroup);

            Optional<GroupMembership> groupMembership = groupMemberRepo.findByMembersAndGroup(creator, savedGroup);
            return groupMembership.map(membership -> {
                        Map permissions = gson.fromJson(membership.getPermissions(), Map.class);
                        Map<String, Object> data = Map.of("groupid", savedGroup.getId(), "isActive", group.isActive(), "roles", permissions);
                        return new UniversalResponse("success", getResponseMessage("groupCreatedSuccessfully"), data);
                    })
                    .orElseGet(() -> {
                        Map<String, Object> data = Map.of("groupid", savedGroup.getId(), "isActive", group.isActive(), "roles", Collections.emptyMap());
                        return new UniversalResponse("success", getResponseMessage("groupCreatedSuccessfully"), data);
                    });
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void createGroupUtilsAndSendInvites(GroupDetailsWrapper groupDetailsWrapper, Member creator, Group group, Group savedGroup) {
        log.info("Sending group invites and creating group utils...");
        addGroupMember(creator, savedGroup, "chairperson");
        double initialAmount;
        try {
             initialAmount = Double.parseDouble(groupDetailsWrapper.getAvailableBalance());
        } catch (Exception e) {
            initialAmount = 0.0;
        }
        addAccount(savedGroup.getId(), group.getName(), groupDetailsWrapper.getCbsAccount(), initialAmount);
        String memberName = String.format("%s %s", creator.getUsers().getFirstName(), creator.getUsers().getLastName());
        createContribution(savedGroup.getId(), savedGroup.getName(), memberName, groupDetailsWrapper.getAvailableBalance());
        // send invites to officials
        sendOfficialsInvite(
                savedGroup.getId(),
                List.of(new MemberRoles(groupDetailsWrapper.getSecretaryPhoneNumber(), "Secretary"),
                        new MemberRoles(groupDetailsWrapper.getTreasurerPhoneNumber(), "Treasurer")))
                .subscribe(universalResponse -> log.info(universalResponse.getMessage()));
    }

    public Optional<Member> findChamaMemberByUserPhone(String phone) {
        Users users = userRepository.findByPhoneNumberAndChannel(phone, Channel.APP.name()).orElse(null);
        if (users == null) return Optional.empty();
        return memberRepository.findByUserId(users.getId());
    }

    @Override
    public Mono<UniversalResponse> sendGroupInvites(long groupid, List<MemberRoles> phonenumbersandrole) {
        return Mono.fromCallable(() -> {
                    Optional<Group> optionalGroup = groupRepository.findById(groupid);
                    if (optionalGroup.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("groupNotFound"));
                    }
                    List<GroupMembership> groupMemberships = groupMemberRepo.findByGroup(optionalGroup.get());
                    // check if there exists a secretary and treasurer
                    long officials = groupMemberships.parallelStream()
                            .filter(gm -> gm.getTitle().equalsIgnoreCase("Treasurer") || gm.getTitle().equalsIgnoreCase("Secretary")).count();

                    if (officials < 2)
                        return new UniversalResponse("fail", getResponseMessage("groupNeedsOfficials"));

                    startGroupInvites(optionalGroup.get(), phonenumbersandrole);

                    CustomAuthenticationUtil.getUsername()
                            .subscribe(username -> sendGroupInvitesSms(username, optionalGroup.get(), phonenumbersandrole));
                    return new UniversalResponse("success", getResponseMessage("inviteIsBeingProcessed"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    private void sendGroupInvitesSms(String username, Group group, List<MemberRoles> phoneNumbersAndRole) {
        Optional<Member> memberOptional = memberRepository.findMemberByEsbwalletaccount(username);
        if (memberOptional.isEmpty()) {
            log.info("Member that was to invite other(s) not found {}", memberOptional);
            return;
        }
        Users user = memberOptional.get().getUsers();
        String memberName = String.format("%s %s", user.getFirstName(), user.getLastName());
        phoneNumbersAndRole.forEach(memberRole -> {
            group.getGroupmembers()
                    .parallelStream()
                    .filter(groupMembership -> !groupMembership.getMembers().getImsi().equals(username))
                    .forEach(groupMembership -> notificationService.sendGroupInviteNotification(memberName,
                            memberRole.getPhonenumber(), group.getName(), groupMembership.getMembers().getImsi(),
                            groupMembership.getMembers().getUsers().getLanguage()));
        });
    }

    @Override
    public Mono<UniversalResponse> sendOfficialsInvite(long groupid, List<MemberRoles> phonenumbersandrole) {
        return Mono.fromCallable(() -> {
                    Optional<Group> optionalGroup = groupRepository.findById(groupid);
                    if (optionalGroup.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("groupNotFound"));
                    }
                    if (phonenumbersandrole.size() != 2) {
                        return new UniversalResponse("fail", getResponseMessage("groupNeedsOfficials"));
                    }

                    startGroupInvites(optionalGroup.get(), phonenumbersandrole);
                    return new UniversalResponse("success", getResponseMessage("groupInviteSentSuccessfully"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getAllMemberGroupsSummary(String phoneNumber) {
        // TODO
        return Mono.empty();
    }

    @Override
    public Mono<UniversalResponse> getAllGroupCategories() {
        return Mono.fromCallable(() -> {
                    List<GroupCategory> groupCategoryList = groupCategoryRepository.findAll();
                    return new UniversalResponse("success", getResponseMessage("groupCategories"), groupCategoryList);
                })
                .publishOn(Schedulers.boundedElastic());
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
    public Mono<UniversalResponse> getAllGroups(Integer page, Integer size) {
        return Mono.fromCallable(() -> {
            Pageable pageable = PageRequest.of(page, size);
            Page<Group> groupsPage = groupRepository.findAllBySoftDeleteFalseOrderByCreatedOnDesc(pageable);
            List<GroupReportWrapper> groupsList = groupsPage
                    .getContent()
                    .parallelStream()
                    .map(this::getGroupReportWrapper)
                    .collect(Collectors.toList());

            UniversalResponse response = new UniversalResponse("success", getResponseMessage("groupsList"), groupsList);
            response.setMetadata(Map.of("numofrecords", groupRepository.countBySoftDeleteFalse()));
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public GroupReportWrapper getGroupReportWrapper(Group group) {
        GroupCategory groupCategory = groupCategoryRepository.findById(group.getCategoryId()).orElse(null);
        return GroupReportWrapper.builder()
                .groupId(group.getId())
                .name(group.getName())
                .location(group.getLocation())
                .description(group.getDescription())
                .category(groupCategory != null ? groupCategory.getName() : "")
                .isActive(group.isActive())
                .createdBy(String.format(" %s %s", group.getCreator().getUsers().getFirstName(), group.getCreator().getUsers().getLastName()))
                .creatorPhone(group.getCreator().getUsers().getPhoneNumber())
                .hasWallet(group.isWalletexists())
                .groupImage(group.getGroupImageUrl())
                .purpose(group.getPurpose())
                .createdOn(group.getCreatedOn())
                .groupSize(group.getGroupmembers().size())
                .build();
    }

    @Override
    @Async
    public void updateEsbGroupRegistration(long groupId) {
        Optional<Group> optionalGroup = groupRepository.findById(groupId);
        if (optionalGroup.isEmpty()) {
            FailedOperations failedOperations = FailedOperations.builder()
                    .stage("Updating group wallet detail")
                    .json_data(gson.toJson(groupId))
                    .message(String.format("Group by id %s not found", groupId))
                    .operation(Operation.OPERATION_ESB_REGISTRATION_RESULTS_UPDATE.name())
                    .build();
            failedOperationsRepository.save(failedOperations);
            return;
        }
        Group group = optionalGroup.get();
        group.setWalletexists(true);
    }

    @Override
    @Async
    public void updatePollResultPositions(List<PollResult> pollResultList) {
        pollResultList.forEach(result -> {
            long groupId = result.getGroupId();
            String positionName = result.getPostName();
            long memberId = result.getMemberId();
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) {
                FailedOperations failedOperations = FailedOperations
                        .builder()
                        .operation(Operation.OPERATION_APPLY_POLL_RESULTS.name())
                        .message(String.format("Group with id %s not found", groupId))
                        .json_data(gson.toJson(result))
                        .groupId(groupId)
                        .stage("Group search")
                        .build();
                failedOperationsRepository.save(failedOperations);
                return;
            }
            Optional<Member> optionalMember = memberRepository.findById(memberId);
            if (optionalMember.isEmpty()) {
                FailedOperations failedOperations = FailedOperations
                        .builder()
                        .operation(Operation.OPERATION_APPLY_POLL_RESULTS.name())
                        .message(String.format("Member with id %s not found", memberId))
                        .json_data(gson.toJson(result))
                        .groupId(groupId)
                        .stage("Member Search")
                        .build();
                failedOperationsRepository.save(failedOperations);
                return;
            }
            Member newMember = optionalMember.get();
            Group group = optionalGroup.get();
            Optional<GroupMembership> optionalOldMember = groupMemberRepo.findFirstByGroupAndTitle(group, positionName);
            GroupTitles groupTitles = null;
            if (optionalOldMember.isPresent()) {
                groupTitles = groupTitleRepo.findByTitlenameContaining(PositionName.MEMBER.name()).get();
                GroupMembership oldMember = optionalOldMember.get();
                oldMember.setTitle(PositionName.MEMBER.name());
                oldMember.setPermissions(groupTitles.getPermissions());
                groupMemberRepo.save(oldMember);
//                publishingService.logoutUser().apply(oldMember.getMembers().getUsers().getPhoneNumber());
            }
            GroupMembership newGroupMembership = groupMemberRepo.findByMembersAndGroup(newMember, group).get();
            if (groupTitles == null) {
                groupTitles = groupTitleRepo.findByTitlenameContaining(positionName).get();
            }
            newGroupMembership.setTitle(positionName);
            newGroupMembership.setPermissions(groupTitles.getPermissions());
            groupMemberRepo.save(newGroupMembership);
//            publishingService.logoutUser().apply(newGroupMembership.getMembers().getUsers().getPhoneNumber());
        });
    }

    @Override
    public Mono<UniversalResponse> getMemberpermissionsInGroup(String phonenumber, long groupid) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findChamaMemberByUserPhone(phonenumber);
                    if (optionalMember.isEmpty()) {
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("memberNotFound")).build();
                    }
                    Optional<Group> optionalGroup = groupRepository.findById(groupid);
                    if (optionalGroup.isEmpty()) {
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("groupNotFound")).build();
                    }
                    Optional<GroupMembership> optionalGroupMembership = groupMemberRepo.findByMembersAndGroup(optionalMember.get(), optionalGroup.get());
                    if (optionalGroupMembership.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("noPermissionsFound"), Collections.emptyList());
                    }
                    Map permissions = gson.fromJson(optionalGroupMembership.get().getPermissions(), Map.class);
                    return new UniversalResponse("success", getResponseMessage("userPermissionsPerGroup"), permissions);
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> updateMemberpermissionsperGroup(String phonenumber, long groupid, Map<String, Object> permissions) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findChamaMemberByUserPhone(phonenumber);
                    UniversalResponse response;
                    if (optionalMember.isEmpty()) {
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("memberNotFound")).build()
                                ;
                    }
                    Optional<Group> optionalGroup = groupRepository.findById(groupid);
                    if (optionalGroup.isEmpty()) {
                        return UniversalResponse.builder().status("fail").message(getResponseMessage("groupNotFound")).build();
                    }
                    Optional<GroupMembership> optionalGroupMembership = groupMemberRepo.findByMembersAndGroup(optionalMember.get(), optionalGroup.get());
                    if (optionalGroupMembership.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"), Collections.emptyList());
                    }
                    GroupMembership groupMembership = optionalGroupMembership.get();
                    String permissions_temp = gson.toJson(permissions);
                    groupMembership.setPermissions(permissions_temp);
                    groupMemberRepo.save(groupMembership);
                    return new UniversalResponse("success", getResponseMessage("memberPermissionsUpdated"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    /**
     * Needs to have role of treasurer or Chairman to perform actions
     *
     * @param memberPhoneNumber
     * @param groupId
     * @param approve
     * @param currentUser
     * @return
     */
    @Override
    public Mono<UniversalResponse> approveDeclineMemberRequestToLeaveGroup(String memberPhoneNumber, Long groupId, boolean approve, String currentUser) {
        return Mono.fromCallable(() -> {
            if (memberPhoneNumber.equals(currentUser))
                return new UniversalResponse("fail", getResponseMessage("cannotApproveOwnRequest"));

            Optional<Member> optionalMember = findChamaMemberByUserPhone(currentUser);
            if (optionalMember.isEmpty())
                throw new MemberNotFoundException();

            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty())
                throw new GroupNotFoundException();

            Member member = optionalMember.get();
            Group group = optionalGroup.get();
            Optional<GroupMembership> optionalMembership = groupMemberRepo.findByMembersAndGroup(member, optionalGroup.get());
            if (optionalMembership.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"));

            Optional<Member> optionalActionedMember = findChamaMemberByUserPhone(memberPhoneNumber);
            if (optionalActionedMember.isEmpty())
                throw new MemberNotFoundException();

            Member actionedMember = optionalActionedMember.get();
            Optional<GroupMembership> optionalActionedMembership = groupMemberRepo.findByMembersAndGroup(actionedMember, group);
            if (optionalActionedMembership.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("actionedMemberNotPartOfGroup"));

            GroupMembership groupMembership = optionalActionedMembership.get();
            groupMembership.setActivemembership(!approve);
            groupMembership.setIsrequesttoleaveactedon(true);
            groupMemberRepo.save(groupMembership);

            String requestingToLeaveMemberName = String.format("%s %s", actionedMember.getUsers().getFirstName(), actionedMember.getUsers().getLastName());
            String officialName = String.format("%s %s", member.getUsers().getFirstName(), member.getUsers().getLastName());

            sendRequestToLeaveSms(requestingToLeaveMemberName, officialName, approve, actionedMember.getImsi(), group.getName(), group.getId(), actionedMember.getUsers().getLanguage());
            // write off loans, fines and penalties
            if (approve) publishingService.writeOffLoansAndPenalties(actionedMember.getId(), groupId);
            return new UniversalResponse("success", getResponseMessage("requestProcessedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    private void sendRequestToLeaveSms(String requestingToLeaveMemberName, String officialName, boolean approve, String phoneNumber, String groupName, long groupId, String language) {
        List<GroupMembership> groupMemberships = groupMemberRepo.findAllByGroupIdAndActivemembershipTrue(groupId);
        if (approve) {
            // send to actioned member
            try {
                notificationService.sendRequestToLeaveAccepted(requestingToLeaveMemberName, officialName, phoneNumber, groupName, language);
            }catch (Exception e){
                log.error("failure to send request to leave message {}",e.getMessage());
            }

            // send to rest of group
            groupMemberships.parallelStream()
                    .filter(gm -> !Objects.equals(gm.getMembers().getImsi(), phoneNumber))
                    .forEach(gm -> notificationService.sendRequestToLeaveAcceptedGroup(requestingToLeaveMemberName, officialName, gm.getMembers().getImsi(), groupName, gm.getMembers().getUsers().getLanguage()));
            return;
        }

        // send to actioned member
        try {
            notificationService.sendRequestToLeaveDeclined(requestingToLeaveMemberName, officialName, phoneNumber, groupName, language);
        } catch (Exception e){
            log.error("request leave declined message failure...... {}",e.getMessage());
        }

        // send to rest of group
        groupMemberships.parallelStream()
                .filter(gm -> !Objects.equals(gm.getMembers().getImsi(), phoneNumber))
                .forEach(gm -> notificationService.sendRequestToLeaveDeclinedGroup(requestingToLeaveMemberName, officialName, gm.getMembers().getImsi(), groupName, gm.getMembers().getUsers().getLanguage()));
    }

    @Override
    public Mono<UniversalResponse> getMembersRequestToLeaveGroup(Long groupId, String currentUser) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(currentUser);
            if (optionalMember.isEmpty()) {
                throw new MemberNotFoundException();
            }
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty())
                throw new GroupNotFoundException();
            //
            Group group = optionalGroup.get();
            Member member = optionalMember.get();
            Optional<GroupMembership> optionalMembership = groupMemberRepo.findByMembersAndGroup(member, group);
            if (optionalMembership.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"));

            List<GroupMembership> groupMembershipList =
                    groupMemberRepo.findAllByGroupAndRequesttoleavegroupIsTrueAndIsrequesttoleaveactedonIsTrue(group);
            List<MemberWrapper> finalMembersList = groupMembershipList.parallelStream()
                    .map(mapperFunction.mapToMemberWrapper())
                    .collect(Collectors.toList());
            return new UniversalResponse("success", getResponseMessage("membersRequestingToLeave"), finalMembersList);
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> deactivateGroupAccount(GroupMembershipWrapper groupMembershipWrapper) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(groupMembershipWrapper.getPhonenumber());
            if (optionalMember.isPresent()) {
                Member members = optionalMember.get();
                Optional<Group> optionalGroup = groupRepository.findById(groupMembershipWrapper.getGroupid());
                if (optionalGroup.isEmpty()) throw new GroupNotFoundException();
                Group group = optionalGroup.get();
                Optional<GroupMembership> optionalMembership = groupMemberRepo.findByMembersAndGroup(members, group);
                if (optionalMembership.isEmpty())
                    return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"));
                GroupMembership membership = optionalMembership.get();
                membership.setRequesttoleavegroup(true);
                groupMemberRepo.save(membership);
                return new UniversalResponse("success", getResponseMessage("requestToLeaveReceived"),
                        Collections.emptyList());
            }
            return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"), Collections.emptyList());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> uploadDocuments(long groupid, FilePart document, String fileName, String uploadedby, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Users> optionalUser = userRepository.findByPhoneNumberAndChannel(uploadedby, channel.name());
            if (optionalUser.isEmpty()) {
                return new UniversalResponse("fail", "user not found");
            }
            Optional<Group> optionalGroup = groupRepository.findById(groupid);
            if (optionalGroup.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));
            }
            Users users = optionalUser.get();
            Group group = optionalGroup.get();
            String path = fileHandlerService.uploadFile(document);
            if (!path.isEmpty()) {
                GroupDocuments documents = GroupDocuments.builder()
                        .fileName(fileName)
                        .group(group)
                        .uploadedBy(users)
                        .path(path)
                        .build();
                groupDocumentsRepository.save(documents);
                return new UniversalResponse("success", String.format("%s %s", "upload  success for file", fileName));
            } else {
                return new UniversalResponse("fail", getResponseMessage("failedToUploadDocument"));
            }
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ResponseEntity<?>> retrieveFile(long groupid, String filename) {
        return Mono.fromSupplier(() -> {
            Group group = groupRepository.findById(groupid).orElse(null);
            if (group == null)
                return ResponseEntity.ok().body(new UniversalResponse("fail", getResponseMessage("groupNotFound")));

            GroupDocuments groupDocuments = groupDocumentsRepository.findByGroupAndFileName(group, filename).orElse(null);

            if (groupDocuments == null)
                return ResponseEntity.ok().body(new UniversalResponse("fail", getResponseMessage("groupDocumentNotFound")));

            return fileHandlerService.downloadFileFromUrl(groupDocuments.getPath()).block();
        }).publishOn(Schedulers.boundedElastic());
    }

    private List<GroupsDocumentsWrapper> getGroupDocuments(Group group) {
        List<GroupDocuments> groupDocumentsList = groupDocumentsRepository.findAllByGroup(group);
        return groupDocumentsList.parallelStream()
                .filter(doc -> doc.getPath() == null)
                .map(doc -> GroupsDocumentsWrapper.builder()
                        .name(doc.getName())
                        .uploadedBy(String.format(String.format("%s %s", doc.getUploadedBy().getFirstName(), doc.getUploadedBy().getLastName())))
                        .url(fileHandlerService.getFileUrl(doc.getPath()))
                        .build()
                ).collect(Collectors.toList());
    }

    @Override
    public Mono<UniversalResponse> getGroupDetails(long groupId) {
        return Mono.fromCallable(() -> {
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) {
                throw new GroupNotFoundException();
            }
            Group group = optionalGroup.get();

            GroupWrapper groupWrapper = mapperFunction.mapGroupToGroupWrapper().apply(group);
            Optional<GroupCategory> groupCategory = groupCategoryRepository.findById(groupWrapper.getCategoryId());

            groupCategory.ifPresent(gc -> groupWrapper.setGroupType(gc.getName()));

            int numberOfInvites = groupInvitesRepository.countByGroup(group);
            int numberOfGroupMembers = groupMemberRepo.countByGroupAndActivemembershipIsTrue(group);

            Map<String, List<Object>> defaultObject = Map.of("accounts", Collections.emptyList(), "contributions", Collections.emptyList());
            UniversalResponse groupAccountAndContributions = webClient.get()
                    .uri("/api/v2/payment/ussd/account/group-info?groupId=" + group.getId())
                    .retrieve()
                    .bodyToMono(UniversalResponse.class)
                    .onErrorResume(t -> {
                        log.error(t.getLocalizedMessage());
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("couldNotFetchGroupAccountingInfo"), defaultObject));
                    }).block();

            Map<String, Object> data = (Map<String, Object>) groupAccountAndContributions.getData();
            Map<String, Object> newData = Map.of(
                    "groupDetails", groupWrapper,
                    "accounts", data.get("accounts"),
                    "contributions", data.get("contributions"),
                    "invites", numberOfInvites,
                    "members", numberOfGroupMembers
            );

            return new UniversalResponse("success", getResponseMessage("groupDetails"), newData);
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<GroupAccountDetails> groupAccountDetails(long groupId) {
        return webClient.get()
                .uri("" + groupId)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve()
                .bodyToMono(String.class)
                .map(res -> gson.fromJson(res, GroupAccountDetails.class))
                .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(500)));
    }

    @Override
    public Mono<UniversalResponse> getCountryData() {
        return Mono.fromCallable(() -> {
            try {
                File countryJSon = ResourceUtils.getFile("classpath:country-codes.json");
                final Type REVIEW_TYPE = new TypeToken<List<CountryInfo>>() {
                }.getType();
                JsonReader reader = new JsonReader(new FileReader(countryJSon));
                List<CountryInfo> data = gson.fromJson(reader, REVIEW_TYPE);
                return new UniversalResponse("success", getResponseMessage("countries"), data);
            } catch (Exception ignored) {
            }
            return new UniversalResponse("fail", getResponseMessage("serverError"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> readFileandsendInvites(Mono<FilePart> multipartFile, long groupid) {
        return Mono.fromCallable(() -> multipartFile.map(filePart -> {
                    if (!filePart.filename().split(".")[1].equals("xlsx")) {
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("unsupportedFileExtension"), Collections.emptyList()));
                    }
                    List<MemberRoles> phonenumberandroles = getPhoneNumbersFromFile(filePart);
                    if (phonenumberandroles.isEmpty()) {
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("couldNotExtractPhoneNumbers"), Collections.emptyList()));
                    }
                    Optional<Group> optionalGroup = groupRepository.findById(groupid);
                    if (optionalGroup.isEmpty()) {
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("groupNotFound"), Collections.emptyList()));
                    }
                    Group group = optionalGroup.get();
                    startGroupInvites(group, phonenumberandroles);
                    return Mono.just(new UniversalResponse("success", getResponseMessage("groupInviteSentSuccessfully"), Collections.emptyList()));
                }))
                .flatMap(res -> res)
                .flatMap(res -> res)
                .publishOn(Schedulers.boundedElastic());
    }

    private List<MemberRoles> getPhoneNumbersFromFile(FilePart file) {
        List<MemberRoles> phoneNumbersAndRoles = new ArrayList<>();
        try {
            Workbook workbook = WorkbookFactory.create(file);
            Sheet datatypeSheet = workbook.getSheetAt(0);
            for (Row currentRow : datatypeSheet) {
                Cell phoneNumberCell = currentRow.getCell(0);
                Cell roleCell = currentRow.getCell(1);
                if (phoneNumberCell.getCellType() == CellType.STRING) {
                    //we dont want string values
                    log.info(phoneNumberCell.getStringCellValue() + "--string");
                } else if (phoneNumberCell.getCellType() == CellType.NUMERIC) {
                    log.info(phoneNumberCell.getNumericCellValue() + "--int");
                    MemberRoles memberRoles = new MemberRoles();
                    memberRoles.setPhonenumber(String.valueOf(phoneNumberCell.getNumericCellValue()));
                    memberRoles.setRole(roleCell.getStringCellValue());
                    phoneNumbersAndRoles.add(memberRoles);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return phoneNumbersAndRoles;
    }

    @Async
    public void startGroupInvites(Group group, List<MemberRoles> invites) {
        List<GroupInvites> groupInvitesList = new ArrayList<>();
        invites.parallelStream().forEach(phonenumbersandrole -> {
            String phoneNumber = phonenumbersandrole.getPhonenumber();
            if (phoneNumber.startsWith("0")) {
                phoneNumber = phoneNumber.replaceFirst("0", "255");
            }
            if (phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.replaceFirst("\\+", "");
            }

            String role = phonenumbersandrole.getRole();
            Optional<Member> optionalMember = findChamaMemberByUserPhone(phoneNumber);
            if (optionalMember.isPresent()) {
                Optional<GroupMembership> optionalGroupMembers = groupMemberRepo.findByMembersAndGroup(optionalMember.get(), group);
                if (optionalGroupMembers.isPresent()) {
                    // reactivate membership
                    GroupMembership groupMembership = optionalGroupMembers.get();
                    groupMembership.setActivemembership(true);
                    groupMembership.setRequesttoleavegroup(false);
                    groupMembership.setSoftDelete(false);
                    groupMemberRepo.save(groupMembership);

                    // Send message to invited person
                    try {
                        notificationService.sendMemberGroupRejoinSms(group.getName().toUpperCase(), phoneNumber, optionalMember.get().getUsers().getLanguage());
                    }catch (Exception e){
                        log.error("cannot send invite member sms {}",e.getMessage());
                    }

                    return;
                }
            }
            GroupInvites groupInvites = new GroupInvites();
            groupInvites.setGroup(group);
            groupInvites.setPhonenumber(phoneNumber);
            groupInvites.setNewmember(optionalMember.isEmpty());
            groupInvites.setStatus("active");
            groupInvites.setInvitedrole(role);
            //send out text notifications
            SMS_TYPES textType = groupInvites.isNewmember() ? SMS_TYPES.NEW_MEMBER_INVITE : SMS_TYPES.EXISTING_MEMBER_INVITE;

            //
            try {
                notificationService.sendInviteSms(textType, group.getName(), phoneNumber, "English");
            } catch (Exception e){
                log.error("send invite sms failure----------{}",e.getMessage());
            }



            List<GroupInvites> invitesList = groupInvitesRepository.findByPhonenumberAndGroupId(phoneNumber, group.getId());
            AtomicBoolean isAlreadyAMember = new AtomicBoolean(false);
            invitesList.forEach(groupInvites1 -> {
                if (groupInvites1.getStatus().equals("active") || groupInvites1.getStatus().equals("accepted")) {
                    isAlreadyAMember.set(true);
                }

                if (groupInvites1.getStatus().toLowerCase().equals("rejected")) {
                    groupInvites1.setStatus("active");
                }
            });
            if (!isAlreadyAMember.get())
                groupInvitesList.add(groupInvites);
        });
        groupInvitesRepository.saveAll(groupInvitesList);
    }

    @Override
    public Mono<UniversalResponse> getGroupsbyMemberaccount(String account, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(account);
            if (optionalMember.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());

            Pageable pageable = PageRequest.of(page, size);

            return new UniversalResponse("success", getResponseMessage("groupsMemberBelongs"),
                    getGroupsMemberBelongsTo(optionalMember.get(), pageable));
        }).publishOn(Schedulers.boundedElastic());
    }

    private Map<Long, GroupAccountDetails> getGroupsSummary(String account) {
        Optional<Member> optionalMember = findChamaMemberByUserPhone(account);
        if (optionalMember.isEmpty()) return null;
        Pageable pageable = PageRequest.of(0, 50);
        List<GroupMembershipWrapper> groupsMemberBelongsto = getGroupsMemberBelongsTo(optionalMember.get(), pageable);
        Map<Long, GroupAccountDetails> summaryHashmap = new HashMap<>();

        for (GroupMembershipWrapper groupMembership : groupsMemberBelongsto) {
            Group groups = groupRepository.findById(groupMembership.getGroupid()).orElse(null);
            if (groups == null) continue;
            groupAccountDetails(groups.getId())
                    .doOnNext(details -> {
                        summaryHashmap.put(groups.getId(), details);
                    }).subscribe();
        }

        return summaryHashmap;
    }

    @Override
    public List<GroupMembershipWrapper> getGroupsMemberBelongsTo(Member members, Pageable pageable) {
        Page<GroupMembership> groupMembershipPage = groupMemberRepo.findByMembersAndActivemembershipTrue(members, pageable);

        return groupMembershipPage.getContent().parallelStream()
                .filter(groupMembership -> !groupMembership.getGroup().isSoftDelete() && groupMembership.isActivemembership())
                .map(p -> {
                    GroupMembershipWrapper groupMembership = new GroupMembershipWrapper();
                    Group groups = p.getGroup();
                    groupMembership.setGroupid(groups.getId());
                    groupMembership.setActivemembership(p.isActivemembership());
                    groupMembership.setGroupname(groups.getName());
                    groupMembership.setGroupsize((int) groups.getGroupmembers().stream().filter(GroupMembership::isActivemembership).count());
                    groupMembership.setLocation(groups.getLocation());
                    groupMembership.setTitle(p.getTitle());
                    groupMembership.setCreatedon(p.getCreatedOn());
                    groupMembership.setGroupImageUrl(groups.getGroupImageUrl());
                    groupMembership.setGroupActive(groups.isActive());
                    return groupMembership;
                }).collect(Collectors.toList());
    }

    @Override
    public Mono<UniversalResponse> getInvitesByMemberAccount(String phoneNumber, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(phoneNumber);
            if (optionalMember.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());
            }
            Pageable pageable = PageRequest.of(page, size);
            List<GroupInvites> groupInvitesList = groupInvitesRepository.findByPhonenumberAndStatus(phoneNumber, "active", pageable);
            List<GroupInvitesbyMember> groupInvites =
                    groupInvitesList
                            .parallelStream()
                            .map(p -> {
                                GroupInvitesbyMember groupInvitesbyMember = new GroupInvitesbyMember();
                                groupInvitesbyMember.setCreatedon(p.getCreatedOn());
                                groupInvitesbyMember.setGroupname(p.getGroup().getName());
                                groupInvitesbyMember.setId(p.getId());
                                return groupInvitesbyMember;
                            }).collect(Collectors.toList());
            int countInvites = groupInvitesRepository.countByPhonenumberAndStatus(phoneNumber, "active");
            UniversalResponse universalResponse = new UniversalResponse("success",
                    getResponseMessage("pendingGroupInvites"),
                    groupInvites);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", countInvites);
            universalResponse.setMetadata(metadata);
            return universalResponse;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getInvitesByGroup(long groupId, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), Collections.emptyList());
            }
            Group group = optionalGroup.get();
            List<GroupinvitesbyGroup> inviteList = group
                    .getGroupinvites()
                    .stream()
                    .filter(m -> m.getStatus().equals("active"))
                    .map(m -> {
                        GroupinvitesbyGroup groupinvitesbyGroup = new GroupinvitesbyGroup();
                        groupinvitesbyGroup.setCreatedon(m.getCreatedOn());
                        groupinvitesbyGroup.setInviteid(m.getId());
                        groupinvitesbyGroup.setPhonenumber(m.getPhonenumber());
                        groupinvitesbyGroup.setStatus(m.getStatus());
                        groupinvitesbyGroup.setRegisteredmember(!m.isNewmember());

                        return groupinvitesbyGroup;
                    }).collect(Collectors.toList());
            UniversalResponse universalResponse = new UniversalResponse("success", getResponseMessage("group"), inviteList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", inviteList.size());
            universalResponse.setMetadata(metadata);
            return universalResponse;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getAllMembersInGroup(long groupId, int page, int size) {
        return Mono.fromCallable(() -> {
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) {
                throw new GroupNotFoundException();
            }
            Group group = optionalGroup.get();
            Pageable pageable = PageRequest.of(page, size);
            List<GroupMembershipWrapper> groupMembershipList =
                    groupMemberRepo.findByGroup(group, pageable)
                            .stream()
                            .map(p -> {
                                GroupMembershipWrapper groupMembership = new GroupMembershipWrapper();
                                Member members = p.getMembers();
                                String name = members.getUsers().getFirstName() + " " + members.getUsers().getLastName();
                                groupMembership.setName(name);
                                groupMembership.setGroupid(group.getId());
                                groupMembership.setActivemembership(p.isActivemembership());
                                groupMembership.setEmail(members.getUsers().getEmail());
                                groupMembership.setPhonenumber(members.getUsers().getPhoneNumber());
                                groupMembership.setGroupname(group.getName());
                                groupMembership.setTitle(p.getTitle());
                                groupMembership.setCreatedon(p.getCreatedOn());
                                groupMembership.setDeactivationreason(p.getDeactivationreason());
                                groupMembership.setId(p.getId());
                                return groupMembership;
                            }).collect(Collectors.toList());
            int countMembers = groupMemberRepo.countByGroup(group);
            UniversalResponse universalResponse = new UniversalResponse("success", getResponseMessage("membersInGroup"), groupMembershipList);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("numofrecords", countMembers);
            universalResponse.setMetadata(metadata);

            return universalResponse;
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> acceptordeclineInvite(long inviteid, String phoneNumber, boolean groupmember) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(phoneNumber);
            if (optionalMember.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());
            }
            Member member = optionalMember.get();
            List<GroupInvites> invitesList = groupInvitesRepository.findByPhonenumberAndStatusAndId(phoneNumber, "active", inviteid);
            if (invitesList.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("inviteNotFound"), Collections.emptyList());
            }
            if (groupmember)
                acceptInvite(invitesList, member);
            else
                declineInvite(invitesList);
            return new UniversalResponse(
                    "success",
                    groupmember ? getResponseMessage("inviteAccepted") : getResponseMessage("declineInvite"),
                    invitesList.get(0).getGroup().getId());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void declineInvite(List<GroupInvites> invitesList) {
        List<GroupInvites> declinedInvites = invitesList.parallelStream()
                .peek(invite -> invite.setStatus("rejected")).collect(Collectors.toList());
        groupInvitesRepository.saveAll(declinedInvites);
    }

    @Async
    public void acceptInvite(List<GroupInvites> invitesList, Member member) {
        List<GroupInvites> acceptedList = invitesList.parallelStream()
                .peek(invite -> {
                    invite.setStatus("accepted");
                    String roleTitle = invite.getInvitedrole();
                    Group groups = invite.getGroup();
                    addGroupMember(member, groups, roleTitle.toLowerCase());
                }).collect(Collectors.toList());
        groupInvitesRepository.saveAll(acceptedList);
    }

    @Override
    public Mono<UniversalResponse> exitFromGroup(long groupId, String phoneNumber, String reasonToLeave) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(phoneNumber);
            if (optionalMember.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());
            }
            //
            Optional<Group> optionalGroup = groupRepository.findById(groupId);
            if (optionalGroup.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"), Collections.emptyList());
            }
            //

            // Check if group has all officials
            Group group = optionalGroup.get();
            if (!checkIfGroupHasQuorum(group)) return UniversalResponse.builder()
                    .status("Success")
                    .message(getResponseMessage("lackOfQuorum"))
                    .build();
            String response = removeGroupmember(optionalMember.get(), group, reasonToLeave);
            return new UniversalResponse(response.contains("removed") ? "success" : "fail", response);
        }).publishOn(Schedulers.boundedElastic());
    }

    private boolean checkIfGroupHasQuorum(Group group) {
        long numberOfOfficials = groupMemberRepo.findByGroup(group)
                .parallelStream()
                .filter(membership -> !membership.getTitle().equals("member"))
                .count();
        if (numberOfOfficials < 3) {
            group.setSoftDelete(true);

            groupRepository.save(group);
            return false;
        }
        return true;
    }

    public String removeGroupmember(Member members, Group groups, String reasontoleave) {
        Optional<GroupMembership> optionalGroupMember = groupMemberRepo.findByMembersAndGroup(members, groups);
        if (optionalGroupMember.isEmpty()) {
            return getResponseMessage("memberIsNotPartOfGroup");
        }
        GroupMembership groupMembership = optionalGroupMember.get();
        if (!groupMembership.getTitle().equalsIgnoreCase("member")) {
            return getResponseMessage("officialNotAllowedToLeave");
        }
        //
        if (groupMembership.isActivemembership()) {
            groupMembership.setActivemembership(false);
            groupMembership.setDeactivationreason(reasontoleave);
            groupMembership.setRequesttoleavegroup(true);
            groupMemberRepo.save(groupMembership);
            return getResponseMessage("memberRemovedFromGroup");
        }
        return getResponseMessage("memberAlreadyDeactivated");
    }

    @Override
    public Mono<UniversalResponse> activateUserGroupAccount(GroupMembershipWrapper groupMembershipWrapper) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(groupMembershipWrapper.getPhonenumber());
            if (optionalMember.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());
            }
            Member members = optionalMember.get();
            Optional<Group> optionalGroup = groupRepository.findById(groupMembershipWrapper.getGroupid());
            if (optionalGroup.isEmpty()) throw new GroupNotFoundException();
            Optional<GroupMembership> optionalGroupMember = groupMemberRepo.findByMembersAndGroup(members, optionalGroup.get());
            if (optionalGroupMember.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberIsNotPartOfGroup"));
            GroupMembership groupMembership = optionalGroupMember.get();
            groupMembership.setActivemembership(true);
            groupMemberRepo.save(groupMembership);
            return new UniversalResponse("success", getResponseMessage("memberGroupAccountActivated"), Collections.emptyList());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> updateUserGroupRole(UpdatememberpermissionsWrapper updatememberpermissionsWrapper) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findChamaMemberByUserPhone(updatememberpermissionsWrapper.getPhonenumber());
            Optional<GroupTitles> groupTitles = groupTitleRepo.findByTitlenameContaining(updatememberpermissionsWrapper.getRole());
            if (optionalMember.isPresent()) {
                Member members = optionalMember.get();
                Optional<Group> optionalGroup = groupRepository.findById(updatememberpermissionsWrapper.getGroupid());
                if (optionalGroup.isEmpty()) throw new GroupNotFoundException();
                Optional<GroupMembership> optionalGroupMembers = groupMemberRepo.findByMembersAndGroup(members, optionalGroup.get());
                if (optionalGroupMembers.isEmpty()) {
                    return new UniversalResponse("fail", getResponseMessage("groupMembershipNotFound"));
                }
                GroupMembership groupMembership = optionalGroupMembers.get();
                groupMembership.setTitle(groupTitles.isPresent() ? groupTitles.get().getTitlename() : "");
                String permissions = groupTitles.isPresent() ? groupTitles.get().getPermissions() : "{}";
                groupMembership.setPermissions(permissions);
                groupMemberRepo.save(groupMembership);
                return new UniversalResponse("success", getResponseMessage("memberRoleUpdated"), Collections.emptyList());
            }
            throw new MemberNotFoundException();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public void addGroupMember(Member members, Group groups, String roletitle) {
        Optional<GroupMembership> groupMembersList = groupMemberRepo.findByMembersAndGroup(members, groups);
        Optional<GroupTitles> groupTitles = groupTitleRepo.findByTitlenameContaining(roletitle);
        GroupMembership groupMembership;
        if (groupMembersList.isEmpty()) {
            groupMembership = new GroupMembership();
            groupMembership.setGroup(groups);
            groupMembership.setMembers(members);
            groupMembership.setActivemembership(true);
            groupMembership.setTitle(groupTitles.isPresent() ? groupTitles.get().getTitlename() : roletitle);
            String permissions = groupTitles.isPresent() ? groupTitles.get().getPermissions() : "{}";
            groupMembership.setPermissions(permissions);
        } else {
            groupMembership = groupMembersList.get();
            groupMembership.setActivemembership(true);
            groupMembership.setDeactivationreason("");
            groupMembership.setTitle(groupTitles.isPresent() ? groupTitles.get().getTitlename() : roletitle);
            String permissions = groupTitles.isPresent() ? groupTitles.get().getPermissions() : "{}";
            groupMembership.setPermissions(permissions);
        }
        groupMemberRepo.save(groupMembership);
    }

    @Override
    public Mono<UniversalResponse> enableGroup(Long groupId, String cbsAccount, String loggedUser) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);

            if (groupOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Group group = groupOptional.get();
            if (group.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsActive"));

            if (group.getCbsAccount() != null && !group.getCbsAccount().equals(DEFAULT_ACCOUNT))
                return new UniversalResponse("fail", getResponseMessage("cannotChangeGroupCoreAccount"));

            Optional<Users> user = userRepository.findByEmailAndChannel(loggedUser, Channel.PORTAL.name());

            if (user.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("userNotFound"));

            if (group.getCbsAccount() == null || group.getCbsAccount().equals(DEFAULT_ACCOUNT) && cbsAccount.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("cannotEnableGroupWithoutCBS"));

            Optional<Group> optionalGroupWithCBSAccount = groupRepository.findByCbsAccount(cbsAccount);

            if (optionalGroupWithCBSAccount.isPresent())
                return new UniversalResponse("fail", String.format(getResponseMessage("groupWithCBSAccountExists"), optionalGroupWithCBSAccount.get().getName()));

            // verify group account
            UniversalResponse groupLookup = null;

            if (!cbsAccount.isEmpty()) {
                groupLookup = groupAccountLookupOnCBS(cbsAccount);

                if (groupLookup.getStatus().equals("fail"))
                    return new UniversalResponse("fail", getResponseMessage("coreAccountNotFound"));
            }

            // send event to enable group contributions
            String modifier = String.format("{ %s %s, %s }", user.get().getFirstName(), user.get().getLastName(), user.get().getId());
            publishingService.enableGroupContributions(group.getId(), modifier);

            String account = group.getCbsAccount() == null || group.getCbsAccount().equals(DEFAULT_ACCOUNT) ? cbsAccount : group.getCbsAccount();

            VicobaGroupRequest vicobaGroupRequest = (VicobaGroupRequest) groupLookup.getData();

            publishingService.updateGroupCoreAccount(account, group.getId(), vicobaGroupRequest.getAvailableBalance(), modifier);

            group.setCbsAccount(account);
            group.setActive(true);
            group.setLastModifiedBy(modifier);
            group.setLastModifiedDate(new Date());
            groupRepository.save(group);

            sendGroupEnabledSms(group, true);

            return new UniversalResponse("success", "Group activated successfully!");
        }).publishOn(Schedulers.boundedElastic());
    }

    @Async
    public void sendGroupEnabledSms(Group group, boolean activated) {
        groupMemberRepo.findByGroup(group)
                .parallelStream()
                .filter(GroupMembership::isActivemembership)
                .forEach(gm -> {
                    if (activated)
                        notificationService.sendGroupEnabledSms(group.getName(), gm.getMembers().getImsi(), gm.getMembers().getUsers().getLanguage());
                    else
                        notificationService.sendGroupDisabledSms(group.getName(), gm.getMembers().getImsi(), gm.getMembers().getUsers().getLanguage());
                });
    }

    @Override
    public Mono<UniversalResponse> activateGroup(Long groupId, String loggedUser) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);

            if (groupOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Group group = groupOptional.get();

            if (group.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsActive"));

            if (group.getCbsAccount() == null || group.getCbsAccount().equals(DEFAULT_ACCOUNT))
                return new UniversalResponse("fail", getResponseMessage("enableGroupFirst"));

            Optional<Users> userOptional = userRepository.findByEmail(loggedUser);

            if (userOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("adminNotFound"));

            Users user = userOptional.get();

            // send event to enable group contributions
            String modifier = String.format("{ %s %s, %s }", user.getFirstName(), user.getLastName(), user.getId());
            publishingService.enableGroupContributions(group.getId(), modifier);

            group.setActive(true);
            group.setLastModifiedBy(modifier);
            group.setLastModifiedDate(new Date());

            groupRepository.save(group);
            sendGroupEnabledSms(group, false);
            return new UniversalResponse("success", getResponseMessage("groupActivatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> disableGroup(Long groupId, String loggedUser) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);

            if (groupOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Group group = groupOptional.get();
            if (!group.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsDisabled"));

            Optional<Users> userOptional = userRepository.findByEmail(loggedUser);

            if (userOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("adminNotFound"));

            Users user = userOptional.get();

            // send event to enable group contributions
            String modifier = String.format("{ %s %s, %s }", user.getFirstName(), user.getLastName(), user.getId());
            publishingService.disableGroupContributions(group.getId(), modifier);

            group.setActive(false);
            group.setLastModifiedBy(modifier);
            group.setLastModifiedDate(new Date());
            groupRepository.save(group);
            sendGroupEnabledSms(group, false);
            return new UniversalResponse("success", getResponseMessage("groupActivatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> disableGroupByChairperson(Long groupId, String loggedUser) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);

            if (groupOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Group group = groupOptional.get();
            if (!group.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsDisabled"));

            Optional<Member> memberOptional = memberRepository.findByImsi(loggedUser);

            if (memberOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Optional<GroupMembership> groupMembership = groupMemberRepo.findByMembersAndGroup(memberOptional.get(), group);

            if (groupMembership.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupMembershipNotFound"));

            if (!groupMembership.get().getTitle().equalsIgnoreCase("Chairperson"))
                return new UniversalResponse("fail", getResponseMessage("notTheChairperson"));

            Users user = memberOptional.get().getUsers();

            // send event to enable group contributions
            String modifier = String.format("{ %s %s, %s }", user.getFirstName(), user.getLastName(), user.getId());
            publishingService.disableGroupContributions(group.getId(), modifier);

            group.setActive(false);
            group.setLastModifiedBy(modifier);
            group.setLastModifiedDate(new Date());
            groupRepository.save(group);
            sendGroupEnabledSms(group, false);
            return new UniversalResponse("success", getResponseMessage("groupdeactivatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> updateGroupName(long groupId, String name, String modifier) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);
            Optional<Member> memberOptional = memberRepository.findByImsi(modifier);
            if (groupOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));
            if (memberOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Group group = groupOptional.get();
            if (!group.isActive())
                return new UniversalResponse("fail", getResponseMessage("groupIsInactive"));

            group.setName(name);
            group.setLastModifiedBy(modifier);
            group.setLastModifiedDate(new Date());

            groupRepository.save(group);
            publishingService.updateContributionName(group.getId(), group.getName(), memberOptional.get().getImsi());
            return new UniversalResponse("success", getResponseMessage("groupNameUpdatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    /**
     * Updates the existing official to a normal member and
     * makes the new poll winner the new official with the title of the
     * previous official.
     *
     * @param pollWinnerInfo that contains a member id, group id, and group title
     */
    @Async
    @Override
    public void updateGroupLeader(String pollWinnerInfo) {
        log.info("Group poll winner info... {}", pollWinnerInfo);
        JsonObject jsonObject = gson.fromJson(pollWinnerInfo, JsonObject.class);

        long groupId = jsonObject.get("groupId").getAsLong();
        long memberId = jsonObject.get("memberId").getAsLong();
        String title = jsonObject.get("title").getAsString();

        Optional<Group> groupOptional = groupRepository.findById(groupId);

        groupOptional.ifPresentOrElse(group -> {
            Optional<GroupMembership> existingLeaderMembership =
                    group.getGroupmembers().parallelStream()
                            .filter(gm -> gm.getTitle().equalsIgnoreCase(title)).findFirst();

            Optional<GroupMembership> newLeaderMembership =
                    group.getGroupmembers().parallelStream()
                            .filter(gm -> gm.getMembers().getId() == memberId).findFirst();

            existingLeaderMembership.ifPresentOrElse(egm -> newLeaderMembership.ifPresentOrElse(ngm -> {
                String existingLeaderPermissions = egm.getPermissions();
                String existingLeaderGroupTitle = egm.getTitle();
                String newLeaderOldTitle = ngm.getTitle();
                String newLeaderOldPermissions = ngm.getPermissions();

                ngm.setPermissions(existingLeaderPermissions);
                ngm.setTitle(existingLeaderGroupTitle);
                ngm.setLastModifiedBy("POLLS");
                ngm.setLastModifiedDate(new Date());

                egm.setTitle(newLeaderOldTitle);
                egm.setPermissions(newLeaderOldPermissions);
                ngm.setLastModifiedBy("POLLS");
                egm.setLastModifiedDate(new Date());

                GroupMembership savedEGM = groupMemberRepo.save(egm);
                GroupMembership savedNGM = groupMemberRepo.save(ngm);

                log.info("Updated the new leader successfully...");
                log.info("Updated the existing leader successfully...");

                // send sms notifications to all group members
                String newLeaderName = String.format("%s %s", savedNGM.getMembers().getUsers().getFirstName(),
                        savedNGM.getMembers().getUsers().getLastName());

                sendNewLeaderElectedMessage(group, group.getName(), newLeaderName, savedNGM.getTitle());
            }, () -> log.info("New leader group membership not found... On updating group leader")), () -> log.info("Existing leader group membership not found... On updating group leader"));
        }, () -> log.info("Cannot find group... On updating group leader"));
    }

    private void sendNewLeaderElectedMessage(Group group, String groupName, String newLeaderName, String groupTitle) {
        groupMemberRepo.findByGroup(group).parallelStream()
                .filter(GroupMembership::isActivemembership)
                .forEach(gm -> notificationService.sendNewLeaderElectedMessage(gm.getMembers().getImsi(), newLeaderName, groupTitle, groupName, gm.getMembers().getUsers().getLanguage()));
    }

    @Override
    public Mono<UniversalResponse> getMessageTemplates(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return Mono.fromCallable(() -> messagetemplatesRepo.findAll(pageable))
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("messageTemplates"), res.getContent(),
                        new Date(), Map.of("numofrecords", messagetemplatesRepo.countAllBySoftDeleteFalse())));
    }

    @Override
    public Mono<UniversalResponse> editMessageTemplate(MessageTemplateWrapper messageTemplateWrapper) {
        return Mono.fromCallable(() -> {
            Optional<MessageTemplates> messageTemplatesOptional = messagetemplatesRepo.findById(messageTemplateWrapper.getId());

            if (messageTemplatesOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("messageTemplateNotFound"));

            MessageTemplates messageTemplate = messageTemplatesOptional.get();
            messageTemplate.setTemplate(messageTemplateWrapper.getTemplate() == null ? messageTemplate.getTemplate() : messageTemplateWrapper.getTemplate());
            messageTemplate.setLanguage(messageTemplateWrapper.getLanguage() == null ? messageTemplate.getLanguage() : messageTemplateWrapper.getLanguage());
            messageTemplate.setType(messageTemplateWrapper.getType() == null ? messageTemplate.getType() : messageTemplateWrapper.getType());

            messagetemplatesRepo.save(messageTemplate);

            return new UniversalResponse("success", getResponseMessage("messageTemplateEditedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> addMessageTemplate(MessageTemplateWrapper messageTemplateWrapper) {
        return Mono.fromCallable(() -> {
            MessageTemplates messageTemplate = messagetemplatesRepo.findByTypeAndLanguage(messageTemplateWrapper.getType(), messageTemplateWrapper.getLanguage());

            if (messageTemplate != null)
                return new UniversalResponse("fail", getResponseMessage("messageTemplateExists"));

            if (messageTemplateWrapper.getTemplate() == null || messageTemplateWrapper.getLanguage() == null || messageTemplateWrapper.getType() == null)
                return new UniversalResponse("fail", getResponseMessage("insufficientMessageTemplateDetails"));

            messageTemplate = new MessageTemplates();
            messageTemplate.setTemplate(messageTemplateWrapper.getTemplate());
            messageTemplate.setLanguage(messageTemplateWrapper.getLanguage());
            messageTemplate.setType(messageTemplateWrapper.getType());

            messagetemplatesRepo.save(messageTemplate);

            return new UniversalResponse("success", getResponseMessage("messageTemplateEditedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getGroupMembersSummary(long groupId) {
        return Mono.fromCallable(() -> {
            Optional<Group> groupOptional = groupRepository.findById(groupId);

            if (groupOptional.isEmpty()) return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            List<GroupMembership> groupMemberships = groupMemberRepo.findByGroup(groupOptional.get());

            long totalMembers = groupMemberships.parallelStream().filter(GroupMembership::isActivemembership).count();
            List<MemberWrapper> members = groupMemberships.parallelStream()
                    .filter(gm -> !Objects.equals(gm.getTitle(), "Member") && gm.isActivemembership())
                    .map(gm -> mapperFunction.mapToMemberWrapper().apply(gm))
                    .collect(Collectors.toList());

            return UniversalResponse.builder()
                    .status("success")
                    .message("Group members summary")
                    .data(members)
                    .metadata(Map.of("groupName", groupOptional.get().getName(), "totalMembers", totalMembers))
                    .timestamp(new Date())
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> findAllCountries() {
        return Mono.fromCallable(countryRepository::findAll)
                .publishOn(Schedulers.boundedElastic())
                .map(res -> new UniversalResponse("success", getResponseMessage("countries"), res));
    }

    @Override
    public Mono<UniversalResponse> findMemberInGroup(Long groupId, String phoneNumber) {
        return Mono.fromCallable(() -> {
            Optional<Group> optionalGroup = groupRepository.findById(groupId);

            if (optionalGroup.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("groupNotFound"));

            Optional<Member> optionalMember = memberRepository.findByImsi(phoneNumber);

            if (optionalMember.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));


            Optional<GroupMembership> optionalGroupMembership = groupMemberRepo.findByMembersAndGroup(optionalMember.get(), optionalGroup.get());

            if (optionalGroupMembership.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotPartOfGroup"));

            MemberWrapper memberWrapper = mapperFunction.mapMemberToMemberWrappers().apply(optionalMember.get());

            return new UniversalResponse("success", "Member in group", memberWrapper);
        }).publishOn(Schedulers.boundedElastic());
    }
}
