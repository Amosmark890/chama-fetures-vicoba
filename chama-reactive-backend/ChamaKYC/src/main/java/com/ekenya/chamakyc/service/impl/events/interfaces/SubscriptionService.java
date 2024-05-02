package com.ekenya.chamakyc.service.impl.events.interfaces;


import com.ekenya.chamakyc.wrappers.broker.PollResult;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface SubscriptionService {
    Consumer<Map<String, Object>> updateGroupEsbRegistration();

    Consumer<List<PollResult>> updatePollPositions();

    Consumer<String> pollWinner();

    Consumer<String> disableMember();
}
