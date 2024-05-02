package com.eclectics.chamapoll.service;

import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.wrappers.*;
import reactor.core.publisher.Mono;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
public interface PollService {
    Mono<UniversalResponse> createEmptyPoll(CreatePollRequest createPollRequest, String loggedUserPhone);

    Mono<UniversalResponse> createPoll(CreatePollWrapper createPollWrapper, String loggedUserPhone);

    Mono<UniversalResponse> cancelPoll(CancelPollWrapper cancelPollWrapper);

    Mono<UniversalResponse> deletePoll(CancelPollWrapper cancelPollWrapper, String username);

    Mono<UniversalResponse> getMemberActivePolls(String loggedUserPhone);

    Mono<UniversalResponse> getPollPositions(long pollId, String loggedUserPhone);

    Mono<UniversalResponse> vyePosition(long positionId, long pollId, String loggedUserPhone);

    Mono<UniversalResponse> exitPosition(long positionId, long pollId, String loggedUserPhone);

    Mono<UniversalResponse> votePositions(VoteRequestWrapper requestWrapper, String loggedUserPhone);

    Mono<UniversalResponse> getPollResults(long pollId, String username);

    Mono<UniversalResponse> addPositionPollCandidate(String positionName, long pollId, String candidatePhone);

    Mono<UniversalResponse> addMemberToPosition(PollPositions pollPosition, long pollId, String loggedUserPhone);

    Mono<UniversalResponse> findPollsInGroup(GroupIdWrapper groupIdWrapper);
}
