package com.ekenya.chamakyc.resource.ussd;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/kyc/ussd/group")
public class UssdGroupResource {
    private final ChamaGroupService chamaGroupService;
    private final Gson gson;

    @PostMapping("/vicoba/lookup")
    public Mono<ResponseEntity<?>> groupLookUp(@RequestBody AccountNumberWrapper accountNumberWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.groupAccountLookup(accountNumberWrapper.getAccount(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/create")
    public Mono<ResponseEntity<?>> createGroup(@Valid @RequestBody GroupDetailsWrapper groupDetailsWrapper) {
        log.info("Group creation body... {}", groupDetailsWrapper);
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.createGroup(groupDetailsWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update")
    @PreAuthorize("hasPermission(#groupUpdateWrapper.getGroupId(), 'group' ,@objectAction.initFields('groupaccount','canedit'))")
    @ApiOperation(value = "Update Group name", notes = "This operation can only be done by the officials.")
    public Mono<ResponseEntity<?>> editGroupName(@RequestBody GroupUpdateWrapper groupUpdateWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.updateGroupName(groupUpdateWrapper.getGroupId(), groupUpdateWrapper.getName(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/invite")
    public Mono<ResponseEntity<?>> sendGroupInvites(@Valid @RequestBody InviteWrapper inviteWrapper) {
        return chamaGroupService.sendGroupInvites(inviteWrapper.getGroupid(), List.of(new MemberRoles(inviteWrapper.getPhoneNumber(), "Member")))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/invite/officials")
    @PreAuthorize("hasPermission(#invitesWrapper.getGroupid(), 'group' ,@objectAction.initFields('groupaccount','cancreate'))")
    public Mono<ResponseEntity<?>> sendOfficialsGroupInvite(@Valid @RequestBody InvitesWrapper invitesWrapper) {
        return Mono.fromCallable(() -> chamaGroupService.sendGroupInvites(invitesWrapper.getGroupid(), invitesWrapper.getPhonenumbersandrole()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/invite/accept")
    public Mono<ResponseEntity<?>> acceptOrDeclineInvite(@RequestBody AcceptInviteWrapper acceptInviteWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .map(username -> chamaGroupService.acceptordeclineInvite(acceptInviteWrapper.getInviteId(), username,
                        acceptInviteWrapper.getAction().equalsIgnoreCase("accept")))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/info")
    public Mono<ResponseEntity<?>> getGroupMemberIsPartOf(@RequestBody MemberGroupsWrapper memberGroupsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> Mono.fromCallable(() -> {
                    if (memberGroupsWrapper.getGroupId().isPresent()) {
                        return chamaGroupService.getGroupDetails(memberGroupsWrapper.getGroupId().get());
                    }
                    if (memberGroupsWrapper.getPage().isPresent() && memberGroupsWrapper.getSize().isPresent()) {
                        return chamaGroupService.getGroupsbyMemberaccount(
                                username,
                                memberGroupsWrapper.getPage().get(),
                                memberGroupsWrapper.getSize().get());
                    }
                    return Mono.just(new UniversalResponse("fail", "kindly provide the page and size parameters"));
                }))
                .flatMap(res -> res)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/invites")
    @PreAuthorize("hasPermission(#wrapper.getGroupId().get()? #wrapper.getGroupId().get(): 0,'group',@objectAction.initFields('invites','canview'))")
    public Mono<ResponseEntity<?>> getGroupInvites(@RequestBody GroupInvitesRequestWrapper wrapper) {
        return Mono.fromCallable(() -> {
                    if (wrapper.getGroupId().isPresent()) {
                        return chamaGroupService.getInvitesByGroup(
                                wrapper.getGroupId().get(),
                                wrapper.getPage(),
                                wrapper.getSize());
                    } else {
                        return CustomAuthenticationUtil.getUsername()
                                .flatMap(username -> chamaGroupService.getInvitesByMemberAccount(username, wrapper.getPage(), wrapper.getSize()));
                    }
                })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/members-summary")
    @ApiOperation(value = "Fetch the group officials and the number of members in group", notes = "Pass the group id alone. I'm reusing the DTO. Bare with me.")
    public Mono<ResponseEntity<?>> getGroupMembersSummary(@RequestBody ResourceId resourceId) {
        return chamaGroupService.getGroupMembersSummary(resourceId.getId())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/member-permissions")
    @PreAuthorize("hasPermission(#request.getPhoneNumber().isPresent()?#request.getGroupId() : 0, 'group' , @objectAction.initFields('members','canview'))")
    public Mono<ResponseEntity<?>> getMemberPermissionsPerGroup(@RequestBody MemberPermissionsRequest request) {
        if (request.getPhoneNumber().isEmpty()) {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> chamaGroupService.getMemberpermissionsInGroup(username, request.getGroupId()))
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }

        return chamaGroupService.getMemberpermissionsInGroup(request.getPhoneNumber().get(), request.getGroupId())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update/member-permissions")
    @PreAuthorize("hasPermission(#updateMemberPermissionsRequest.groupid,'group',@objectAction.initFields('members','canedit'))")
    public Mono<ResponseEntity<?>> updateMemberPermissionsPerGroup(@RequestBody @Valid UpdateMemberPermissionsRequest updateMemberPermissionsRequest) {
        return chamaGroupService
                .updateMemberpermissionsperGroup(updateMemberPermissionsRequest.getPhonenumber(), updateMemberPermissionsRequest.getGroupid(), updateMemberPermissionsRequest.getPermissions())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/leave")
    public Mono<ResponseEntity<?>> leaveGroup(@RequestBody LeaveGroupWrapper leaveGroupWrapper) {
        String member = leaveGroupWrapper.getMemberphonenumber();
        if (member == null) {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> chamaGroupService.exitFromGroup(leaveGroupWrapper.getGroupid(), username, leaveGroupWrapper.getReason()))
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }
        return chamaGroupService.exitFromGroup(leaveGroupWrapper.getGroupid(), member, leaveGroupWrapper.getReason())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/categories")
    public Mono<ResponseEntity<?>> getGroupCategories() {
        return Mono.fromCallable(chamaGroupService::getAllGroupCategories)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/members-request-to-leave-pending-approval")
    public Mono<ResponseEntity<?>> getMembersRequestedToLeave(@RequestBody GroupIdWrapper groupIdWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.getMembersRequestToLeaveGroup(groupIdWrapper.getGroupid(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/approve-decline-leave-request")
    @PreAuthorize("hasPermission(#approveGroupLeaveRequest.getGroupid(), 'group', @objectAction.initFields('members','candelete'))")
    public Mono<ResponseEntity<?>> approveDeclineMemberRequestToLeaveGroup(@RequestBody ApproveGroupLeaveRequest approveGroupLeaveRequest) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.approveDeclineMemberRequestToLeaveGroup(approveGroupLeaveRequest.getPhonenumber(), approveGroupLeaveRequest.getGroupid(), approveGroupLeaveRequest.getApprove(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasPermission(#resourceId.getId(), 'title' ,@objectAction.initFields('groupdetails','candelete'))")
    @ApiOperation(value = "Delete a group.", notes = "Pass the group id alone.")
    public Mono<ResponseEntity<?>> disableGroup(@RequestBody ResourceId resourceId) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.disableGroupByChairperson(resourceId.getId(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/member-in-group")
    @PreAuthorize("hasPermission(#memberInGroupWrapper.getGroupId(), 'group', @objectAction.initFields('members','candelete'))")
    public Mono<ResponseEntity<?>> findMemberInGroup(@RequestBody MemberInGroupWrapper memberInGroupWrapper) {
        return chamaGroupService.findMemberInGroup(memberInGroupWrapper.getGroupId(), memberInGroupWrapper.getPhoneNumber())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }
}
