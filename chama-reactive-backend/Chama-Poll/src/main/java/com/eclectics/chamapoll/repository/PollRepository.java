package com.eclectics.chamapoll.repository;

import com.eclectics.chamapoll.model.Poll;
import com.eclectics.chamapoll.model.constants.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface PollRepository extends JpaRepository<Poll,Long> {
    List<Poll> findAllByGroupId(long groupId);

    List<Poll> findAllByGroupIdAndStatusAndVotingEndGreaterThanEqual(long groupId, Status status, Date today);

    Optional<Poll> findPollByGroupIdAndStatus(Long groupId, Status status);

    Optional<Poll> findPollByGroupIdAndStatusAndVotingEndGreaterThanEqual(Long groupId, Status status, Date date);

    Optional<Poll> findPollByIdAndStatus(long id, Status status);

    Optional<Poll> findPollByGroupIdAndStatus(long groupId, Status status);

    List<Poll> findPollsByGroupIdAndStatusOrderByCreatedOnDesc(long groupId, Status status);

    List<Poll> findPollByResultsAppliedFalseAndVotingEndBeforeAndStatus(Date votingEnd, Status status);

    List<Poll> findAllByResultsAppliedFalse();

    List<Poll> findAllByStatus(Status status);

    List<Poll> findAllByStatusAndVotingEndLessThan(Status status, Date votingEnd);
}