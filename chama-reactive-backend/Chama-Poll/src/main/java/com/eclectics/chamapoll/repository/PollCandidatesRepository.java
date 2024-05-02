package com.eclectics.chamapoll.repository;

import com.eclectics.chamapoll.model.PollCandidates;
import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.model.constants.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


public interface PollCandidatesRepository extends JpaRepository<PollCandidates, Long> {
    Optional<PollCandidates> findPollCandidatesByPositionIdAndMemberIdAndStatus(Long positionId, Long memberId, Status status);

    Optional<PollCandidates> findPollCandidatesByPollIdAndMemberIdAndStatus(Long pollId, Long memberId, Status status);

    Optional<PollCandidates> findPollCandidatesByIdAndStatus(Long id, Status status);

    List<PollCandidates> findAllByPositionIdAndStatus(long positionId, Status status);

    List<PollCandidates> findPollCandidatesByPositionIdAndStatusOrderByVoteCounts(Long id, Status status);

    void deleteAllByPollPositions(PollPositions pollPositions);

    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "DELETE FROM poll_candidates WHERE poll_positions_id = :pollId")
    void deleteAllByPollPositionsId(long pollId);
}
