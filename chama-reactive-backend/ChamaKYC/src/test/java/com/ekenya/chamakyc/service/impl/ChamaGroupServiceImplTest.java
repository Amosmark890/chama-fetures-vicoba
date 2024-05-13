//package com.ekenya.chamakyc.service.impl;
//
//import com.ekenya.chamakyc.dao.chama.Group;
//import com.ekenya.chamakyc.dao.chama.GroupMembership;
//import com.ekenya.chamakyc.dao.chama.Member;
//import com.ekenya.chamakyc.dao.user.Roles;
//import com.ekenya.chamakyc.dao.user.Users;
//import com.ekenya.chamakyc.repository.chama.*;
//import com.ekenya.chamakyc.repository.users.UserRepository;
//import com.ekenya.chamakyc.service.impl.constants.Channel;
//import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
//import com.ekenya.chamakyc.service.impl.functions.MapperFunction;
//import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
//import com.google.gson.Gson;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.context.support.ResourceBundleMessageSource;
//import org.springframework.http.codec.multipart.FilePart;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.Locale;
//import java.util.Optional;
//import java.util.Set;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.*;
//
//class ChamaGroupServiceImplTest {
//    @Mock
//    UserRepository userRepository;
//    @Mock
//    MemberRepository memberRepository;
//    @Mock
//    GroupRepository groupRepository;
//    @Mock
//    PublishingService publishingService;
//    @Mock
//    GroupMembersRepository groupMemberRepo;
//    @Mock
//    GroupTitlesRepository groupTitleRepo;
//    @InjectMocks
//    ChamaGroupServiceImpl chamaGroupService;
//    Gson gson;
//    @Mock
//    GroupDocumentsRepository groupDocumentsRepository;
//    @Mock
//    GroupInvitesRepository groupInvitesRepository;
//    PasswordEncoder passwordEncoder;
//    @Mock
//    FilePart filePart;
//
//    private Member member;
//    private Users user;
//    private Set<GroupMembership> groupMembershipSet;
//    private Group group;
//    private GroupMembership groupMembership;
//    private SimpleDateFormat simpleDateFormat;
//    private ResourceBundleMessageSource source;
//
//    @BeforeEach
//    void setUp() throws ParseException {
//        MockitoAnnotations.openMocks(this);
//        source = new ResourceBundleMessageSource();
//        source.setBasename("messages");
//        source.setDefaultEncoding("UTF-8");
//        source.setDefaultLocale(Locale.ENGLISH);
//        gson = new Gson();
//        chamaGroupService = new ChamaGroupServiceImpl(
//                userRepository, memberRepository, groupRepository, publishingService, groupMemberRepo, groupTitleRepo,
//                gson, null, groupDocumentsRepository, groupInvitesRepository, null,
//                null, null, null, null, null,
//                source
//        );
//        simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
//        passwordEncoder = new BCryptPasswordEncoder();
//        Roles roles = Roles.builder()
//                .name("ROLE_USER")
//                .resourceid("chama")
//                .rules("{\"reports\":[\"cancreate\",\"canview\",\"candelete\",\"canedit\"]}")
//                .build();
//
//        group = Group.builder()
//                .name("Eclectics")
//                .categoryId(1L)
//                .groupImageUrl("https://images.unsplash.com/photo-1611162617474-5b21e879e113?ixlib=rb-1.2.1&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80")
//                .build();
//
//        user = Users.builder()
//                .firstName("Alex")
//                .lastName("Maina")
//                .email("maina.alex@eclectics.io")
//                .username("tester@Alex")
//                .dateOfBirth(simpleDateFormat.parse("2022-06-21"))
//                .password(passwordEncoder.encode("tester123"))
//                .phoneNumber("254726690702")
//                .gender("Male")
//                .countryCode("254")
//                .nationalId("4352671843")
//                .nationality("Kenyan")
//                .lastLogin(new Date())
//                .profilePicUrl("https://w7.pngwing.com/pngs/281/222/png-transparent-humanoid-robot-telegram-robocup-robot-electronics-sticker-humanoid-robot-thumbnail.png")
//                .active(true)
//                .deactivatedBy(null)
//                .resourceId("chama")
//                .firstTimeLogin(true)
//                .active(true)
//                .roles(roles)
//                .build();
//        member = Member.builder()
//                .imsi("67283837462933")
//                .isregisteredmember(true)
//                .ussdplatform(false)
//                .userDeviceId("5367284546728")
//                .androidplatform(true)
//                .iosplatform(false)
//                .deactivationdate(null)
//                .esbwalletaccount("Wadt35367464376")
//                .walletexists(true)
//                .users(user)
//                .build();
//        groupMembership = GroupMembership
//                .builder()
//                .group(group)
//                .activemembership(true)
//                .deactivationreason(null)
//                .members(member)
//                .isrequesttoleaveactedon(false)
//                .build();
//
//        groupMembershipSet = Set.of(groupMembership);
//    }
//
//    @Test
//    void testCreateGroup_failWhenMemberNotFound() {
//        when(chamaGroupService.findChamaMemberByUserPhone("2547000101001")).thenReturn(Optional.empty());
//        Mono<UniversalResponse> createGroupRes = chamaGroupService.createGroup("", "", "", "", 0L, "", filePart);
//        StepVerifier
//                .create(createGroupRes)
//                .assertNext((r) -> {
//                    assertThat(r.getStatus().equals("fail")).isTrue();
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void testCreateGroup_failWhenMemberHasCreatedMoreThanThreeGroups() {
//        when(userRepository.findByPhoneNumberAndChannel("", "")).thenReturn(Optional.of(user));
//        when(memberRepository.findByUserId(0L)).thenReturn(Optional.of(member));
//        when(chamaGroupService.findChamaMemberByUserPhone("254712345678")).thenReturn(Optional.of(member));
//        when(groupRepository.countAllByCreator(new Member())).thenReturn(4L);
//        when(groupRepository.findByCreatorAndNameLike(new Member(), "")).thenReturn(Optional.of(group));
////        when(groupMemberRepo.findByMembersAndGroup(any(Member.class), any(Group.class))).thenReturn(Optional.of(groupMembership));
////        doNothing().when(chamaGroupService).addGroupMember(any(Member.class), any(Group.class), anyString());
//        Mono<UniversalResponse> createGroupRes = chamaGroupService.createGroup("", "", "", "", 0L, "", filePart);
//        StepVerifier
//                .create(createGroupRes)
//                .assertNext((r) -> {
//                    assertThat(r.getStatus().equals("fail")).isTrue();
//                })
//                .verifyComplete();
//    }
//
//    @Test
//    void testCreateGroup_failWhenThereIsAnotherGroupWithTheNameCreatedByTheMember() {
//        when(userRepository.findByPhoneNumberAndChannel("", "")).thenReturn(Optional.of(user));
//        when(memberRepository.findByUserId(0L)).thenReturn(Optional.of(member));
//        when(chamaGroupService.findChamaMemberByUserPhone("254712345678")).thenReturn(Optional.of(member));
//        when(groupRepository.countAllByCreator(new Member())).thenReturn(3L);
//        when(groupRepository.findByCreatorAndNameLike(new Member(), "")).thenReturn(Optional.of(group));
////        when(groupMemberRepo.findByMembersAndGroup(any(Member.class), any(Group.class))).thenReturn(Optional.of(groupMembership));
////        doNothing().when(chamaGroupService).addGroupMember(any(Member.class), any(Group.class), anyString());
//        Mono<UniversalResponse> createGroupRes = chamaGroupService.createGroup("", "", "", "", 0L, "", filePart);
//        StepVerifier
//                .create(createGroupRes)
//                .assertNext((r) -> {
//                    assertThat(r.getStatus().equals("fail")).isTrue();
//                })
//                .verifyComplete();
//    }
//}