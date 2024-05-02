package com.eclectics.chamapoll.repository;


import com.eclectics.chamapoll.model.Poll;
import com.eclectics.chamapoll.model.PollPositions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PollPositionsRepository extends JpaRepository<PollPositions, Long> {

    List<PollPositions> findAllByPollIdAndStatus(long pollId, boolean status);

    Optional<PollPositions> findPollPositionsByIdAndStatus(long id, boolean status);

    @Query(nativeQuery = true, value = "Select * from poll_positions where poll_id= :pollId AND name like CONCAT('%',:name,'%') AND status ='A'")
    Optional<PollPositions> getPositionByNameAndPollIdandStatus(@Param("name") String position, @Param("pollId") long pollId);

    Optional<PollPositions> findByPollAndName(Poll pollId, String positionName);
}
