package com.eclectics.chamapoll.service.impl;

import com.eclectics.chamapoll.model.Poll;
import com.eclectics.chamapoll.model.PollCandidates;
import com.eclectics.chamapoll.model.PollPositions;
import com.eclectics.chamapoll.service.PublishService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import static com.eclectics.chamapoll.util.KafkaTopicsUtil.POLL_WINNER_TOPIC;

/**
 * @author Alex Maina
 * @created 29/12/2021
 */
@Service
@RequiredArgsConstructor
public class PublishServiceImpl implements PublishService {

    private static final String SEND_TEXT_TOPIC = "sendVicobaText-in-0";
    private final Gson gson;
    private final StreamBridge streamBridge;

    @Override
    public void publishNewLeader(PollCandidates pollCandidate) {
        JsonObject pollWinnerInfo = new JsonObject();

        PollPositions pollPosition = pollCandidate.getPollPositions();
        Poll poll = pollPosition.getPoll();

        pollWinnerInfo.addProperty("groupId", poll.getGroupId());
        pollWinnerInfo.addProperty("memberId", pollCandidate.getMemberId());
        pollWinnerInfo.addProperty("title", pollPosition.getName().toLowerCase());

        streamBridge.send(POLL_WINNER_TOPIC, pollWinnerInfo.toString());
    }

    @Override
    public void sendText(String message, String phoneNumber) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        jsonObject.addProperty("phoneNumber", phoneNumber);

        streamBridge.send(SEND_TEXT_TOPIC, jsonObject.toString());
    }
}
