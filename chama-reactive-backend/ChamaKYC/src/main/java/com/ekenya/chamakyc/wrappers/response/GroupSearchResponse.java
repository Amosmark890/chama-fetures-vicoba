package com.ekenya.chamakyc.wrappers.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupSearchResponse {
    @JsonProperty(value = "groupId")
    private int groupid;
    @JsonProperty(value = "createdOn")
    private String createdon;
    @JsonProperty(value = "active")
    private boolean isactive;
    private String category;
    private String description;
    @JsonProperty(value = "groupImage")
    private String groupimage;
    private String location;
    private String name;
    private String purpose;
    @JsonProperty(value = "hasWallet")
    private boolean walletexists = true;
    @JsonProperty(value = "createdBy")
    private String createdby;
    @JsonProperty(value = "creatorPhone")
    private String creatorphone;
    @JsonProperty(value = "groupSize")
    private String groupsize;
    @JsonProperty(value = "cbsAccount")
    private String cbsaccount;
}
