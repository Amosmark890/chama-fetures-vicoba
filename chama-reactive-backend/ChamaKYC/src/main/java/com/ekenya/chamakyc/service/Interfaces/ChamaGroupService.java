package com.ekenya.chamakyc.service.Interfaces;


import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.GroupReportWrapper;
import com.ekenya.chamakyc.wrappers.broker.PollResult;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChamaGroupService {
    UniversalResponse groupAccountLookupOnCBS(String account);

    Mono<UniversalResponse> createGroup(String groupname, String location, String description, String createdby_account, Long categoryId, String purpose, FilePart file);

    Mono<UniversalResponse> createGroup(GroupDetailsWrapper chamaWrapper, String createdBy);

    Mono<UniversalResponse> getMemberpermissionsInGroup(String phonenumber, long groupid);

    Mono<UniversalResponse> updateMemberpermissionsperGroup(String phonenumber, long groupid, Map<String, Object> permissions);

    Mono<UniversalResponse> approveDeclineMemberRequestToLeaveGroup(String memberPhoneNumber, Long groupId, boolean approve,
                                                                    String currentUser);

    Mono<UniversalResponse> getMembersRequestToLeaveGroup(Long groupId, String currentUser);

    Mono<UniversalResponse> deactivateGroupAccount(GroupMembershipWrapper groupMembershipWrapper);

    Mono<UniversalResponse> uploadDocuments(long groupid, FilePart multipart, String fileName, String uploadedby, Channel channel);

    Mono<ResponseEntity<?>> retrieveFile(long groupid, String filename);

    Mono<UniversalResponse> getGroupDetails(long groupid);

    Mono<UniversalResponse> getCountryData();

    Mono<UniversalResponse> readFileandsendInvites(Mono<FilePart> multipartFile, long groupid);

    Mono<UniversalResponse> getGroupsbyMemberaccount(String account, int page, int size);

    List<GroupMembershipWrapper> getGroupsMemberBelongsTo(Member members, Pageable pageable);

    Mono<UniversalResponse> getInvitesByMemberAccount(String account, int page, int size);

    Mono<UniversalResponse> getInvitesByGroup(long groupid, int page, int size);

    Mono<UniversalResponse> getAllMembersInGroup(long groupid, int page, int size);

    Mono<UniversalResponse> acceptordeclineInvite(long inviteid, String useraccount, boolean groupmember);

    Mono<UniversalResponse> exitFromGroup(long groupid, String useraccount, String reasontoleave);

    Mono<UniversalResponse> activateUserGroupAccount(GroupMembershipWrapper groupMembershipWrapper);

    Mono<UniversalResponse> updateUserGroupRole(UpdatememberpermissionsWrapper updatememberpermissionsWrapper);

    void addGroupMember(Member members, Group groups, String roletitle);

    String removeGroupmember(Member members, Group groups, String reasontoleave);

    Optional<Member> findChamaMemberByUserPhone(String phone);

    Mono<UniversalResponse> sendGroupInvites(long groupid, List<MemberRoles> phonenumbersandrole);

    Mono<UniversalResponse> sendOfficialsInvite(long groupid, List<MemberRoles> phonenumbersandrole);

    Mono<UniversalResponse> getAllMemberGroupsSummary(String phoneNumber);

    Mono<UniversalResponse> getAllGroupCategories();

    Mono<UniversalResponse> getAllGroups(Integer page, Integer size);

    GroupReportWrapper getGroupReportWrapper(Group group);

    void updateEsbGroupRegistration(long groupId);

    void updatePollResultPositions(List<PollResult> pollResultList);

    Mono<UniversalResponse> groupAccountLookup(String account, String name);

    Mono<UniversalResponse> enableGroup(Long groupId, String cbsAccount, String loggedUser);

    Mono<UniversalResponse> activateGroup(Long groupId, String loggedUser);

    Mono<UniversalResponse> disableGroup(Long groupId, String loggedUser);

    Mono<UniversalResponse> disableGroupByChairperson(Long groupId, String loggedUser);

    Mono<UniversalResponse> updateGroupName(long groupId, String name, String modifier);

    void updateGroupLeader(String pollWinnerInfo);

    Mono<UniversalResponse> getMessageTemplates(Integer page, Integer size);

    Mono<UniversalResponse> editMessageTemplate(MessageTemplateWrapper messageTemplateWrapper);

    Mono<UniversalResponse> getGroupMembersSummary(long groupId);

    Mono<UniversalResponse> findAllCountries();

    Mono<UniversalResponse> addMessageTemplate(MessageTemplateWrapper messageTemplateWrapper);

    Mono<UniversalResponse> findMemberInGroup(Long groupId, String phoneNumber);
}
