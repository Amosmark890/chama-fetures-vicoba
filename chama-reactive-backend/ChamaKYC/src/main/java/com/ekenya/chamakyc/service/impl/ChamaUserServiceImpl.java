package com.ekenya.chamakyc.service.impl;


import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.dao.chama.Otp;
import com.ekenya.chamakyc.dao.user.Roles;
import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.repository.chama.GroupInvitesRepository;
import com.ekenya.chamakyc.repository.chama.MemberRepository;
import com.ekenya.chamakyc.repository.chama.OtpRepository;
import com.ekenya.chamakyc.repository.users.RolesRepository;
import com.ekenya.chamakyc.repository.users.UserRepository;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.Interfaces.FileHandlerService;
import com.ekenya.chamakyc.service.Interfaces.NotificationService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.service.impl.constants.SMS_TYPES;
import com.ekenya.chamakyc.service.impl.events.interfaces.PublishingService;
import com.ekenya.chamakyc.service.impl.functions.MapperFunction;
import com.ekenya.chamakyc.util.MaskUtils;
import com.ekenya.chamakyc.util.Utils;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import com.ekenya.chamakyc.wrappers.response.AuthResponse;
import com.ekenya.chamakyc.wrappers.response.GroupInvitesWrapper;
import com.ekenya.chamakyc.wrappers.response.PinValidationMetadata;
import com.ekenya.chamakyc.wrappers.response.UsersDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.ekenya.chamakyc.util.StringConstantsUtil.ACTIVATION_URL;
import static com.ekenya.chamakyc.util.Utils.mapToRequestBody;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamaUserServiceImpl implements ChamaUserService {

    @Value("${vicoba.url}")
    private String vicobaUrl;
    @Value("${vicoba.portal}")
    private String portalUrl;

    private final Gson gson = new Gson();
    @Value("${auth.server.basic}")
    private String authServerAuthorization;

    private final UserRepository userRepository;
    private final FileHandlerService fileHandlerService;
    private final MemberRepository memberRepository;
    private final RolesRepository rolesRepository;
    private final PublishingService publishingService;
    private final OtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final MapperFunction mapperFunction;

    private WebClient webClient;
    @Value("${auth.server.url}")
    private String authServerUrl;
    private WebClient authServerWebClient;
    private final ChamaGroupService chamaGroupService;
    private final GroupInvitesRepository groupInvitesRepository;
    private final ResourceBundleMessageSource source;
    private Users users;

    /**
     * Initialize the Webclient with the Vicoba url
     */
    @PostConstruct
    public void init() {
        webClient = WebClient.builder()
                .baseUrl(vicobaUrl)
                .build();
        authServerWebClient = WebClient.builder()
                .baseUrl(authServerUrl)
                .build();
    }

    private String getResponseMessage(String tag) {
        Locale locale = LocaleContextHolder.getLocale();
        return source.getMessage(tag, null, locale);
    }

    @Override
    public Mono<UniversalResponse> findAllPortalUsers(Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return Mono.fromCallable(() -> {
            Page<Users> users = userRepository.findAllByChannel(Channel.PORTAL.name(), pageable);
            List<UsersDetails> usersDetailsList = users.getContent().parallelStream()
                    .map(user -> mapperFunction.mapToUserDetails().apply(user))
                    .collect(Collectors.toList());
            Map<String, Number> metadata = Map.of(
                    "page", users.getNumber(),
                    "numOfRecords", userRepository.countByChannel(Channel.PORTAL.name()),
                    "totalPages", users.getTotalPages());
            return UniversalResponse.builder()
                    .status("Success")
                    .message("Portal user details")
                    .timestamp(new Date())
                    .data(usersDetailsList)
                    .metadata(metadata)
                    .build();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> vicobaPinValidation(String pin, String phoneNumber) {
        Map<String, String> pinValidationReq = new HashMap<>();
        pinValidationReq.put("0", "0200");
        pinValidationReq.put("2", phoneNumber);
        pinValidationReq.put("3", "101010");
        pinValidationReq.put("4", "0");
        pinValidationReq.put("24", "MM");
        pinValidationReq.put("25", pin);
        pinValidationReq.put("32", "VICOBA");
        pinValidationReq.put("65", "PIN_VALIDATION");
        pinValidationReq.put("102", phoneNumber);

        String body = gson.toJson(pinValidationReq);

        return Mono.fromCallable(() -> {
            // get the groups member is part of and their invites
            Optional<Member> memberOptional = memberRepository.findMemberByEsbwalletaccount(phoneNumber);

            if (memberOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("youAreNotAVicobaMember"));

            List<GroupMembershipWrapper> groupsMemberBelongsTo =
                    chamaGroupService.getGroupsMemberBelongsTo(memberOptional.get(), PageRequest.of(0, 50));

            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .flatMap(json -> {
                        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
                        log.info("Pin validation response => {}", json);

                        if (jsonObject.get("48").getAsString().equals("Failed")) {
                            String message = jsonObject.get("54").getAsString();
                            boolean isBlocked = message.toLowerCase().contains("blocked");
                            if (isBlocked==true){
                                blockVicobaUserPass(phoneNumber);
                            }


                            log.info("Using cached credentials:::");
                            return requestToken(pin, phoneNumber, groupsMemberBelongsTo, memberOptional.get())
                                    .flatMap(res -> {
                                        if (res.getStatus().equals("fail")) {
                                            log.info("Logging in failed using cached credentials:::");
                                            return Mono.just(new UniversalResponse("fail", message, Map.of("isBlocked", isBlocked)));
                                        }

                                        log.info("Logged in successfully using cached credentials:::");
                                        return Mono.just(res);
                                    });
                        }

                        // persist the PIN
                        updateVicobaUserPass(pin, phoneNumber);
                        return requestToken(pin, phoneNumber, groupsMemberBelongsTo, memberOptional.get());
                    })
                    .onErrorResume(t -> {
                        log.info("The error => {}", t.getMessage());
                        return requestToken(pin, phoneNumber, groupsMemberBelongsTo, memberOptional.get());
                    })
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<UniversalResponse> requestToken(String pin, String phoneNumber, List<GroupMembershipWrapper> groupsMemberBelongsTo, Member member) {
        // request token from auth server
        MultiValueMap<String, String> authInfo = new LinkedMultiValueMap<>();
        authInfo.add("grant_type", "password");
        authInfo.add("username", phoneNumber);
        authInfo.add("password", pin);

        return authServerWebClient
                .post()
                .uri("/oauth/token")
                .header(HttpHeaders.AUTHORIZATION, authServerAuthorization)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(authInfo))
                .retrieve()
                .bodyToMono(AuthResponse.class)
                .doOnError(t -> log.error(t.getLocalizedMessage(), t))
                .publishOn(Schedulers.boundedElastic())
                .flatMap(res -> {
                    if (res.getStatus() != 200)
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("failedToLogin"), res, new Date(), null));

                    List<GroupInvitesWrapper> invites = groupInvitesRepository.findUserInvites(phoneNumber, "active");

                    String accounts = member.getLinkedAccounts() == null ? "" : member.getLinkedAccounts();
                    PinValidationMetadata metadata = new PinValidationMetadata(accounts, invites, groupsMemberBelongsTo);
                    return Mono.just(new UniversalResponse("success", getResponseMessage("loggedIn"), res, new Date(), metadata));
                })
                .onErrorResume(t -> Mono.just(new UniversalResponse("fail", getResponseMessage("couldNotLogin"))));
    }

    @Async
    void updateVicobaUserPass(String pin, String phoneNumber) {
        Optional<Member> member = findMemberByUserPhone(phoneNumber, Channel.APP);

        if (member.isEmpty()) return;

        Users user = member.get().getUsers();
        String encodedPass = passwordEncoder.encode(pin);
        user.setPassword(encodedPass);
        user.setBlocked(false);
        user.setActive(true);

        userRepository.save(user);
    }

    //block user and set active false

    @Async
    void blockVicobaUserPass( String phoneNumber) {
        Optional<Member> member = findMemberByUserPhone(phoneNumber, Channel.APP);

        if (member.isEmpty()) return;

        Users user = member.get().getUsers();
        user.setBlocked(true);
        user.setActive(false);
        userRepository.save(user);
    }

    /**
     * Refresh user details with DCB wallet account.
     * Used to get any updated linked accounts.
     *
     * @return a message, status and data if any
     */
    @Override
    public Mono<UniversalResponse> refreshVicobaUserLinkedAccounts(String phoneNumber) {
        return Mono.fromCallable(() -> {
            Optional<Member> memberOptional = memberRepository.findByImsi(phoneNumber);

            if (memberOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Map<String, String> accountLookup = mapToRequestBody("userLookup", phoneNumber);

            String body = gson.toJson(accountLookup);

            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(json -> updateMemberLinkedAccounts(memberOptional, json))
                    .publishOn(Schedulers.boundedElastic())
                    .doOnError(t -> log.error("Error refreshing user linked accounts... Reason => {}", t.getLocalizedMessage()))
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("couldNotRefreshAccountDetails")))
                    .block();
        }).publishOn(Schedulers.boundedElastic());
    }

    private UniversalResponse updateMemberLinkedAccounts(Optional<Member> memberOptional, String json) {
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        if (jsonObject.get("48").getAsString().equals("Successful")) {
            // update user linked accounts
            VicobaUserRequest vicobaUserRequest = new Gson().fromJson(jsonObject.get("54").getAsJsonObject(), VicobaUserRequest.class);
            Member member = memberOptional.get();
            member.setLinkedAccounts(vicobaUserRequest.getLinkedAccounts());
            memberRepository.save(member);

            return new UniversalResponse("success", getResponseMessage("accountRefreshed"),
                    Map.of("hasLinkedAccounts", !vicobaUserRequest.getLinkedAccounts().isEmpty(),
                            "linkedAccounts", vicobaUserRequest.getLinkedAccounts()));
        } else
            return new UniversalResponse("fail", "Dial *150*85# to self register.", Map.of("isExisting", false));
    }

    /**
     * Lookup user with DCB wallet account.
     * Persist their data if they exist.
     *
     * @return a message, status and data if any
     */
    @Override
    public Mono<UniversalResponse> lookupVicobaUser(String phoneNumber, Channel channel) {

        if (findMemberByUserPhone(phoneNumber, channel).isPresent())
            return Mono.just(new UniversalResponse("success", getResponseMessage("userExists"), Map.of("isExisting", true)));

        //create esb body
        Map<String, String> accountLookup = mapToRequestBody("userLookup", phoneNumber);

        try {
            String body = gson.toJson(accountLookup);

            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(json -> {
                        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

                        if (jsonObject.get("48").getAsString().equals("Successful")) {
                            // save new user
                            NewUser newUser = getNewUser(jsonObject);

                            createVicobaUser(newUser, channel);

                            return Mono.just(new UniversalResponse("success", getResponseMessage("dcbWalletAccountFound"), Map.of("isExisting", true)));
                        } else
                            return Mono.just(new UniversalResponse("fail", getResponseMessage("dcbWalletAccountNotFound"), Map.of("isExisting", false)));
                    })
                    .onErrorReturn(new UniversalResponse("fail", getResponseMessage("couldNotProcessRequest")))
                    .publishOn(Schedulers.boundedElastic());
        } catch (Exception e) {
            log.info("Error... {}", e.getMessage());
        }
        throw new IllegalArgumentException(getResponseMessage("notSuccessful"));
    }

    @Override
    public Mono<UniversalResponse> createVicobaUssdUser(String phoneNumber, Channel channel) {
        Optional<Member> memberOptional = findMemberByUserPhone(phoneNumber, channel);
        if (memberOptional.isPresent()) {
            Users user = memberOptional.get().getUsers();
            return Mono.just(new UniversalResponse(
                            "success",
                            getResponseMessage("memberFound"),
                            null,
                            new Date(),
                            Map.of(
                                    "phoneNumber", phoneNumber,
                                    "memberName", user.getFirstName().concat(" ").concat(user.getLastName()),
                                    "language", user.getLanguage().startsWith("En") ? "en" : "sw"
                            )
                    )
            );
        }

        Map<String, String> accountLookup = mapToRequestBody("userLookup", phoneNumber);

        try {
            String body = new ObjectMapper().writeValueAsString(accountLookup);

            return webClient
                    .post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(json -> {
                        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

                        if (jsonObject.get("48").getAsString().equals("Successful")) {
                            // save new user
                            NewUser newUser = getNewUser(jsonObject);

                            createVicobaUser(newUser, channel);

                            return Mono.just(new UniversalResponse(
                                    "success",
                                    getResponseMessage("memberFoundAndPersisted"),
                                    null,
                                    new Date(),
                                    Map.of("phoneNumber", phoneNumber, "language", newUser.getLanguage().startsWith("En") ? "en" : "sw")));
                        } else
                            return Mono.just(new UniversalResponse(
                                    "fail",
                                    getResponseMessage("dcbWalletAccountNotFound"),
                                    null,
                                    new Date(),
                                    Map.of("phoneNumber", phoneNumber)));
                    }).publishOn(Schedulers.boundedElastic());

        } catch (Exception e) {
            log.info("Error... {}", e.getMessage());
        }
        throw new IllegalArgumentException("Not successful!");
    }

    private NewUser getNewUser(JsonObject jsonObject) {
        VicobaUserRequest vicobaUserRequest = new Gson().fromJson(jsonObject.get("54").getAsJsonObject(), VicobaUserRequest.class);
        NewUser newUser = new NewUser();
        newUser.setPhonenumber(vicobaUserRequest.getPhoneNumber());
        newUser.setIdentification(vicobaUserRequest.getIdentification());
        newUser.setFirstname(vicobaUserRequest.getFirstName());
        newUser.setEmail(vicobaUserRequest.getEmail());
        newUser.setNationality(vicobaUserRequest.getNationality());
        String language = vicobaUserRequest.getLanguage().toLowerCase().startsWith("ki") ? "Kiswahili" : "English";
        newUser.setLanguage(language);
        newUser.setGender(vicobaUserRequest.getGender());
        newUser.setOthernames(vicobaUserRequest.getMiddleName().concat(" ").concat(vicobaUserRequest.getLastName()));
        newUser.setLinkedAccounts(vicobaUserRequest.getLinkedAccounts());
        return newUser;
    }

    @Async
    public void createVicobaUser(NewUser newUser, Channel channel) {
        Optional<Users> optionalMember = userRepository.findByPhoneNumberAndChannel(newUser.getPhonenumber(), channel.name());

        if (optionalMember.isPresent()) return;

        Users user = new Users();
        Member createdMember = new Member();
        user.setFirstName(newUser.getFirstname());
        user.setLastName(newUser.getOthernames());
        user.setEmail(newUser.getEmail());
        user.setDateOfBirth(newUser.getDateofbirth());
        user.setNationalId(newUser.getIdentification());
        user.setNationality(newUser.getNationality());
        user.setGender(newUser.getGender());
        user.setPhoneNumber(newUser.getPhonenumber());
        user.setActive(true);
        user.setLanguage(newUser.getLanguage());
        user.setChannel(channel.name());
        user.setResourceId("chama");
        user.setLoginAttempts(0);
        Users savedUser = userRepository.save(user);

        createdMember.setIsregisteredmember(true);
        createdMember.setUserDeviceId(newUser.getUserDeviceId());
        createdMember.setUsers(savedUser);
        createdMember.setImsi(newUser.getPhonenumber());
        createdMember.setEsbwalletaccount(newUser.getPhonenumber());
        createdMember.setWalletexists(true);
        createdMember.setActive(true);
        createdMember.setLinkedAccounts(newUser.getLinkedAccounts());
        memberRepository.save(createdMember);
        addRoles("ROLE_USER", "chama_clientid", savedUser);
    }

    @Override
    public Mono<UniversalResponse> createAppUser(NewUser newUser, Channel channel) {
        return Mono.fromCallable(() -> {
            log.info("Creating a new member...");
            Optional<Users> optionalUsers = userRepository.findByPhoneNumberAndChannel(newUser.getPhonenumber(), channel.name());
            Optional<Member> optionalMember = findMemberByUserPhone(newUser.getPhonenumber(), channel);
            if (optionalMember.isPresent() && optionalUsers.isPresent()) {
                return new UniversalResponse("fail", getResponseMessage("memberExistsWithPhone"), new ArrayList<>());
            }
            Users user = new Users();
            Member createdMember = new Member();
            user.setFirstName(newUser.getFirstname());
            user.setLastName(newUser.getOthernames());
            user.setEmail(newUser.getEmail());
            user.setDateOfBirth(newUser.getDateofbirth());
            user.setNationalId(newUser.getIdentification());
            user.setNationality(newUser.getNationality());
            user.setGender(newUser.getGender());
            user.setPhoneNumber(newUser.getPhonenumber());
            user.setActive(true);
            user.setChannel(channel.name());
            user.setResourceId("chama");
            user.setLoginAttempts(0);
            Users savedUser = userRepository.save(user);
            createdMember.setIsregisteredmember(true);
            createdMember.setUserDeviceId(newUser.getUserDeviceId());
            createdMember.setUsers(savedUser);
            createdMember.setImsi(newUser.getPhonenumber());
            memberRepository.save(createdMember);
            addRoles("ROLE_USER", "chama_clientid", savedUser);
            publishingService.publishMemberWalletInfo(user.getPhoneNumber(), user.getNationalId());
            return new UniversalResponse("success", getResponseMessage("memberCreatedSuccessfully"), createdMember);
        }).publishOn(Schedulers.boundedElastic());
    }

    private void addRoles(String rolename, String resourceid, Users user) {
        Optional<Roles> role = rolesRepository.findByNameAndResourceid(rolename, resourceid);

        if (role.isEmpty()) {
            log.info("Role not found...");
            return;
        }

        user.setRoles(role.get());
        userRepository.save(user);
    }

//    public Optional<Member> findMemberByUserPhone(String phone, Channel channel) {
//        Users users = userRepository.findByPhoneNumberAndChannel(phone, channel.name()).orElse(null);
//        if (users == null) return Optional.empty();
//        return memberRepository.findByUserId(users.getId());
//    }


    // Create a cache for the findByPhoneNumberAndChannel method
    Cache<String, Optional<Users>> usersCache = Caffeine.newBuilder().build();

    // Create a cache for the findByUserId method
    Cache<Long, Optional<Member>> memberCache = Caffeine.newBuilder().build();


    public Optional<Member> findMemberByUserPhone(String phone, Channel channel) {
        String cacheKey = phone + ":" + channel.name();
        Optional<Users> cachedUsers = usersCache.getIfPresent(cacheKey);
        if (cachedUsers != null) {
            log.info("User found in cache for key: " + cacheKey);
            return cachedUsers.map(Users::getId)
                    .flatMap(userId -> memberCache.get(userId, key -> {
                        log.info("Member not found in cache, fetching from database for key: " + key);
                        return memberRepository.findByUserId(userId);
                    }));
        } else {
            log.info("User not found in cache, fetching from database for key: " + cacheKey);
            Optional<Users> fetchedUsers = userRepository.findByPhoneNumberAndChannel(phone, channel.name());
            if (fetchedUsers.isPresent()) {
                usersCache.put(cacheKey, fetchedUsers);
                return fetchedUsers.map(Users::getId)
                        .flatMap(userId -> memberCache.get(userId, key -> {
                            log.info("Member not found in cache, fetching from database for key: " + key);
                            return memberRepository.findByUserId(userId);
                        }));
            } else {
                return Optional.empty();
            }
        }
    }




    @Override
    public Mono<UniversalResponse> createPortalUser(NewUser newUser, Channel channel) {
        return Mono.fromCallable(() -> {
            if (checkExistingSystemUsers(newUser.getEmail()))
                return new UniversalResponse("fail", getResponseMessage("portalUserExists"), Collections.emptyList());

            String otp = Utils.generate6DigitsOtp();
            Users portalSystem = Users.builder()
                    .firstName(newUser.getFirstname())
                    .lastName(newUser.getOthernames())
                    .dateOfBirth(newUser.getDateofbirth())
                    .gender(newUser.getGender())
                    .phoneNumber(newUser.getPhonenumber())
                    .email(newUser.getEmail())
                    .nationalId(newUser.getIdentification())
                    .nationality(newUser.getNationality())
                    .resourceId("chama_portal")
                    .channel(channel.name())
                    .active(true)
                    .firstTimeLogin(true)
                    .password(passwordEncoder.encode(otp))
                    .build();
            Users savedPortalUser = userRepository.save(portalSystem);
            addRoles("ROLE_PORTAL_USER", "chama_portal", savedPortalUser);
            sendAccountCreatedPasswordReset(otp, newUser.getEmail());
            return new UniversalResponse("success", getResponseMessage("userCreatedSuccessfully"), Collections.emptyList());
        }).publishOn(Schedulers.boundedElastic());
    }

    private void sendOTPMail(String otp, String email, String memberName) {
        String activationUrl = String.format(ACTIVATION_URL, otp);
        notificationService
                .sendEmail("Chama Password Reset Email",
                        String.format("Dear %s use this link %s to reset your password.", memberName, activationUrl), email);
    }

    private boolean checkExistingSystemUsers(String email) {
        return userRepository.existsByEmailAndChannel(email, Channel.PORTAL.name());
    }

    private String getRandomString() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    @Override
    public Mono<UniversalResponse> uploadUserprofile(FilePart filePart, String uploadedBy, Channel channel) {
        return Mono.fromCallable(() -> {
                    String filetype = Objects.requireNonNull(filePart.headers().getContentType()).getType().split("/")[0];
                    if (!filetype.equals("image")) {
                        return new UniversalResponse("fail", getResponseMessage("fileMustBeAnImage"));
                    }
                    Optional<Users> optionalUser = userRepository.findByPhoneNumberAndChannel(uploadedBy, channel.name());
                    if (optionalUser.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("userNotFound"));
                    }
                    Users user = optionalUser.get();
                    String fileName = fileHandlerService.uploadFile(filePart);
                    if (!fileName.isEmpty()) {
                        user.setProfilePicUrl(fileName);
                        userRepository.save(user);
                        return new UniversalResponse("success", getResponseMessage("imageUploadedSuccessfully"));
                    }
                    return new UniversalResponse("fail", getResponseMessage("imageUploadFailed"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ResponseEntity<?>> retrieveUserprofileImage(String account, Channel channel) {
        return Mono.fromCallable(() -> {
            Users user = userRepository.findByPhoneNumberAndChannel(account, channel.name())
                    .orElse(userRepository.findByEmail(account).orElse(null));

            if (user == null) return ResponseEntity.notFound().build();

            if (user.getProfilePicUrl() == null) return ResponseEntity.notFound().build();

            return fileHandlerService.downloadFileFromUrl(user.getProfilePicUrl()).block();
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getMemberDetails(String account, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findMemberByUserPhone(account, channel);
            Optional<Users> optionalUsers = userRepository.findByPhoneNumberAndChannel(account, channel.name());
            UniversalResponse response;
            if (optionalUsers.isEmpty() && optionalMember.isEmpty()) {
                response = new UniversalResponse("fail", getResponseMessage("userNotFound"), Collections.emptyList());
                response.setMetadata("user not found");
                return response;
            }

            Member member = optionalMember.get();
            Users user = optionalUsers.get();

            BalanceInquiry bal = balanceInquiry(user.getPhoneNumber()).block();

            UsersDetails memberDetails = new UsersDetails();
            memberDetails.setActive(user.isActive());
            memberDetails.setEmail(user.getEmail());
            memberDetails.setImsi(member.getImsi());
            memberDetails.setPhonenumber(user.getPhoneNumber());
            memberDetails.setFirstname(user.getFirstName());
            memberDetails.setDateofbirth(user.getDateOfBirth() == null ? null : user.getDateOfBirth().toString());
            memberDetails.setGender(user.getGender());
            memberDetails.setBlocked(user.isBlocked());
            memberDetails.setNationality(user.getNationality());
            memberDetails.setOthernames(user.getLastName());
            memberDetails.setIdentification(user.getNationalId());
            memberDetails.setWalletexists(member.isWalletexists());
            memberDetails.setProfilePic(fileHandlerService.getFileUrl(user.getProfilePicUrl()));
            memberDetails.setLinkedAccounts(member.getLinkedAccounts());
            if (bal.getAvailableBal() != null) {
                memberDetails.setWalletbalance(Double.parseDouble(bal.getAvailableBal()));
            } else {
                memberDetails.setWalletbalance(0);
            }

            response = new UniversalResponse("success", getResponseMessage("detailsForAppUser"), memberDetails);
            response.setMetadata(user.isActive() ? "success" : getResponseMessage("inactiveAccount"));
            return response;
        }).publishOn(Schedulers.boundedElastic());
    }

    private Mono<BalanceInquiry> balanceInquiry(String walletAccount) {
        Map<String, String> balanceInquiryReq = getBalanceInquiryReq(walletAccount);

        return webClient
                .post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gson.toJson(balanceInquiryReq))
                .retrieve()
                .bodyToMono(String.class)
                .map(jsonString -> {
                    JsonObject jsonObject = new Gson().fromJson(jsonString, JsonObject.class);

                    if (jsonObject.get("48").getAsString().equals("fail"))
                        return new BalanceInquiry();

                    return gson.fromJson(jsonObject.get("54").getAsJsonObject(), BalanceInquiry.class);
                })
                .doOnError(t -> log.error(t.getLocalizedMessage(), t))
                .onErrorReturn(new BalanceInquiry());
    }

    private Map<String, String> getBalanceInquiryReq(String account) {
        Map<String, String> balanceInquiryReq = new HashMap<>();
        balanceInquiryReq.put("0", "0200");
        balanceInquiryReq.put("2", account);
        balanceInquiryReq.put("3", "310000");
        balanceInquiryReq.put("4", "0");
        balanceInquiryReq.put("24", "MM");
        balanceInquiryReq.put("32", "VICOBA");
        balanceInquiryReq.put("65", "BI");
        balanceInquiryReq.put("102", account);

        return balanceInquiryReq;
    }

    @Override
    public Mono<UniversalResponse> updateUser(NewUser newUser, Channel channel) {
        return Mono.fromCallable(() -> {
            Users user = userRepository.findByPhoneNumberAndChannel(newUser.getPhonenumber(), channel.name())
                    .orElse(userRepository.findByEmail(newUser.getEmail()).isPresent() ? userRepository.findByEmail(newUser.getEmail()).get() : null);
            if (user == null) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), new ArrayList<>());
            }
            user.setFirstName(newUser.getFirstname());
            user.setLastName(newUser.getOthernames());
            user.setEmail(newUser.getEmail());
            user.setDateOfBirth(newUser.getDateofbirth());
            user.setNationalId(newUser.getIdentification());
            user.setNationality(newUser.getNationality());
            user.setPhoneNumber(newUser.getPhonenumber());
            user.setGender(newUser.getGender());

            Optional<Member> optionalMember = findMemberByUserPhone(newUser.getPhonenumber(), channel);
            if (optionalMember.isPresent()) {
                optionalMember.get().setIsregisteredmember(true);
                memberRepository.save(optionalMember.get());
            }
            userRepository.save(user);
            return new UniversalResponse("success", "member saved successfully", new ArrayList<>());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> updateUserPassword(String password, int otp, String phoneNumber, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findMemberByUserPhone(phoneNumber, channel);
            if (optionalMember.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"), new ArrayList<>());
            Member member = optionalMember.get();
            Users user = member.getUsers();
            Optional<Otp> optionalOtp = otpRepository.findByOtpValueAndUserAndExpiredFalseAndOtpType(String.valueOf(otp), member.getUsers(), "FORGOTPASSWORD");
            if (optionalOtp.isPresent()) {
                String sha256hex = Hashing.sha256()
                        .hashString(String.valueOf(otp), StandardCharsets.UTF_8)
                        .toString();
                if (sha256hex.equals(password))
                    return new UniversalResponse("fail", getResponseMessage("otpAndPasswordCannotBeSame"), new ArrayList<>());

                boolean matches = passwordEncoder.matches(password, user.getPassword());
                if (matches) {
                    return new UniversalResponse("fail", getResponseMessage("useNewPassword"));
                }
                user.setPassword(passwordEncoder.encode(password));
                optionalOtp.get().setExpired(true);
                userRepository.save(user);
                otpRepository.save(optionalOtp.get());
                return new UniversalResponse("success", "password update processed", new ArrayList<>());
            } else {
                return new UniversalResponse("fail", "incorrect update password details, confirm that otp matches user", new ArrayList<>());
            }
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> verifyOtp(long otp, String phonenumber, String OtpType, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findMemberByUserPhone(phonenumber, channel);
            if (optionalMember.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
            Optional<Otp> optionalOtp = otpRepository.findByOtpValueAndUserAndExpiredFalseAndOtpType(String.valueOf(otp), optionalMember.get().getUsers(), OtpType);
            if (optionalOtp.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("otpNotFound"));
            } else {
                Otp saved = optionalOtp.get();
                saved.setExpired(true);
                otpRepository.save(saved);
                return new UniversalResponse("success", getResponseMessage("otpVerified"));
            }
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> createUserWalletAccount(String phonenumber, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findMemberByUserPhone(phonenumber, channel);
            if (optionalMember.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
            }

            Member members = optionalMember.get();
            if (members.isWalletexists()) {
                return new UniversalResponse("fail", getResponseMessage("onlyOneWallet"));
            } else {
                try {
                    Users user = members.getUsers();
                    publishingService.publishMemberWalletInfo(user.getPhoneNumber(), user.getNationalId());
                } catch (Exception exception) {
                    log.info("Error sending to ESB. The Error " + exception.getMessage());
                }
                return new UniversalResponse("success", getResponseMessage("creatingUserWalletAccount"));
            }
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public void updateIMSI(String phoneNumber, String iMsi, Channel channel) {
        Optional<Member> optionalMember = findMemberByUserPhone(phoneNumber, channel);
        if (optionalMember.isEmpty()) return;
        Member member = optionalMember.get();
        member.setImsi(iMsi);
        memberRepository.save(member);
    }

    public void setOtpToExpired(Member member, String otpType) {
        Optional<Otp> optionalOtp = otpRepository.findOtpByUserAndExpiredFalseAndOtpType(member.getUsers(), otpType);
        if (optionalOtp.isPresent()) {
            Otp otp = optionalOtp.get();
            otp.setExpired(true);
            otpRepository.save(otp);
        }
    }

    @Override
    public Mono<UniversalResponse> forgotPassword(String account, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Member> optionalMember = findMemberByUserPhone(account, channel);
            if (optionalMember.isEmpty())
                return new UniversalResponse("fail", "could not find user with the provided details", Collections.emptyList());
            Member member = optionalMember.get();
            setOtpToExpired(member, SMS_TYPES.FORGOTPASSWORD.name());
            Users user = member.getUsers();
            user.setActive(false);
            userRepository.save(user);
            Otp otp = Otp.builder()
                    .user(member.getUsers())
                    .otpType(SMS_TYPES.FORGOTPASSWORD.name())
                    .otpValue(Utils.generate4DigitsOTP())
                    .build();
            String phone = user.getPhoneNumber();
            try {
                notificationService.sendOtpSms(SMS_TYPES.FORGOTPASSWORD, otp.getOtpValue(), phone, user.getLanguage(), user.getFirstName());
            } catch (Exception e){
                log.error("sending otp message failure -------{}",e.getMessage());
            }

            return new UniversalResponse("success", getResponseMessage("useOTP"), Collections.emptyList());
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> editPhoneandEmail(String oldphone, String newphone, String newemail, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Users> optionalUsers = userRepository.findByPhoneNumberAndChannel(oldphone, channel.name());
            if (optionalUsers.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
            }
            Users user = optionalUsers.get();
            user.setEmail(newemail);
            user.setPhoneNumber(newphone);
            userRepository.save(user);
            return new UniversalResponse("success", getResponseMessage("userDetailsUpdated"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> logoutUser(String auth) {
        return Mono.fromCallable(() -> {
//                    publishingService.logoutUserUsingUsername(auth);
            return new UniversalResponse("success", "User logged out successfully");
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> deactivateAccount(ActivationWrapper request, Channel channel) {
        return Mono.fromCallable(() -> {
            Optional<Users> optionalUsers = userRepository.findByPhoneNumberAndChannel(request.getPhoneNumber(), channel.name());
            if (optionalUsers.isEmpty()) optionalUsers = userRepository.findByEmail(request.getEmail());
            if (optionalUsers.isEmpty()) {
                return new UniversalResponse("fail", getResponseMessage("userNotFound"));
            }
            Users user = optionalUsers.get();
            user.setActive(false);
            userRepository.save(user);
            return new UniversalResponse("success", getResponseMessage("accountDeactivatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> userLookUp(String phone, Channel channel) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findMemberByUserPhone(phone, channel);
                    if (optionalMember.isEmpty()) {
                        return new UniversalResponse("fail", getResponseMessage("memberNotFound"), Collections.emptyList());
                    }
                    String memberName = optionalMember.get().getUsers().getFirstName() + " " + optionalMember.get().getUsers().getLastName();
                    return new UniversalResponse("success", getResponseMessage("userFound"), Map.of("phoneNumber", phone, "name", memberName.toUpperCase()));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> resendOtpSms(String phone, String otpType, Channel channel) {
        return Mono.fromCallable(() -> {
                    if (otpType.equalsIgnoreCase("registration")) {
                        return requestFirstTimePassword(phone, channel);
                    } else if (otpType.equalsIgnoreCase("password")) {
                        return forgotPassword(phone, channel);
                    } else if (otpType.equalsIgnoreCase("device")) {
                        return deviceVerificationOtp(phone, channel);
                    } else {
                        return Mono.just(new UniversalResponse("fail", getResponseMessage("badRequest")));
                    }
                })
                .flatMap(res -> res)
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> requestFirstTimePassword(String phoneNumber, Channel channel) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findMemberByUserPhone(phoneNumber, channel);
                    if (optionalMember.isEmpty())
                        return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
                    Member members = optionalMember.get();
                    Users user = members.getUsers();
                    user.setActive(true);
                    setOtpToExpired(members, SMS_TYPES.REGISTRATION.name());
                    user.setFirstTimeLogin(true);
                    Otp otp = Otp.builder()
                            .otpValue(Utils.generate4DigitsOTP())
                            .user(members.getUsers())
                            .expired(false)
                            .otpType(SMS_TYPES.REGISTRATION.name())
                            .build();
                    Otp createdOtp = otpRepository.save(otp);
                    String userNewPassword = Hashing.sha256()
                            .hashString(createdOtp.getOtpValue(), StandardCharsets.UTF_8)
                            .toString();
                    user.setPassword(passwordEncoder.encode(userNewPassword));
                    userRepository.save(user);
                    log.info("First time password... {}", otp.getOtpValue());
                    String language = user.getLanguage();
                    // use English for now
                    if (language == null || language.equals("ki")) language = "English";
                    notificationService.sendOtpSms(SMS_TYPES.REGISTRATION, user.getFirstName(), createdOtp.getOtpValue(), user.getPhoneNumber(), language);
                    return new UniversalResponse("success", getResponseMessage("otpRequestInitialized"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> deviceVerificationOtp(String phoneNumber, Channel channel) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findMemberByUserPhone(phoneNumber, channel);
                    if (optionalMember.isEmpty())
                        return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
                    Member member = optionalMember.get();
                    Users user = member.getUsers();
                    user.setActive(false);
                    setOtpToExpired(member, SMS_TYPES.DEVICEVERIFICATION.name());
                    String password = Utils.generate6DigitsOtp();
                    Otp otp = Otp.builder()
                            .user(member.getUsers())
                            .otpValue(password)
                            .otpType(SMS_TYPES.DEVICEVERIFICATION.name())
                            .expired(false)
                            .build();
                    userRepository.save(user);
                    otpRepository.save(otp);
                    notificationService.sendOtpSms(SMS_TYPES.DEVICEVERIFICATION, user.getFirstName(), otp.getOtpValue(), user.getPhoneNumber(), user.getLanguage());
                    return new UniversalResponse("success", getResponseMessage("otpRequestInitialized"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> setUserPassword(NewUser newUser, Channel channel) {
        return Mono.fromCallable(() -> {
                    Optional<Member> optionalMember = findMemberByUserPhone(newUser.getPhonenumber(), channel);
                    if (optionalMember.isEmpty())
                        return new UniversalResponse("fail", getResponseMessage("memberNotFound"));
                    Users users = optionalMember.get().getUsers();
                    boolean matches = passwordEncoder.matches(newUser.getOldPassword(), users.getPassword());
                    if (!matches)
                        return new UniversalResponse("fail", getResponseMessage("provideCorrectOldPassword"));
                    matches = passwordEncoder.matches(newUser.getNewPassword(), users.getPassword());
                    if (matches)
                        return new UniversalResponse("fail", getResponseMessage("useNewPassword"));
                    users.setPassword(passwordEncoder.encode(newUser.getNewPassword()));
                    users.setFirstTimeLogin(false);
                    userRepository.save(users);
                    return new UniversalResponse("success", getResponseMessage("passwordAddedSuccessfully"));
                })
                .publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> getAccountDetail(String account, Channel channel) {
        return Mono.fromCallable(() -> {
                    Users user = userRepository.findByPhoneNumberAndChannel(account, channel.name())
                            .orElse(userRepository.findByEmail(account).isPresent() ? userRepository.findByEmail(account).get() : null);
                    if (user == null) {
                        return new UniversalResponse("fail", getResponseMessage("userNotFound"));
                    } else {
                        return new UniversalResponse("success", getResponseMessage("accountDetails"), user);
                    }
                })
                .publishOn(Schedulers.boundedElastic());
    }

    /**
     * Update password for portal user.
     *
     * @param password the new password. To be encrypted.
     * @param username
     * @return success if the otp exists and user exists. Else return failure.
     */
    @Override
    public Mono<UniversalResponse> updatePortalUserPassword(String password, String username) {
        return Mono.fromCallable(() -> {
            Optional<Users> optionalUser = userRepository.findByEmailAndChannel(username, Channel.PORTAL.name());

            if (optionalUser.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("userNotFound"));

            Users user = optionalUser.get();
            user.setPassword(passwordEncoder.encode(password));
            user.setFirstTimeLogin(false);
            user.setActive(true);
            user.setBlocked(false);
            user.setLoginAttempts(0);

            userRepository.save(user);
            return new UniversalResponse("success", getResponseMessage("passwordUpdatedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<UniversalResponse> resetPortalUserPassword(String email, String name) {
        return Mono.fromCallable(() -> {
            Optional<Users> optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("userNotFound"));

            Users user = optionalUser.get();

            if (email.equals(name))
                return new UniversalResponse("fail", getResponseMessage("cannotRestSelfPassword"));

            String otp = Utils.generate6DigitsOtp();
            String temporaryPassword = passwordEncoder.encode(otp);
            user.setPassword(temporaryPassword);
            user.setFirstTimeLogin(true);
            user.setBlocked(false);
            user.setActive(true);
            user.setLoginAttempts(0);

            userRepository.save(user);
            sendPasswordResetMail(otp, email);
            return new UniversalResponse("success", getResponseMessage("passwordResetLinkSent"));
        }).publishOn(Schedulers.boundedElastic());
    }

    private void sendPasswordResetMail(String otp, String email) {
        notificationService
                .sendEmail("Vicoba Password Reset Email",
                        String.format("Dear Vicoba Admin,\n" +
                                "\n" +
                                "An account reset operation was initiated on your account. Please navigate to %s and use the provided credentials to login and set your new password :\n" +
                                " username : %s\n" +
                                " password : %s\n" +
                                "\n" +
                                "Best regards,\n" +
                                "The Chama24 Team", portalUrl, email, otp),
                        email);
    }

    private void sendAccountCreatedPasswordReset(String otp, String email) {
        notificationService
                .sendEmail("Vicoba Admin First Time Pin",
                        String.format("Dear Vicoba  Admin,\n" +
                                "\n" +
                                "A new account has been created for you. Please navigate to %s and use the provided credentials to login and set your new password :\n" +
                                " username : %s\n" +
                                " password : %s\n" +
                                "\n" +
                                "Best regards,\n" +
                                "The DCB Vicoba Team", portalUrl, email, otp),
                        email);
    }

    @Override
    public Mono<UniversalResponse> getChamaUsers(Integer page, Integer size) {
        return Mono.fromCallable(() -> {
                    Pageable pageable = PageRequest.of(page, size);
                    Page<Member> membersList = memberRepository.findAll(pageable);
                    List<UsersDetails> usersDetailsLIst = membersList
                            .stream()
                            .map(p -> {
                                Users user = p.getUsers();
                                UsersDetails usersDetails = new UsersDetails();
                                usersDetails.setUserid(p.getId());
                                usersDetails.setFirstname(user.getFirstName());
                                usersDetails.setOthernames(user.getLastName());
                                usersDetails.setPhonenumber(user.getPhoneNumber());
                                usersDetails.setEmail(user.getEmail());
                                usersDetails.setNationality(user.getNationality());
                                usersDetails.setGender(user.getGender());
                                usersDetails.setIdentification(user.getNationalId());
                                usersDetails.setDateofbirth(user.getDateOfBirth() != null ? user.getDateOfBirth().toString() : null);
                                usersDetails.setActive(p.isActive());
                                usersDetails.setBlocked(user.isBlocked());
                                usersDetails.setWalletbalance(0);
                                return usersDetails;
                            })
                            .collect(Collectors.toList());
                    UniversalResponse response = new UniversalResponse("success", getResponseMessage("appUserList"), usersDetailsLIst);
                    Map<String, Long> numofrecords = Map.of("numofrecords", memberRepository.count());
                    response.setMetadata(numofrecords);
                    return response;
                })
                .publishOn(Schedulers.boundedElastic());

    }

    @Override
    public Mono<UniversalResponse> createPortalUser(NewUser newUser, Channel channel, String createdBy) {
        return null;
    }

    /**
     * Check if the user has any of the provided roles.
     *
     * @param roles the list of roles
     * @return the value that indicate if the user has any of the roles
     */
    @Override
    public int verifyHasAnyRole(List<String> roles) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<Users> userOptional = findByUsername(username);
        // User not found
        if (userOptional.isEmpty()) return 0;

        Users user = userOptional.get();
        // Check if the user has any of the roles provided
        if (roles.contains(user.getRoles().getName())) return 1;

        return 0;
    }

    @Override
    public Optional<Users> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<Member> findMemberByUsername(String imsi) {
        return memberRepository.findByImsi(imsi);
    }

    @Override
    public Mono<UniversalResponse> logPasswordResetRequest(String email) {
        return null;
    }

    @Override
    public Mono<UniversalResponse> getPasswordResetRequests(int page, int size) {
        return null;
    }

    @Override
    public Mono<UniversalResponse> blockAppUser(String phoneNumber, String blockedBy, boolean block) {
        return Mono.fromCallable(() -> {
            Optional<Users> adminUserOptional = userRepository.findByEmailAndChannel(blockedBy, Channel.PORTAL.name());

            if (adminUserOptional.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("adminNotFound"));

            Optional<Users> optionalUsers = userRepository.findByPhoneNumberAndChannel(phoneNumber, Channel.APP.name());

            if (optionalUsers.isEmpty())
                return new UniversalResponse("fail", getResponseMessage("memberNotFound"));

            Users users = optionalUsers.get();
            Member member = memberRepository.findByUserId(users.getId()).orElse(null);

            if (block) {
                member.setActive(false);
                users.setActive(false);
                users.setBlocked(true);
                users.setDeactivatedBy(adminUserOptional.get());
            } else { // unblock
                member.setActive(true);
                users.setActive(true);
                users.setBlocked(false);
                users.setDeactivatedBy(adminUserOptional.get());
            }

            memberRepository.save(member);
            userRepository.save(users);

            return new UniversalResponse("success", getResponseMessage("userBlockedSuccessfully"));
        }).publishOn(Schedulers.boundedElastic());
    }

    @Override
    public void disableMember(String memberPhoneNumberInfo) {
        Mono.fromRunnable(() -> {
            JsonObject jsonObject = gson.fromJson(memberPhoneNumberInfo, JsonObject.class);
            String phoneNumber = jsonObject.get("phoneNumber").getAsString();

            Optional<Member> optionalMember = memberRepository.findByImsi(phoneNumber);

            optionalMember.ifPresentOrElse(member -> {
                member.setActive(false);
                memberRepository.save(member);
            }, () -> log.info("Member not found.... on disabling member"));
        }).publishOn(Schedulers.boundedElastic()).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}
