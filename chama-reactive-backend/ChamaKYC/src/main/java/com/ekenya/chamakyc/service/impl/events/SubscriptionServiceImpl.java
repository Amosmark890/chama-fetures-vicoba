package com.ekenya.chamakyc.service.impl.events;

import com.ekenya.chamakyc.service.Interfaces.ChamaGroupService;
import com.ekenya.chamakyc.service.Interfaces.ChamaUserService;
import com.ekenya.chamakyc.service.impl.events.interfaces.SubscriptionService;
import com.ekenya.chamakyc.wrappers.broker.PollResult;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
    private final ChamaGroupService chamaGroupService;
    private final ChamaUserService chamaUserService;


    @Override
    public Consumer<Map<String, Object>> updateGroupEsbRegistration() {
        return (map)-> {
            boolean status= (Boolean) map.get("status");
            long groupId= (Long) map.get("group");
            if(status) chamaGroupService.updateEsbGroupRegistration(groupId);
        };
    }

    @Override
    public Consumer<List<PollResult>> updatePollPositions(){
       return chamaGroupService::updatePollResultPositions;
    }

    @Override
    @Bean
    public Consumer<String> pollWinner() {
        return chamaGroupService::updateGroupLeader;
    }

    @Override
    @Bean
    public Consumer<String> disableMember() {
        return chamaUserService::disableMember;
    }

}
