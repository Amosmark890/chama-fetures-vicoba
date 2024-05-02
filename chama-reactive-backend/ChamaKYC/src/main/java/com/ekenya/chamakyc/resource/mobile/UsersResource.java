package com.ekenya.chamakyc.resource.mobile;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.Optional;

@RequestMapping("/api/v2/kyc/user")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UsersResource {
    private final ChamaUserService chamaUserService;
    private final ChamaGroupService chamaGroupService;

    @PostMapping("/pin-validation")
    public Mono<UniversalResponse> pinValidation(@RequestBody PinValidationWrapper pinValidationWrapper) {
        return chamaUserService.vicobaPinValidation(pinValidationWrapper.getPass(), pinValidationWrapper.getPhoneNumber());
    }

    @GetMapping("/account-status")
    public Mono<ResponseEntity<UniversalResponse>> getAccountStatus(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.getMemberDetails(username, channel))
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/vicoba")
    public Mono<ResponseEntity<UniversalResponse>> lookupVicobaUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper,
                                                                    @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.lookupVicobaUser(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/refresh-linked-accounts")
    public Mono<ResponseEntity<UniversalResponse>> refreshVicobaUserLinkedAccounts() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(chamaUserService::refreshVicobaUserLinkedAccounts)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/app-user")
    public Mono<ResponseEntity<UniversalResponse>> createAppUser(@RequestBody @Valid NewUser newUser,
                                                                 @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.createAppUser(newUser, channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PutMapping("/update")
    public Mono<ResponseEntity<UniversalResponse>> updateAppUser(@RequestBody @Valid NewUser newUser,
                                                                 @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.updateUser(newUser, channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/deactivate-account")
    public Mono<ResponseEntity<?>> deactivateAccount(ActivationWrapper activationWrapper,
                                                     @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.deactivateAccount(activationWrapper, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update-phone-email")
    public Mono<ResponseEntity<?>> editAccountPhoneAndEmail(
            @RequestBody EditphoneandemailWrapper editphoneandemailwrapper,
            @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.editPhoneandEmail(editphoneandemailwrapper.getOldphone(),
                        editphoneandemailwrapper.getNewphone(), editphoneandemailwrapper.getNewemail(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update-password")
    public Mono<ResponseEntity<?>> updatePassword(
            @RequestBody @Valid PasswordUpdater passwordUpdater,
            @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.updateUserPassword(passwordUpdater.getPhonenumber(),
                        Integer.parseInt(passwordUpdater.getOtp()), passwordUpdater.getPassword(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/forgot-password")
    public Mono<ResponseEntity<?>> forgotPassword(@RequestParam Optional<String> account,
                                                  @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return Mono.fromCallable(() -> {
                    String phoneNumber;
                    if (account.isEmpty()) {
                        phoneNumber = SecurityContextHolder.getContext().getAuthentication().getName();
                    } else {
                        phoneNumber = account.get();
                    }
                    return chamaUserService.forgotPassword(phoneNumber, channel);
                })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/verify-otp")
    public Mono<ResponseEntity<?>> verifyPasswordOTP(@RequestBody @Valid VerifyOtpWrapper wrapper,
                                                     @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.verifyOtp(wrapper.getOtp(), wrapper.getPhoneNumber(), "FORGOTPASSWORD", channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/verify-device-otp")
    public Mono<ResponseEntity<?>> verifyDeviceOTP(
            @RequestBody @Valid VerifyOtpWrapper wrapper,
            @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.verifyOtp(wrapper.getOtp(), wrapper.getPhoneNumber(), "DEVICEVERIFICATION", channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/member-details")
    public Mono<ResponseEntity<?>> getMemberDetails(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.getMemberDetails(username, channel))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/create-wallet")
    public Mono<ResponseEntity<?>> createUserWallet(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.createUserWalletAccount(username, channel))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/upload-profile", consumes = {MediaType.IMAGE_PNG_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public Mono<ResponseEntity<?>> uploadUserProfileImage(
            @RequestPart("profile_picture") FilePart filepart,
            @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.uploadUserprofile(filepart, username, channel))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/profile")
    public Mono<ResponseEntity<?>> getProfilePicture(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username ->  chamaUserService.retrieveUserprofileImage(username, channel));
    }

    @PostMapping(value = "/deactivate-group-account")
    public Mono<ResponseEntity<?>> deactivateGroupAccount(@RequestBody GroupMembershipWrapper groupMembership) {
        return chamaGroupService.deactivateGroupAccount(groupMembership)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/activate-group-account")
    public Mono<ResponseEntity<?>> activateUserGroupAccount(@RequestBody GroupMembershipWrapper groupMembership) {
        return chamaGroupService.activateUserGroupAccount(groupMembership)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/logout")
    public Mono<ResponseEntity<?>> logoutUser() {
        return Mono.fromCallable(() -> {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(chamaUserService::logoutUser);
        })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

    }

    @PostMapping(value = "/query-user")
    public Mono<ResponseEntity<?>> queryUser(@RequestBody String phone,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.userLookUp(phone, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/request-password")
    public Mono<ResponseEntity<?>> requestOtp(@RequestParam String phone,
                                              @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.requestFirstTimePassword(phone, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

    }

    @PostMapping(value = "/resend-otp/{type}")
    public Mono<ResponseEntity<?>> resendOtp(@PathVariable String type, @RequestParam String phone,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.resendOtpSms(phone, type, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/request-device-otp")
    public Mono<ResponseEntity<?>> deviceOtp(@RequestParam String phone,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.deviceVerificationOtp(phone, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/set-user-password")
    public Mono<ResponseEntity<?>> setUserPassword(@RequestBody NewUser newUser,
                                                   @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.setUserPassword(newUser, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
