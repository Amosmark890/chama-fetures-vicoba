package com.ekenya.chamakyc.repository.chama;

import com.ekenya.chamakyc.dao.chama.Otp;
import com.ekenya.chamakyc.dao.user.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface OtpRepository extends JpaRepository<Otp,Long> {
    Optional<Otp> findByOtpValueAndUserAndExpiredFalseAndOtpType(String otpValue, Users user, String otpType);
    Optional<Otp> findOtpByUserAndExpiredFalse(Users users);
    Optional<Otp> findOtpByUserAndExpiredFalseAndOtpType(Users user, String otpType);
    Optional<Otp> findByOtpValueAndUserAndExpiredFalse(String otp, Users user);
}
