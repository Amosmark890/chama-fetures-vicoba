package com.eclectics.chamapoll.repository;

import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.model.PollVoteData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PollVoteDataRepository extends JpaRepository<PollVoteData, Long> {
    boolean existsPollVoteDataByMemberIdAndPositionIdAndCandidateId(long memberId, long positionId, long candidateId);

    boolean existsByCreatorIdAndPollPositions(long pollId, PollPositions pollPositions);

    List<PollVoteData> findAllByPollPositions(PollPositions pollId);

    void deleteAllByPollPositions(PollPositions pollPosition);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM poll_votes_data WHERE poll_positions_id = :pollPositionsId")
    void deleteAllByPollPositionsById(long pollPositionsId);
}
