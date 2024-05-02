package com.ekenya.chamaauthorizationserver.repository;

import com.ekenya.chamaauthorizationserver.entity.Roles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolesRepository extends JpaRepository<Roles,Long> {
}
