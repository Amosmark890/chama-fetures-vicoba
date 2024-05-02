package com.ekenya.chamakyc.resource.portal;

import com.ekenya.chamakyc.service.Interfaces.DashboardValuesService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.Date;

import static com.ekenya.chamakyc.util.StringConstantsUtil.*;

@Validated
@RestController
@RequestMapping("/portal/kyc")
@RequiredArgsConstructor
public class DashboardResource {

    private final DashboardValuesService dashboardValuesService;

    @GetMapping("/group-registration-trend")
    public Mono<ResponseEntity<?>> getGroupRegistrationTrend(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, years")
            @RequestParam(name = "period", defaultValue = "monthly")
            @Size(max = 7, message = "The ")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Period cannot contain special characters or digits")
            String period,
            @ApiParam(value = "group name, default all")
            @RequestParam(name = "group", defaultValue = "all")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group,
            @ApiParam(value = "country , default kenya")
            @RequestParam(name = "country", defaultValue = "kenya")
            @Size(max = 20, message = "Country cannot have a length greater than 25")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Country cannot have special characters or digits")
            String country
    ) {
        return dashboardValuesService.groupRegistrationTrends(startDate, endDate, period, group, country.trim())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/groupdash")
    public Mono<ResponseEntity<?>> groupDashData() {
        return dashboardValuesService.getPortalGroupValues()
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group")
    @ApiOperation(value = "Get group reports")
    public Mono<ResponseEntity<?>> getGroupReports(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, yearly")
            @RequestParam(name = "period", defaultValue = "days")
            @Size(max = 10, message = "Period length cannot be of length greater than 10")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Period cannot contain capital letters, special characters or digits.")
            String period,
            @ApiParam(value = "status ")
            @RequestParam(name = "status", defaultValue = "true")
            boolean status,
            @ApiParam(value = "group")
            @RequestParam(name = "group", defaultValue = "all")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "1")
            int page
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return dashboardValuesService.getGroupQueryByType(startDate, endDate, period, status, pageable)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/members")
    @ApiOperation(value = "Get member reports")
    public Mono<ResponseEntity<?>> getMemberReports(
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("startdate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date startDate,
            @ApiParam(value = "takes the format dd-MM-yyyy ")
            @RequestParam("enddate")
            @DateTimeFormat(pattern = "dd-MM-yyyy")
            Date endDate,
            @ApiParam(value = " days,weeks,monthly, yearly")
            @RequestParam(name = "period", defaultValue = "days")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Period cannot contain capital letters, special characters or digits.")
            String period,
            @ApiParam(value = "status ")
            @RequestParam(name = "status", defaultValue = "true")
            boolean status,
            @ApiParam(value = "group")
            @RequestParam(name = "group", defaultValue = "all")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Group filter cannot contain capital letters, special characters or digits.")
            String group,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "1")
            int page
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return dashboardValuesService.getMemberQueryByType(startDate, endDate, period, status, group, pageable)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/member-search")
    public Mono<ResponseEntity<?>> searchForAppUsers(
            @ApiParam(value = "First name")
            @RequestParam(value = "firstname", defaultValue = "")
            @Size(max = 15, message = "First name cannot be of length more than 15")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "First name cannot contain special characters and numbers")
            String firsName,
            @ApiParam(value = "Other names")
            @RequestParam(value = "othername", defaultValue = "")
            @Size(max = 15, message = "Other name cannot be of length more than 15")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Other name cannot contain special characters and numbers")
            String otherNames,
            @ApiParam(value = "Email")
            @RequestParam(value = "email", defaultValue = "", required = false)
            @Pattern(regexp = EMPTY_OR_EMAIL_REGEX, message = "Provide a valid email!")
            String email,
            @ApiParam(value = "Phone number")
            @RequestParam(value = "phonenumber", defaultValue = "")
            @Pattern(regexp = EMPTY_OR_PHONE_NUMBER_MATCH, message = "Phone number should be of size at least 10 - 12")
            String phoneNumber,
            @ApiParam(value = "Gender")
            @RequestParam(value = "gender", defaultValue = "")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Gender should not contain special characters and numbers")
            String gender,
            @ApiParam(value = "Status")
            @RequestParam(value = "status", defaultValue = "")
            @Size(max = 10, message = "Status length should not exceed length 10")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Status should not contain special characters")
            String status,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        return dashboardValuesService.searchForAppUsers(firsName, otherNames, gender, email, phoneNumber, status, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/admin-search")
    public Mono<ResponseEntity<?>> searchForPortalUsers(
            @ApiParam(value = "First name")
            @RequestParam(value = "firstname", defaultValue = "", required = false)
            @Size(max = 15, message = "First name cannot be of length more than 15")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "First name cannot contain special characters and numbers")
            String firstName,
            @ApiParam(value = "Other names")
            @RequestParam(value = "othername", defaultValue = "", required = false)
            @Size(max = 15, message = "Other name cannot be of length more than 15")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Other name cannot contain special characters and numbers")
            String otherNames,
            @ApiParam(value = "Email")
            @RequestParam(value = "email", defaultValue = "", required = false)
            @Pattern(regexp = EMPTY_OR_EMAIL_REGEX, message = "Provide a valid email!")
            String email,
            @ApiParam(value = "Phone number")
            @RequestParam(value = "phonenumber", defaultValue = "", required = false)
            @Pattern(regexp = EMPTY_OR_PHONE_NUMBER_MATCH, message = "Phone number should be of size at least 10 - 12")
            String phoneNumber,
            @ApiParam(value = "Gender")
            @RequestParam(value = "gender", defaultValue = "", required = false)
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Gender should not contain special characters and numbers")
            String gender,
            @ApiParam(value = "Status")
            @RequestParam(value = "status", defaultValue = "", required = false)
            @Size(max = 10, message = "Status length should not exceed length 10")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Status should not contain special characters")
            String status,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        return dashboardValuesService.searchForPortalUsers(firstName, otherNames, gender, email, phoneNumber, status, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/group-search")
    public Mono<ResponseEntity<?>> searchGroup(
            @ApiParam(value = "Group name")
            @RequestParam(value = "name", defaultValue = "")
            @Pattern(regexp = EMPTY_OR_WORD, message = "Group name should not contain special characters and digits")
            String groupName,
            @ApiParam(value = "Group core account")
            @RequestParam(value = "cbsaccount", defaultValue = "")
            @Size(max = 15, message = "Provide a valid phone number")
            @Pattern(regexp = CBS_ACCOUNT_MATCH, message = "CBS account should not contain alphabets and special characters")
            String cbsAccount,
            @ApiParam(value = "Creator name")
            @RequestParam(value = "creator", defaultValue = "")
            @Pattern(regexp = EMPTY_OR_UPPER_AND_LOWER_CASE_MATCH, message = "Creator name should not contain special characters and digits")
            String createdBy,
            @ApiParam(value = "Creator phone number")
            @RequestParam(value = "creatorphone", defaultValue = "")
            @Size(max = 12, message = "Phone number cannot be longer than 12 digits")
            @Pattern(regexp = EMPTY_OR_PHONE_NUMBER_MATCH, message = "Provide a valid phone number")
            String createdByPhone,
            @ApiParam(value = "Status")
            @RequestParam(value = "status", defaultValue = "")
            @Size(max = 8, message = "The status can eitther be active or inactive")
            @Pattern(regexp = EMPTY_OR_LOWER_CASE_MATCH, message = "Status cannot contain capital letters, digits and special characters")
            String status,
            @ApiParam(value = "Date of creation takes the format dd-MM-yyyy HH:mm:ss ")
            @RequestParam(value = "createdonstart", defaultValue = "")
            @DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
            String createdOnStart,
            @ApiParam(value = "Date of creation takes the format dd-MM-yyyy HH:mm:ss ")
            @RequestParam(value = "createdonend", defaultValue = "")
            @DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
            String createdOnEnd,
            @ApiParam(name = "size", value = "size of records returned")
            @RequestParam(name = "size", defaultValue = "20")
            int size,
            @ApiParam(name = "page", value = "page of records to be returned")
            @RequestParam(name = "page", defaultValue = "0")
            int page
    ) {
        return dashboardValuesService.searchForGroup(groupName, createdBy, createdByPhone, cbsAccount, status, createdOnStart, createdOnEnd, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }
}
