package com.ekenya.chamakyc.wrappers.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Class name: PortalGroupsWrapper
 * Creater: wgicheru
 * Date:4/1/2020
 */
@Getter
@Setter
public class PortalGroupsWrapper {
    long totalgroups;
    long activegroups;
    long inactivegroups;

    long totalcontributions;
    long activecontributions;
    long inactivecontributions;

    long totalaccounts;
    double accountvalue;
    double accountavg;

    long totalmembers;
    long activemembers;
    long inactivemembers;
}
