package com.ekenya.chamakyc.wrappers.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class name: ChamaWrapper
 * Creater: wgicheru
 * Date:2/20/2020
 */
@Getter
@Setter
@ToString
public class ChamaWrapper {
    /**
     * The Groupname.
     */
    @NotNull(message = "field cannot be null") @NotEmpty(message = "field cannot be empty")
    String groupname, /**
     * The Location.
     */
    location,
    /**
     * The purpose.
     */
    purpose,
    /**
     * The Description.
     */
    description;

    Long categoryId;

    @Nullable
    private ContributionWrapper contributions;

    @Nullable
    @Size(max = 15, message = "Length cannot be more than 15")
    private String cbsAccount;

    @Nullable
    private double availableBalance = 0.0;

    @Nullable
    private String cbsAccountName;

    /**
     * The Invites phonenumbers.
     */
    @Nullable
   List<MemberRoles> invites_phonenumbers_and_roles;

    /**
     * Class name: ContributionWrapper
     * Creater: wgicheru
     * Date:3/12/2020
     */
    @Getter
    @Setter
    public static class ContributionWrapper {
        @NotNull(message = "name cannot be null") @NotEmpty(message = "name cannot be empty")
        private String name;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
        private Date startdate;
        private Map<String,Object> contributiondetails;
        private long contributiontypeid;
        @NotNull(message = "amount cannot be empty")
        private Long scheduletypeid;
        @NotNull(message = "amount cannot be empty")
        private Long amounttypeid;
        @NotNull(message = "group id cannot be empty")
        private Long groupid;

        private Long amount;
        private Integer reminders;
        private Double penalty;
        private String frequency;
        private Boolean ispercentage;
        private Integer duedate;
    }
}
