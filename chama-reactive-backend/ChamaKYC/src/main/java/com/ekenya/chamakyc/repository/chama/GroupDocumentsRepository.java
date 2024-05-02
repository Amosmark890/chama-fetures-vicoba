package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Group;
import com.ekenya.chamakyc.dao.chama.GroupDocuments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupDocumentsRepository extends JpaRepository<GroupDocuments,Long> {
    Optional<GroupDocuments> findByGroupAndFileName(Group group, String fileName);
    List<GroupDocuments> findAllByGroup(Group group);
}
