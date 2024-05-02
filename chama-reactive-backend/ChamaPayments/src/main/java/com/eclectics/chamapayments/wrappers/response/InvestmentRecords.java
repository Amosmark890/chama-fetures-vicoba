package com.eclectics.chamapayments.wrappers.response;

import lombok.*;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InvestmentRecords {
    private long id;
    private String name;
    private double value;
    private String description;
    private String managerphone;
    private String managername;
    private String groupname;
    private long groupid;

}
