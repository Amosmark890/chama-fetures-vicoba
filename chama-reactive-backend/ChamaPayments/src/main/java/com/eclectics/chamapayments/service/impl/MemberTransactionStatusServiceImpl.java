package com.eclectics.chamapayments.service.impl;

import com.eclectics.chamapayments.model.MemberTransactionStatus;
import com.eclectics.chamapayments.repository.MemberTransactionStatusRepository;
import com.eclectics.chamapayments.service.MemberTransactionStatusService;
import com.eclectics.chamapayments.service.constants.TransactionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberTransactionStatusServiceImpl implements MemberTransactionStatusService {

    private final MemberTransactionStatusRepository transactionStatusRepository;

    private boolean checkIfIsViableToEnable(MemberTransactionStatus mts) {
        ZoneId zoneId = ZoneId.systemDefault();

        long lastTransactedEpoch = mts.getLastTransacted().atZone(zoneId).toEpochSecond();
        long nowEpoch = LocalDateTime.now().atZone(zoneId).toEpochSecond();

        long difference = nowEpoch - lastTransactedEpoch;

        return difference / 60 >= 5;
    }

    /**
     * Checks if a member is able to transact.
     * Creates a new entry if there is none existing.
     *
     * @param walletAccount the member wallet account
     * @return true or false
     */
    @Override
    public boolean canTransact(String walletAccount) {
        Optional<MemberTransactionStatus> optionalMemberTransactionStatus = transactionStatusRepository.findFirstByWalletAccount(walletAccount);

        if (optionalMemberTransactionStatus.isEmpty()) {
            MemberTransactionStatus memberTransactionStatus = new MemberTransactionStatus();
            memberTransactionStatus.setLastTransacted(LocalDateTime.now());
            memberTransactionStatus.setWalletAccount(walletAccount);
            memberTransactionStatus.setStatus(TransactionStatus.CANNOT_TRANSACT);
            transactionStatusRepository.save(memberTransactionStatus);

            return true;
        }

        MemberTransactionStatus memberTransactionStatus = optionalMemberTransactionStatus.get();

        TransactionStatus status = memberTransactionStatus.getStatus();

        if (status.equals(TransactionStatus.CAN_TRANSACT)) {
            memberTransactionStatus.setStatus(TransactionStatus.CAN_TRANSACT);

            transactionStatusRepository.save(memberTransactionStatus);
            return true;
        }

        return false;
    }

    /**
     * Scheduler to enable members to make a transaction after 15 mins if a callback is not received
     * for the previous transaction.
     */
    @Override
    @Scheduled(fixedDelay = 300000L)
    @SchedulerLock(name = "enableMemberToTransact", lockAtMostFor = "2m")
    public void enableMemberToTransact() {
        Mono.fromRunnable(() -> {
            log.info("Enabling users to transact... BEGIN");
            // Enable any member to transact if a callback is not received after 15min
            List<MemberTransactionStatus> memberTransactionStatuses = transactionStatusRepository.findAllByLastTransacted();

            memberTransactionStatuses.stream()
                    .filter(this::checkIfIsViableToEnable)
                    .forEach(mts -> {
                        mts.setStatus(TransactionStatus.CAN_TRANSACT);
                        mts.setLastTransacted(LocalDateTime.now());
                        transactionStatusRepository.save(mts);
                    });
            log.info("Enabling users to transact... END");
        }).subscribeOn(Schedulers.boundedElastic()).publishOn(Schedulers.boundedElastic()).subscribe();
    }

}
