package com.ekenya.chamakyc.repository.error;

import com.ekenya.chamakyc.dao.error.FailedOperations;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Alex Maina
 * @created 12/01/2022
 */
public interface FailedOperationsRepository extends JpaRepository<FailedOperations,Long> {
}
