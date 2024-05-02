package com.ekenya.chamakyc.resource.mobile;

import com.ekenya.chamakyc.configs.CustomAuthenticationUtil;
import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.impl.constants.Channel;
import com.ekenya.chamakyc.wrappers.broker.GroupWrapper;
import com.ekenya.chamakyc.wrappers.broker.UniversalResponse;
import com.ekenya.chamakyc.wrappers.request.*;
import com.google.gson.Gson;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/v2/kyc/group")
@Slf4j
@RequiredArgsConstructor
public class GroupResource {

    private final ChamaGroupService chamaGroupService;
    private final Gson gson;

    @ApiIgnore
    @GetMapping("/test")
    public Mono<ResponseEntity<?>> test() {
        return CustomAuthenticationUtil.getUsername()
                .map(username -> ResponseEntity.ok().body("Whoami " + username + "only to be seen by clients using APP and USSD"));
    }

    @PostMapping("/vicoba/lookup")
    public Mono<ResponseEntity<?>> groupLookUp(@RequestBody AccountNumberWrapper accountNumberWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.groupAccountLookup(accountNumberWrapper.getAccount(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/create")
    public Mono<ResponseEntity<?>> createGroup(@Valid @RequestBody GroupDetailsWrapper groupDetailsWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.createGroup(groupDetailsWrapper, username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PutMapping("/update")
    @PreAuthorize("hasPermission(#groupUpdateWrapper.getGroupId(), 'group' ,@objectAction.initFields('groupaccount','canedit'))")
    @ApiOperation(value = "Update Group name", notes = "This operation can only be done by the officials.")
    public Mono<ResponseEntity<?>> editGroupName(@RequestBody GroupUpdateWrapper groupUpdateWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.updateGroupName(groupUpdateWrapper.getGroupId(), groupUpdateWrapper.getName(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/invite")
    public Mono<ResponseEntity<?>> sendGroupInvites(@Valid @RequestBody Optional<InvitesWrapper> invitesWrapper,
                                                    @RequestParam Optional<Long> groupid,
                                                    @RequestParam("invitefile") Optional<FilePart> invitefile) {
        if (invitesWrapper.isPresent()) {
            return chamaGroupService.sendGroupInvites(invitesWrapper.get().getGroupid(), invitesWrapper.get().getPhonenumbersandrole())
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));

        } else if (groupid.isPresent() && invitefile.isPresent()) {
            return chamaGroupService.readFileandsendInvites(Mono.just(invitefile.get()), groupid.get())
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }
        return Mono.just(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new UniversalResponse("fail", "Bad request")));
    }

    @PostMapping("/invite/officials")
    @PreAuthorize("hasPermission(#invitesWrapper.getGroupid(), 'group' ,@objectAction.initFields('groupaccount','cancreate'))")
    public Mono<ResponseEntity<?>> sendOfficialsGroupInvite(@Valid @RequestBody InvitesWrapper invitesWrapper) {
        return Mono.fromCallable(() -> chamaGroupService.sendOfficialsInvite(invitesWrapper.getGroupid(), invitesWrapper.getPhonenumbersandrole()))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/invite/{action}")
    public Mono<ResponseEntity<?>> acceptOrDeclineInvite(@RequestParam long inviteid, @PathVariable String action) {
        return CustomAuthenticationUtil.getUsername()
                .map(username -> chamaGroupService.acceptordeclineInvite(inviteid, username, action.equals("accept")))
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

    @GetMapping("/invites")
    @PreAuthorize("hasPermission(#groupid.isPresent()? #groupid.get(): 0,'group',@objectAction.initFields('invites','canview'))")
    @ApiOperation(value = "Fetch the invites for a group or for individuals.", notes = "The group id is not mandatory if you are querying for your invites.")
    public Mono<ResponseEntity<?>> getGroupInvites(@RequestParam Optional<Long> groupid, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Mono.fromCallable(() -> {
                    if (groupid.isPresent()) {
                        return chamaGroupService.getInvitesByGroup(groupid.get(), page, size);
                    } else {
                        return CustomAuthenticationUtil.getUsername()
                                .flatMap(username -> chamaGroupService.getInvitesByMemberAccount(username, page, size));
                    }
                })
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/members")
    @PreAuthorize("hasPermission(#groupid,'group',@objectAction.initFields('members','canview'))")
    public Mono<ResponseEntity<?>> getMembersInGroup(@RequestParam long groupid, @RequestParam int page, @RequestParam int size) {
        return chamaGroupService.getAllMembersInGroup(groupid, page, size)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/member-permissions")
    @PreAuthorize("hasPermission(#phonenumber.isPresent()?#groupid:0,'group',@objectAction.initFields('members','canview'))")
    public Mono<ResponseEntity<?>> getMemberPermissionsPerGroup(@RequestParam long groupid, @RequestParam Optional<String> phonenumber) {
        if (phonenumber.isEmpty()) {
            return CustomAuthenticationUtil.getUsername()
                    .flatMap(username -> chamaGroupService.getMemberpermissionsInGroup(username, groupid))
                    .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
        }

        return chamaGroupService.getMemberpermissionsInGroup(phonenumber.get(), groupid)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/update/member-permissions")
    @PreAuthorize("hasPermission(#updatememberpermissionsWrapper.getGroupid(),'group',@objectAction.initFields('members','canedit'))")
    public Mono<ResponseEntity<?>> updateMemberPermissionsPerGroup(@RequestBody @Valid UpdatememberpermissionsWrapper updatememberpermissionsWrapper) {
        return chamaGroupService
                .updateMemberpermissionsperGroup(updatememberpermissionsWrapper.getPhonenumber(),
                        updatememberpermissionsWrapper.getGroupid(), updatememberpermissionsWrapper.getPermissions())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/upload/{groupId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.IMAGE_PNG_VALUE})
    @PreAuthorize("hasPermission(#groupId,'group',@objectAction.initFields('documents','cancreate'))")
    public Mono<ResponseEntity<?>> uploadDocuments(
            @RequestPart("file") Mono<FilePart> file,
            @PathVariable long groupId,
            @RequestHeader(HttpHeaders.USER_AGENT) Channel channel) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> {
                    return file.flatMap(image -> chamaGroupService.uploadDocuments(groupId, image, image.filename(), username, channel));
                }).map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @GetMapping("/download/{groupid}/{filename:.+}")
    @PreAuthorize("hasPermission(#groupid,'group',@objectAction.initFields('documents','canview'))")
    public Mono<ResponseEntity<?>> downloadFile(@PathVariable long groupid, @PathVariable String filename) {
        return chamaGroupService.retrieveFile(groupid, filename);
    }

    @PostMapping("/leave")
    @ApiOperation(value = "Leave group", notes = "The member phone number is not required for a member wishing to leave by themselves.")
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

    @GetMapping(value = "/categories")
    public Mono<ResponseEntity<?>> getGroupCategories() {
        return Mono.fromCallable(chamaGroupService::getAllGroupCategories)
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/members-request-to-leave-pending-approval")
    @PreAuthorize("hasPermission(#groupMembership.getGroupid(), 'title' ,@objectAction.initFields('groupdetails','canedit'))")
    public Mono<ResponseEntity<?>> getMembersRequestedToLeave(@RequestBody GroupMembershipWrapper groupMembership) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.getMembersRequestToLeaveGroup(groupMembership.getGroupid(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping(value = "/approve-decline-leave-request")
    @PreAuthorize("hasPermission(#groupMembership.getGroupid(), 'title' ,@objectAction.initFields('groupdetails','canedit'))")
    public Mono<ResponseEntity<?>> approveDeclineMemberRequestToLeaveGroup(@RequestBody GroupMembershipWrapper groupMembership) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.approveDeclineMemberRequestToLeaveGroup(groupMembership.getPhonenumber(), groupMembership.getGroupid(), groupMembership.isApprove(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/disable")
    @PreAuthorize("hasPermission(#groupWrapper.getId(), 'title' ,@objectAction.initFields('groupdetails','candelete'))")
    @ApiOperation(value = "Delete a group.", notes = "Pass the group id alone. I'm reusing the DTO. Bare with me.")
    public Mono<ResponseEntity<?>> disableGroup(@RequestBody GroupWrapper groupWrapper) {
        return CustomAuthenticationUtil.getUsername()
                .flatMap(username -> chamaGroupService.disableGroupByChairperson(groupWrapper.getId(), username))
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

    @PostMapping("/member-in-group")
    @PreAuthorize("hasPermission(#memberInGroupWrapper.getGroupId(), 'group', @objectAction.initFields('members','candelete'))")
    public Mono<ResponseEntity<?>> findMemberInGroup(@RequestBody MemberInGroupWrapper memberInGroupWrapper) {
        return chamaGroupService.findMemberInGroup(memberInGroupWrapper.getGroupId(), memberInGroupWrapper.getPhoneNumber())
                .map(res -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(res));
    }

}
