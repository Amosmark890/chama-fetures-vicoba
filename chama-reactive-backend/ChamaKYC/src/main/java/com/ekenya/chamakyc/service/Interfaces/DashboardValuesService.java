package com.ekenya.chamakyc.service.Interfaces;

import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface DashboardValuesService {

    List<Map<Object, Object>> groupRegistrationTrend(Date startDate, Date endDate, String period, String groupName, String country);

    Mono<UniversalResponse> groupRegistrationTrends(Date startDate, Date endDate, String period, String groupName, String country);

    Mono<UniversalResponse> getPortalGroupValues();

    Mono<UniversalResponse> getGroupQueryByType(Date startDate, Date endDate, String period, boolean status, Pageable pageable);

    Mono<UniversalResponse> getMemberQueryByType(Date startDate, Date endDate, String period, boolean status, String group, Pageable pageable);

    Mono<UniversalResponse> searchForAppUsers(String firstName, String otherNames, String gender, String email, String phoneNumber, String status, int page, int size);

    Mono<UniversalResponse> searchForPortalUsers(String firstName, String otherNames, String gender, String email, String phoneNumber, String status, int page, int size);

    Mono<UniversalResponse> searchForGroup(String groupName, String createdBy, String creatorPhone, String cbsAccount, String status, String createdOnStart, String createdOnEnd, int page, int size);
}
