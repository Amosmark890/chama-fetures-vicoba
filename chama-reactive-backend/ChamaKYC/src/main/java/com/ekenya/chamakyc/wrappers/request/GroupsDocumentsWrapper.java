package com.ekenya.chamakyc.wrappers.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Builder
@Getter
@Setter
public class GroupsDocumentsWrapper {
    private String name;
    private String url;
    private String uploadedBy;
}
