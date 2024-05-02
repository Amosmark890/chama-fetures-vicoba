package com.ekenya.chamakyc.repository.config;

import com.ekenya.chamakyc.dao.config.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {
}