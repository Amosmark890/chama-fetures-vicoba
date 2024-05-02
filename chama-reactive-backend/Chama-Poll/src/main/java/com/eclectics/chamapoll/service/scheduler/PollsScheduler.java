package com.eclectics.chamapoll.service.scheduler;

import com.eclectics.chamapoll.model.Poll;
import com.eclectics.chamapoll.model.PollCandidates;
import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.model.constants.Status;
import com.eclectics.chamapoll.repository.PollCandidatesRepository;
import com.eclectics.chamapoll.repository.PollPositionsRepository;
import com.eclectics.chamapoll.repository.PollRepository;
import com.eclectics.chamapoll.service.ChamaKycService;
import com.eclectics.chamapoll.service.PublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PollsScheduler {

    private final PollRepository pollRepository;
    private final PublishService publishService;
    private final ChamaKycService chamaKycService;
    private final PollPositionsRepository pollPositionsRepository;
    private final PollCandidatesRepository pollCandidatesRepository;

    ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Scheduled(fixedDelay = 600000)
    @SchedulerLock(name = "disablePastPolls", lockAtMostFor = "6m")
    public void disablePastElections() {
        log.info("Disabling past polls...");
        executorService.execute(() -> {
                    Date now = new Date();
                    pollRepository.findAllByStatusAndVotingEndLessThan(Status.ACTIVE, now)
                            .parallelStream()
                            .forEach(poll -> {
                                poll.setStatus(Status.COMPLETED);
                                pollRepository.save(poll);
                            });
                }
        );
    }

    /**
     * A scheduler that runs every 5 minutes and checks
     * if any poll candidate has attained 50%+1 votes.
     * If yes, it publishes an event to make the candidate the
     * required leader.
     */
    @Scheduled(fixedDelay = 300000)
    @SchedulerLock(name = "checkForWinner", lockAtMostFor = "6m")
    public void checkForWinner() {
        log.info("Checking for a winner...");
        List<Poll> polls = pollRepository.findAllByStatus(Status.ACTIVE);
        polls.forEach(poll -> {
            if (poll.getStatus() != Status.ACTIVE) return;
            log.info("Checking for a winner in poll -> {}...", poll.getDescription());
            List<PollPositions> pollPositions = pollPositionsRepository.findAllByPollIdAndStatus(poll.getId(), true);
            int memberCount = chamaKycService.getGroupMembers(poll.getGroupId()).size();

            pollPositions.forEach(position -> {
                List<PollCandidates> pollCandidates = pollCandidatesRepository.findAllByPositionIdAndStatus(position.getId(), Status.ACTIVE);

                Optional<PollCandidates> winner = pollCandidates.stream()
                        .filter(candidate -> candidate.getVoteCounts() / (double) memberCount > 0.51)
                        .findFirst();

                log.info("Found a winner for poll -> {}...", winner.isPresent());

                winner.ifPresent(pollCandidate -> {
                    publishService.publishNewLeader(pollCandidate);
                    poll.setStatus(Status.COMPLETED);
                    pollCandidate.setStatus(Status.COMPLETED);
                    position.setStatus(false);
                    pollRepository.save(poll);
                    pollCandidatesRepository.save(pollCandidate);
                    pollPositionsRepository.save(position);
                });
            });
        });
    }
}
