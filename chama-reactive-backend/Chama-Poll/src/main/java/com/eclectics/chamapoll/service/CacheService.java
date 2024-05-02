package com.eclectics.chamapoll.service;

import com.eclectics.chamapoll.wrappers.GroupMemberWrapper;
import com.eclectics.chamapoll.wrappers.GroupWrapper;
import com.eclectics.chamapoll.wrappers.MemberWrapper;

import java.util.List;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
public interface CacheService {
    void refreshMembers(List<MemberWrapper> memberWrapperList);
    void refreshGroups(List<GroupWrapper> groupWrapperList);
    void refreshMemberGroups(List<GroupMemberWrapper> memberGroupWrapperList);
}
