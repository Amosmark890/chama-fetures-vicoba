package com.ekenya.chamakyc.service.impl.functions;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.GroupMembership;
import com.ekenya.chamakyc.dao.chama.Member;
import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.service.Interfaces.FileHandlerService;
import com.ekenya.chamakyc.wrappers.broker.GroupMemberWrapper;
import com.ekenya.chamakyc.wrappers.broker.GroupWrapper;
import com.ekenya.chamakyc.wrappers.broker.MemberWrapper;
import com.ekenya.chamakyc.wrappers.response.UsersDetails;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Function;

/**
 * @author Alex Maina
 * @created 25/02/2022
 **/
@Component
@AllArgsConstructor
public class MapperFunction {
    private final FileHandlerService fileHandlerService;

    public Function<GroupMembership, MemberWrapper> mapToMemberWrapper() {
        return memberGroup -> {
            Users users = memberGroup.getMembers().getUsers();
            Member member = memberGroup.getMembers();
            return MemberWrapper.builder()
                    .firstname(users.getFirstName())
                    .lastname(users.getLastName())
                    .dateofbirth(users.getDateOfBirth())
                    .phonenumber(users.getPhoneNumber())
                    .countrycode(users.getCountryCode())
                    .identification(users.getNationalId())
                    .nationality(users.getNationality())
                    .gender(users.getGender())
                    .userDeviceId(member.getUserDeviceId())
                    .active(users.isActive())
                    .isregisteredmember(member.isIsregisteredmember())
                    .email(users.getEmail())
                    .ussdplatform(member.isUssdplatform())
                    .androidplatform(member.isAndroidplatform())
                    .iosplatform(member.isIosplatform())
                    .lastlogin(users.getLastLogin())
                    .esbwalletaccount(member.getEsbwalletaccount())
                    .walletexists(member.isWalletexists())
                    .isFirstTimeLogin(users.isFirstTimeLogin())
                    .groupTitle(memberGroup.getTitle())
                    .build();
        };
    }


    public Function<Member, MemberWrapper> mapMemberToMemberWrappers() {
        return member -> MemberWrapper.builder()
                .id(member.getId())
                .firstname(member.getUsers().getFirstName())
                .lastname(member.getUsers().getLastName())
                .dateofbirth(member.getUsers().getDateOfBirth())
                .phonenumber(member.getUsers().getPhoneNumber())
                .countrycode(member.getUsers().getCountryCode())
                .identification(member.getUsers().getNationalId())
                .nationality(member.getUsers().getNationality())
                .gender(member.getUsers().getGender())
                .userDeviceId(member.getUserDeviceId())
                .active(member.getUsers().isActive())
                .isregisteredmember(member.getUsers().isActive())
                .email(member.getUsers().getEmail())
                .ussdplatform(member.isUssdplatform())
                .androidplatform(member.isAndroidplatform())
                .iosplatform(member.isIosplatform())
                .esbwalletaccount(member.getEsbwalletaccount())
                .walletexists(member.isWalletexists())
                .isFirstTimeLogin(member.getUsers().isFirstTimeLogin())
                .createdOn(member.getCreatedOn())
                .language(member.getUsers().getLanguage())
                .linkedAccounts(member.getLinkedAccounts())
                .lastUpdatedOn(member.getLastModifiedDate())
                .softDelete(member.isSoftDelete())
                .build();
    }

    public Function<GroupMembership, GroupMemberWrapper> mapMemberGroupToGroupMemberWrapper() {
        return memberGroup -> GroupMemberWrapper.builder()
                .id(memberGroup.getId())
                .groupId(memberGroup.getGroup().getId())
                .memberId(memberGroup.getMembers().getId())
                .groupName(memberGroup.getGroup().getName())
                .activemembership(memberGroup.isActivemembership())
                .isrequesttoleaveactedon(memberGroup.isIsrequesttoleaveactedon())
                .requesttoleavegroup(memberGroup.isRequesttoleavegroup())
                .deactivationreason(memberGroup.getDeactivationreason())
                .title(memberGroup.getTitle())
                .phoneNumber(memberGroup.getMembers().getImsi())
                .permissions(memberGroup.getPermissions())
                .createdOn(memberGroup.getCreatedOn())
                .softDelete(memberGroup.isSoftDelete())
                .build();
    }

    public Function<Group, GroupWrapper> mapGroupToGroupWrapper() {
        return group -> GroupWrapper.builder()
                .id(group.getId())
                .name(group.getName())
                .csbAccount(group.getCbsAccount())
                .location(group.getLocation())
                .description(group.getDescription())
                .active(group.isActive())
                .categoryId(group.getCategoryId())
                .purpose(group.getPurpose())
                .groupConfig(group.getGroupConfig())
                .createdOn(group.getCreatedOn())
                .updateOn(group.getLastModifiedDate())
                .isDeleted(group.isSoftDelete())
                .walletexists(group.isWalletexists())
                .build();
    }

    public Function<Users, UsersDetails> mapToUserDetails() {
        return user -> UsersDetails.builder()
                .userid(user.getId())
                .firstname(user.getFirstName())
                .othernames(user.getLastName())
                .phonenumber(user.getPhoneNumber())
                .email(user.getEmail())
                .nationality(user.getNationality())
                .gender(user.getGender())
                .identification(user.getNationalId())
                .dateofbirth(user.getDateOfBirth().toString())
                .active(user.isActive())
                .walletbalance(0)
                .build();
    }

}
