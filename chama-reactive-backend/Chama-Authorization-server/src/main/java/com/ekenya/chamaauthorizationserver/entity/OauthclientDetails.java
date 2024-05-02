package com.ekenya.chamaauthorizationserver.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "oauth_client_details")
@Getter
@Setter
public class OauthclientDetails implements Serializable {
    @Id
    @Column(name = "client_id",length = 64)
    String clientId;
    @Column(name = "resource_ids")
    String resourceIds;
    @Column(name = "client_secret")
    String clientSecret;
    String scope;
    @Column(name = "authorized_grant_types")
    String authorizedGrantTypes;
    @Column(name = "web_server_redirect_uri")
    String webServerRedirectUri;
    String authorities;
    @Column(name = "access_token_validity")
    int accessTokenValidity;
    @Column(name = "refresh_token_validity")
    int refreshTokenValidity;
    @Column(name = "additional_information")
    String additionalInformation;
    String autoapprove;

}
