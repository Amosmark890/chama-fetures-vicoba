package com.ekenya.chamaauthorizationserver.repository;

import com.ekenya.chamaauthorizationserver.entity.Users;
import org.hibernate.annotations.OrderBy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users,Long> {
    List<Users> findAllByPhoneNumberAndActive(String phonenumber,Boolean active);
    List<Users> findAllByEmailAndActive(String email, Boolean active);
    List<Users>findUsersByEmailOrPhoneNumber(String email,String phonenumber);
    Optional<Users> findByEmailAndChannelEquals(String username,String channel);
    Optional<Users> findByPhoneNumberAndChannelEquals(String username, String app);
}
