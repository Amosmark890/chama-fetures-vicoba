package com.eclectics.chamapayments.service;

import java.util.function.Consumer;

/**
 * @author Alex Maina
 * @created 06/12/2021
 */
public interface SubscribeService {

    Consumer<String> createMemberWallet();

    Consumer<String> createGroupContribution();

    Consumer<String> createGroupAccount();

    Consumer<String> enableGroupContributions();

    Consumer<String> disableGroupContributions();

    Consumer<String> writeOffLoansAndPenalties();

    Consumer<String> editContributionName();

    Consumer<String> updateGroupCoreAccount();
}
