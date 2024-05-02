package com.ekenya.chamakyc.resource.portal;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.request.ActivationWrapper;
import com.ekenya.chamakyc.wrappers.request.NewUser;
import com.ekenya.chamakyc.wrappers.request.PhoneNumberWrapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import static com.ekenya.chamakyc.util.StringConstantsUtil.*;

@Validated
@RestController
@RequestMapping("/portal/kyc/users")
@RequiredArgsConstructor
public class PortalUserResource {
    private final ChamaUserService chamaUserService;
    private final ChamaGroupService chamaGroupService;

    @GetMapping("/admins")
    @ApiOperation(value = "Fetch portal admins")
    public Mono<ResponseEntity<?>> fetchPortalUsers(@RequestParam Integer page, @RequestParam Integer size) {
        return chamaUserService.findAllPortalUsers(page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    /**
     * Register a new user
     *
     * @param newUser the user details
     * @return success
     */
    @PostMapping("/newuser")
    public Mono<ResponseEntity<?>> createPortalAdminUser(@RequestBody @Valid NewUser newUser) {
        return chamaUserService.createPortalUser(newUser, Channel.PORTAL)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PutMapping("/update-portal-user")
    public Mono<ResponseEntity<?>> updatePortalAdminUser(
            @RequestBody @Valid NewUser newUser) {
        if (newUser.getEmail().isEmpty())
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> {
                        newUser.setEmail(username);
                        return chamaUserService.updateUser(newUser, Channel.PORTAL);
                    }).map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

        return chamaUserService.updateUser(newUser, Channel.PORTAL)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/accountdetails")
    public Mono<ResponseEntity<?>> getAccountDetails(@RequestParam
                                                     @Size(max = 40, message = "Email cannot be of length greater than 40")
                                                     @Pattern(regexp = EMAIL_REGEX, message = "Provide a valid email address")
                                                     String email) {
        if (email == null || email.isBlank()) {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> chamaUserService.getAccountDetail(username, Channel.PORTAL))
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }

        return chamaUserService.getAccountDetail(email, Channel.PORTAL)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/updatepassword")
    public Mono<ResponseEntity<?>> updatePortalUserPassword(@RequestParam
                                                            @Size(min = 6, max = 16, message = "Password cannot be of length greater than 16 characters")
                                                            String password) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.updatePortalUserPassword(password, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/resetpassword")
    public Mono<ResponseEntity<?>> resetPortalUserPassword(@RequestParam
                                                           @Size(max = 40, message = "Email cannot be of length greater than 40")
                                                           @Pattern(regexp = EMAIL_REGEX, message = "Provide a valid email address")
                                                           String email) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.resetPortalUserPassword(email, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/deleteaccount")
    public Mono<ResponseEntity<?>> deactivatePortalAccount(@RequestParam
                                                           @Size(max = 40, message = "Email cannot be of length greater than 40")
                                                           @Pattern(regexp = EMAIL_REGEX, message = "Provide a valid email address")
                                                           String email) {
        if (email == null || email.isBlank()) {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> {
                        ActivationWrapper wrapper = ActivationWrapper.builder()
                                .email(username)
                                .phoneNumber(null)
                                .build();
                        return chamaUserService.deactivateAccount(wrapper, Channel.PORTAL);
                    })
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }

        ActivationWrapper wrapper = ActivationWrapper.builder()
                .email(email)
                .phoneNumber(null)
                .build();

        return chamaUserService.deactivateAccount(wrapper, Channel.PORTAL)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @PostMapping("/appuser/reset-password")
    public Mono<ResponseEntity<?>> resetAppUserPassword(@RequestParam @Pattern(regexp = PHONE_NUMBER_MATCH, message = "Phone number cannot contain special characters and letters") String phone,
                                                        @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return chamaUserService.requestFirstTimePassword(phone, channel)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/appusers")
    public Mono<ResponseEntity<?>> getChamaAppUsers(
            @ApiParam(value = "The user phone number") @RequestParam(required = false) @Pattern(regexp = EMPTY_OR_PHONE_NUMBER_MATCH, message = "Phone number cannot contain special characters and letters") String account,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Mono.fromCallable(() -> {
                    if (account == null || account.isEmpty())
                        return chamaUserService.getChamaUsers(page, size);

                    return chamaUserService.getMemberDetails(account, Channel.APP);
                })
                .flatMap(res -> res)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/appusers/{action}")
    @ApiOperation(value = "Fetch groups a member belongs. Also can fetch a user invites.", notes = "The acceptable action is 'groups' and account is the user phone number. If the action param is missing, then invites are retrieved. The page and size should not miss in both cases.")
    public Mono<ResponseEntity<?>> getUsergroupInfo(@PathVariable String action,
                                                    @RequestParam @Size(max = 16) @Pattern(regexp = CBS_ACCOUNT_MATCH, message = "Account number cannot contain special characters and letters") String account,
                                                    @RequestParam int page,
                                                    @RequestParam int size) {
        return Mono.fromCallable(() -> {
                    if (action.equals("groups")) {
                        // return groups
                        return chamaGroupService.getGroupsbyMemberaccount(account, page, size);
                    } else {
                        // return invites
                        return chamaGroupService.getInvitesByMemberAccount(account, page, size);
                    }
                })
                .flatMap(res -> res)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @GetMapping("/appuser/reset/password")
    public Mono<ResponseEntity<?>> forgotPassword(@RequestParam @Size(max = 16) @Pattern(regexp = CBS_ACCOUNT_MATCH, message = "Account number cannot contain special characters and letters") String account) {
        return chamaUserService.forgotPassword(account, Channel.PORTAL)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @PostMapping("/req/passwordchange")
    @ApiOperation(value = "To log a forgot user password request, it will appear on the list of passsword change requests")
    public Mono<ResponseEntity<?>> logPasswordResetRequest(@RequestParam @Email(regexp = EMAIL_REGEX, message = "Please provide a valid email address") String email) {
        return chamaUserService.logPasswordResetRequest(email)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @ApiIgnore
    @GetMapping("/passwordchangerequests")
    public Mono<ResponseEntity<?>> getPasswordChangeRequests(@RequestParam int page, @RequestParam int size) {
        return chamaUserService.getPasswordResetRequests(page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/block-app-user")
    public Mono<ResponseEntity<?>> blockAppUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaUserService.blockAppUser(phoneNumberWrapper.getPhoneNumber(), username, true))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/unblock-app-user")
    public Mono<ResponseEntity<?>> unBlockAppUser(@RequestBody PhoneNumberWrapper phoneNumberWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(
                        username -> chamaUserService.blockAppUser(phoneNumberWrapper.getPhoneNumber(), username, false))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }
}
