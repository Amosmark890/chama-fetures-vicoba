package com.eclectics.chamapayments.wrappers.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.util.Date;

/**
 * @author Alex Maina
 * @created 08/12/2021
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GroupReportWrapper {
    private long groupId;
    private String category;
    private String name;
    private String location;
    private String description;
    private boolean isActive;
    private String createdBy;
    private String creatorPhone;
    private boolean hasWallet;
    private String groupImage;
    private String purpose;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date createdOn;
    private int groupSize;
}
