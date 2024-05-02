package com.ekenya.chamaauthorizationserver;

import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ChamaAuthorizationServerApplicationTests {
    @Test
    void contextLoads() {
    }

    private final int port=8772;
    private final String baseUrl="http://localhost:"+port;

    /**
     * Target: Portal Authentication
     * Test items:  tokenExpiration, tokenResponse, authenticationPayload format
     */

    @Test
    public void testForMobileAuthentication(){
        System.out.println(baseUrl);
        RestAssured
                .given()
                    .auth().basic("chama_portal","cH@M@Por10@l!")
                    .contentType("multipart/form-data")
                    .multiPart("grant_type","password")
                    .multiPart("username","wycliffmuriithi@gmail.com")
                    .multiPart("password","password")
                .when()
                    .post( baseUrl+ "/oauth/token")
                .then()
                    .assertThat()
                    .statusCode(200)
                    .body("access_token", Matchers.notNullValue())
                    .body("token_type",Matchers.equalTo("bearer"))
                    .body("refresh_token",Matchers.notNullValue())
                    .body("expires_in",Matchers.isA(Integer.class));
    }

    /**
     * Test for wrong credentials for mobile chama client
     * Target: Error response on login
     */

    @Test
    public void testForWrongMobileAuthenticationDetails(){
        RestAssured
                .given()
                    .auth().basic("chama_portal","cH@M@Por10@l!")
                    .contentType("multipart/form-data")
                    .multiPart("grant_type","password")
                    .multiPart("username","wycliffmuriithi@gmail.com")
                    .multiPart("password","123456")
                .when()
                    .post(baseUrl + "/oauth/token")
                .then()
                    .assertThat()
                    .statusCode(400)
                    .body("error", Matchers.equalTo("invalid_grant"))
                    .body("error_description",Matchers.notNullValue());
    }

}
