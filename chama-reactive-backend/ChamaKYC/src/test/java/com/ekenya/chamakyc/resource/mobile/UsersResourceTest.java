package com.ekenya.chamakyc.resource.mobile;

import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.NewUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.security.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.Date;

@ExtendWith(MockitoExtension.class)
@WebFluxTest(controllers = UsersResource.class, excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class,
        ReactiveManagementWebSecurityAutoConfiguration.class})
class UsersResourceTest {

    @MockBean
    ChamaUserService chamaUserService;
    @MockBean
    ChamaGroupService chamaGroupService;
    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAccountStatus() {
    }

    @Test
    void createAppUser() {
        NewUser newUser = new NewUser();
        newUser.setEmail("test@test.com");
        newUser.setFirstname("Jane");
        newUser.setOthernames("Doe");
        newUser.setNationality("Kenya");
        newUser.setIdentification("12345678");
        newUser.setPhonenumber("2547123456");
        newUser.setDateofbirth(new Date());
        newUser.setGender("female");
        newUser.setLanguage("English");

        Member createdMember = new Member();
        createdMember.setIsregisteredmember(true);
        createdMember.setUserDeviceId(newUser.getUserDeviceId());
        createdMember.setUsers(new Users());
        createdMember.setImsi(newUser.getPhonenumber());

        Mockito.when(chamaUserService.createAppUser(newUser, Channel.APP))
                .thenReturn(Mono.just(new UniversalResponse("success", "member created successfully", createdMember)));

        webTestClient
                .post()
                .uri("/api/v2/kyc/user/app-user")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromObject(newUser))
                .exchange()
                .expectStatus().is2xxSuccessful();

        Mockito.verify(chamaUserService, Mockito.times(1)).createAppUser(newUser, Channel.APP);
    }

    @Test
    void updateAppUser() {
    }

    @Test
    void deactivateAccount() {
    }

    @Test
    void editAccountPhoneAndEmail() {
    }

    @Test
    void updatePassword() {
    }

    @Test
    void forgotPassword() {
    }

    @Test
    void verifyPasswordOTP() {
    }

    @Test
    void verifyDeviceOTP() {
    }

    @Test
    void getMemberDetails() {
    }

    @Test
    void createUserWallet() {
    }

    @Test
    void uploadUserProfileImage() {
    }

    @Test
    void getProfilePicture() {
    }

    @Test
    void deactivateGroupAccount() {
    }

    @Test
    void activateUserGroupAccount() {
    }

    @Test
    void logoutUser() {
    }

    @Test
    void queryUser() {
    }

    @Test
    void requestOtp() {
    }

    @Test
    void resendOtp() {
    }

    @Test
    void deviceOtp() {
    }

    @Test
    void setUserPassword() {
    }
}