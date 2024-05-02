package com.eclectics.chamapoll.service;

/**
 * @author Alex Maina
 * @created 27/12/2021
 */
public interface SubscribeService {
    void subscribeGroups(String data);
    void subscribeMembers(String data);
    void subscribeMemberGroups(String data);
}
