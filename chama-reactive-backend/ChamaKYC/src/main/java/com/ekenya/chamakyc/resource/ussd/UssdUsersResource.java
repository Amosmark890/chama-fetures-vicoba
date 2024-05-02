package com.ekenya.chamakyc.resource.ussd;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/kyc/ussd/user")
public class UssdUsersResource {
    private final ChamaUserService chamaUserService;
    private final ChamaGroupService chamaGroupService;

    @PostMapping("/pin-validation")
    public Mono<UniversalResponse> pinValidation(@RequestBody PinValidationWrapper pinValidationWrapper) {
        return chamaUserService.vicobaPinValidation(pinValidationWrapper.getPass(), pinValidationWrapper.getPhoneNumber());
    }

    @PostMapping("/account-status")
    public Mono<ResponseEntity<UniversalResponse>> getAccountStatus(@RequestBody PhoneNumberWrapper phoneNumberWrapper,
                                                                    @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.getMemberDetails(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/vicoba")
    public Mono<ResponseEntity<UniversalResponse>> createAppUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.lookupVicobaUser(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @GetMapping("/refresh-linked-accounts")
    public Mono<ResponseEntity<UniversalResponse>> refreshVicobaUserLinkedAccounts() {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(chamaUserService::refreshVicobaUserLinkedAccounts)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/account-lookup")
    public Mono<ResponseEntity<UniversalResponse>> lookupUssdUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper) {
        return chamaUserService.createVicobaUssdUser(phoneNumberWrapper.getPhoneNumber(), Channel.APP)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/app-user")
    public Mono<ResponseEntity<UniversalResponse>> createAppUser(@RequestBody @Valid NewUser newUser, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.createAppUser(newUser, channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/update")
    public Mono<ResponseEntity<UniversalResponse>> updateAppUser(@RequestBody @Valid NewUser newUser, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.updateUser(newUser, channel)
                .map(res -> ResponseEntity.ok().body(res));
    }

    @PostMapping("/deactivate-account")
    @ApiOperation(value = "Deactivate account", notes = "Email can be empty in this case")
    public Mono<ResponseEntity<?>> deactivateAccount(@RequestBody ActivationWrapper activationWrapper, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
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

    @PostMapping("/forgot-password")
    public Mono<ResponseEntity<?>> forgotPassword(@RequestBody PhoneNumberWrapper phoneNumberWrapper, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return Mono.fromCallable(() -> chamaUserService.forgotPassword(phoneNumberWrapper.getPhoneNumber(), channel))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/verify-otp")
    public Mono<ResponseEntity<?>> verifyPasswordOTP(@RequestBody @Valid VerifyOtpWrapper wrapper, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
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

    @PostMapping("/member-details")
    public Mono<ResponseEntity<?>> getMemberDetails(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        String phone = SecurityContextHolder.getContext().getAuthentication().getName();
        return chamaUserService.getMemberDetails(phone, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @PostMapping("/create-wallet")
    public Mono<ResponseEntity<?>> createUserWallet(@RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.createUserWalletAccount(username, channel))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
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

    @ApiIgnore
    @PostMapping(value = "/logout")
    public Mono<ResponseEntity<?>> logoutUser() {
        return Mono.fromCallable(() -> {
                    String auth = SecurityContextHolder.getContext().getAuthentication().getName();
                    return chamaUserService.logoutUser(auth);
                })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

    }

    @PostMapping(value = "/query-user")
    public Mono<ResponseEntity<?>> queryUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.userLookUp(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/request-password")
    public Mono<ResponseEntity<?>> requestOtp(@RequestBody PhoneNumberWrapper phoneNumberWrapper,
                                              @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.requestFirstTimePassword(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

    }

    @PostMapping(value = "/resend-otp")
    public Mono<ResponseEntity<?>> resendOtp(@RequestBody OtpRequestWrapper otpRequestWrapper,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.resendOtpSms(otpRequestWrapper.getPhone(), otpRequestWrapper.getType(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/request-device-otp")
    public Mono<ResponseEntity<?>> deviceOtp(@RequestBody PhoneNumberWrapper phoneNumberWrapper,
                                             @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.deviceVerificationOtp(phoneNumberWrapper.getPhoneNumber(), channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/set-user-password")
    public Mono<ResponseEntity<?>> setUserPassword(@RequestBody NewUser newUser, @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.setUserPassword(newUser, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
