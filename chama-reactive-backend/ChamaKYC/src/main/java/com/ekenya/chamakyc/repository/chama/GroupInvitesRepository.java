package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.GroupInvites;
import com.ekenya.chamakyc.wrappers.response.GroupInvitesWrapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupInvitesRepository extends JpaRepository<GroupInvites, Long> {
    List<GroupInvites> findByPhonenumberAndGroup(String phonenumber, Group group);

    List<GroupInvites> findByPhonenumberAndGroupId(String phonenumber, Long groupId);

    List<GroupInvites> findByPhonenumberAndStatus(String phoneNumber, String active, Pageable pageable);

    List<GroupInvites> findByPhonenumberAndStatus(String phoneNumber, String status);

    @Query("select new com.ekenya.chamakyc.wrappers.response.GroupInvitesWrapper(gi.id, gi.status, gi.group.id, gi.group.name) from GroupInvites gi where gi.phonenumber = :phoneNumber and gi.status = :status")
    List<GroupInvitesWrapper> findUserInvites(String phoneNumber, String status);

    int countByPhonenumberAndStatus(String phoneNumber, String active);

    List<GroupInvites> findByPhonenumberAndStatusAndId(String phoneNumber, String active, long inviteid);

    @Query("select new com.ekenya.chamakyc.wrappers.response.GroupInvitesWrapper(gi.id, gi.status, gi.group.id, gi.group.name) from GroupInvites gi where gi.group = :groupId")
    List<GroupInvitesWrapper> findByGroup(long groupId);

    int countByGroup(Group group);
}
