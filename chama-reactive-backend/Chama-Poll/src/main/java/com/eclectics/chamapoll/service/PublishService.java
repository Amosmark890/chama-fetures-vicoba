package com.eclectics.chamapoll.service;

import com.eclectics.chamapoll.model.PollCandidates;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
public interface PublishService {

    void publishNewLeader(PollCandidates pollCandidates);

    void sendText(String message, String phoneNumber);
}
