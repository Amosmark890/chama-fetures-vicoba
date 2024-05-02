package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.GroupTitles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupTitlesRepository extends JpaRepository<GroupTitles,Long> {
    Optional<GroupTitles> findByTitlenameContaining(String titlename);

}
