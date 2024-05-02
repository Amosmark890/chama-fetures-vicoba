package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.GroupMembership;
import com.ekenya.chamakyc.dao.chama.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMembersRepository extends JpaRepository<GroupMembership, Long> {
    Optional<GroupMembership> findByMembersAndGroup(Member members, Group groups);

//    List<GroupMembership> findAllByGroupAndRequesttoleavegroupIsTrueAndIsrequesttoleaveactedonIsFalse(Group group);

    Page<GroupMembership> findByMembersAndActivemembershipTrue(Member members, Pageable pageable);

    List<GroupMembership> findAllByGroupIdAndActivemembershipTrue(long groupId);

    List<GroupMembership> findByMembersAndActivemembershipTrue(Member members);

    int countByMembersAndActivemembershipTrue(Member member);

    List<GroupMembership> findByGroup(Group group, Pageable pageable);

    List<GroupMembership> findByGroup(Group group);

    int countByGroup(Group group);

    int countByGroupAndActivemembershipIsTrue(Group group);

    Optional<GroupMembership> findFirstByGroupAndTitle(Group group, String title);

    List<GroupMembership> findAllByGroupAndRequesttoleavegroupIsTrueAndIsrequesttoleaveactedonIsTrue(Group group);
}
