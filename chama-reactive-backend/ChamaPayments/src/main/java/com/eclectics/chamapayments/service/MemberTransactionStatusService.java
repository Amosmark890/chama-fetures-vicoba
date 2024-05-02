package com.eclectics.chamapayments.service;

public interface MemberTransactionStatusService {

    boolean canTransact(String walletAccount);

    void enableMemberToTransact();

}
