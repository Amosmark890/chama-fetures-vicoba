package com.ekenya.chamakyc.repository.users;

import com.ekenya.chamakyc.dao.user.Users;
import com.ekenya.chamakyc.wrappers.SearchDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, Long> {

    Optional<Users> findByPhoneNumberAndChannel(String phoneNumber, String channel);

    Optional<Users> findByEmail(String email);

    Optional<Users> findByEmailAndActiveTrue(String email);

    Optional<Users> findByEmailAndActiveTrueAndChannel(String email, String channel);

    Optional<Users> findByUsername(String username);

    boolean existsByPhoneNumberOrEmail(String phoneNumber, String email);

    boolean existsByEmailAndChannel(String phoneNumber, String email);

    Page<Users> findAllByChannel(String channel, Pageable pageable);

    Optional<Users> findByEmailAndChannel(String username, String name);

    long countBySoftDeleteFalseAndActiveTrue();

    long countByChannel(String channel);

    @Query(nativeQuery = true, value = "SELECT t.ct as totalCount, t.res as resData from search_app_user(:firstName, :otherName, :email, :phoneNumber, :status, :gender, :page, :size) as t(ct, res)")
    SearchDetails searchAppUser(String firstName, String otherName, String email, String phoneNumber, String status, String gender, int page, int size);
    @Query(nativeQuery = true, value = "SELECT t.ct as totalCount, t.res as resData from search_portal_user(:firstName, :otherName, :email, :phoneNumber, :status, :gender, :page, :size) as t(ct, res)")
    SearchDetails searchPortalUser(String firstName, String otherName, String email, String phoneNumber, String status, String gender, int page, int size);
}
