package com.ekenya.chamakyc.repository.users;

import com.ekenya.chamakyc.dao.user.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolesRepository extends JpaRepository<Roles,Long> {
    Optional<Roles> findByNameAndResourceid(String roleName, String resourceId);
}
