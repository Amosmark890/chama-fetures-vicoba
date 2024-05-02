package com.ekenya.chamakyc.service.Interfaces;


import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.ActivationWrapper;
import com.ekenya.chamakyc.wrappers.request.NewUser;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

public interface ChamaUserService {

    Mono<UniversalResponse> refreshVicobaUserLinkedAccounts(String phoneNumber);

    Mono<UniversalResponse> lookupVicobaUser(String phoneNumber, Channel channel);

    Mono<UniversalResponse> createVicobaUssdUser(String phoneNumber, Channel channel);

    Mono<UniversalResponse> createAppUser(NewUser newUser, Channel channel);

    Mono<UniversalResponse> createPortalUser(NewUser newUser, Channel channel);

    Mono<UniversalResponse> createPortalUser(NewUser newUser, Channel channel, String createdBy);

    Mono<UniversalResponse> uploadUserprofile(FilePart filePart, String uploadedBy, Channel channel);

    Mono<ResponseEntity<?>> retrieveUserprofileImage(String user, Channel channel);

    Mono<UniversalResponse> getMemberDetails(String account, Channel channel);

    Mono<UniversalResponse> updateUser(NewUser newUser, Channel channel);

    Mono<UniversalResponse> updateUserPassword(String password, int otp, String phonenumber, Channel channel);

    Mono<UniversalResponse> verifyOtp(long otp, String phonenumber, String OtpType, Channel channel);

    Mono<UniversalResponse> createUserWalletAccount(String phonenumber, Channel channel);

    void updateIMSI(String phonenumber, String imsi, Channel channel);

    Mono<UniversalResponse> forgotPassword(String account, Channel channel);

    Mono<UniversalResponse> editPhoneandEmail(String oldphone, String newphone, String newemail, Channel channel);

    Mono<UniversalResponse> logoutUser(String auth);

    Mono<UniversalResponse> deactivateAccount(ActivationWrapper request, Channel channel);

    Mono<UniversalResponse> userLookUp(String phone, Channel channel);

    Mono<UniversalResponse> resendOtpSms(String phone, String otpType, Channel channel);

    Mono<UniversalResponse> requestFirstTimePassword(String phoneNumber, Channel channel);

    Mono<UniversalResponse> deviceVerificationOtp(String phoneNumber, Channel channel);

    Mono<UniversalResponse> setUserPassword(NewUser newUser, Channel channel);

    Mono<UniversalResponse> getAccountDetail(String account, Channel channel);

    Mono<UniversalResponse> updatePortalUserPassword(String password, String username);

    Mono<UniversalResponse> resetPortalUserPassword(String email, String name);

    Mono<UniversalResponse> getChamaUsers(Integer page, Integer size);

    int verifyHasAnyRole(List<String> roles);

    Optional<Users> findByUsername(String username);

    Mono<UniversalResponse> vicobaPinValidation(String pin, String phoneNumber);

    Mono<UniversalResponse> findAllPortalUsers(Integer page, Integer size);

    Mono<UniversalResponse> logPasswordResetRequest(String email);

    Mono<UniversalResponse> getPasswordResetRequests(int page, int size);

    Mono<UniversalResponse> blockAppUser(String phoneNumber, String blockedBy, boolean block);

    void disableMember(String memberPhoneNumberInfo);
}
