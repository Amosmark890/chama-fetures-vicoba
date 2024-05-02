package com.ekenya.chamakyc.service.impl.constants;

import lombok.experimental.UtilityClass;

/**
 * Define kafka topics to be used by the Stream Bridge for
 * publishing.
 * The names conform to the Consumers in the respective services.
 *
 * @author wnganga
 * @created 15/04/2022
 */
@UtilityClass
public class KafkaChannelsConstants {

    public static final String SEND_EMAIL_TOPIC = "sendEmail-in-0";
    public static final String SEND_OTP_TOPIC = "sendOtp-in-0";
    public static final String SEND_TEXT_TOPIC = "sendVicobaText-in-0";
    public static final String CREATE_USER_WALLET_TOPIC = "createMemberWallet-in-0";

    public static final String CREATE_GROUP_WALLET_TOPIC = "createGroupWallet-in-0";
    public static final String CREATE_CONTRIBUTION_TOPIC = "createGroupContribution-in-0";
    public static final String CREATE_GROUP_ACCOUNT_TOPIC = "createGroupAccount-in-0";
    public static final String ENABLE_GROUP_CONTRIBUTIONS_TOPIC = "enableGroupContributions-in-0";
    public static final String DISABLE_GROUP_CONTRIBUTIONS_TOPIC = "disableGroupContributions-in-0";
    public static final String WRITE_OFF_LOANS_AND_PENALTIES_TOPIC = "writeOffLoansAndPenalties-in-0";
    public static final String AUDIT_LOG = "vicoba-audit-logs";
    public static final String CONTRIBUTION_NAME_EDIT_TOPIC = "contribution-name-edit-topic";
    public static final String UPDATE_GROUP_CORE_ACCOUNT = "update-group-core-account-topic";
}
