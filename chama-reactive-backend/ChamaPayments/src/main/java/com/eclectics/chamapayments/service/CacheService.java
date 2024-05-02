package com.eclectics.chamapayments.service;

import com.eclectics.chamapayments.wrappers.response.GroupMemberWrapper;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import java.util.List;

/**
 * @author Alex Maina
 * @created 22/12/2021
 */
public interface CacheService {
    void refreshMembers(List<MemberWrapper> memberWrapperList);
    void refreshGroups(List<GroupWrapper> groupWrapperList);
    void refreshMemberGroups(List<GroupMemberWrapper> memberGroupWrapperList);
}
