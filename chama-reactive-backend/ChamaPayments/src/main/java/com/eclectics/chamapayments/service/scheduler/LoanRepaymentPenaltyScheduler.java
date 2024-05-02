package com.eclectics.chamapayments.service.scheduler;

import com.eclectics.chamapayments.model.LoanPenalty;
import com.eclectics.chamapayments.model.LoansDisbursed;
import com.eclectics.chamapayments.repository.LoanPenaltyRepository;
import com.eclectics.chamapayments.repository.LoansdisbursedRepo;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.enums.PaymentEnum;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanRepaymentPenaltyScheduler {

    private final ChamaKycService chamaKycService;
    private final LoansdisbursedRepo loansdisbursedRepo;
    private final LoanPenaltyRepository loanPenaltyRepository;

    /**
     * Scheduler for creating loan penalties for members in group.
     * Should be run for like 2 times a day.
     *
     * @throws ParseException
     */
    @Async
    @Scheduled(fixedDelay = 30000)
    @SchedulerLock(name = "checkNonPaidLoans", lockAtLeastFor = "5m")
    void checkNonPaidLoans() throws ParseException {
        //find due loans
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        List<LoansDisbursed> loansDisbursedList = loansdisbursedRepo.findExpiredLoans(new Date());

        for (LoansDisbursed loansDisbursed : loansDisbursedList) {
            //check if group is activated
            GroupWrapper group = chamaKycService.getMonoGroupById(loansDisbursed.getGroupId());

            if (group == null || !group.isActive()) {
                log.info("Group not found... on checking non-paid loans");
                return;
            }

            Double amount = loansDisbursed.getLoanApplications().getAmount();

            if (loansDisbursed.getLoanApplications().getLoanProducts().getPenaltyValue() == null)
                return;

            Integer penaltyValue = loansDisbursed.getLoanApplications().getLoanProducts().getPenaltyValue();

            if (loansDisbursed.getLoanApplications().getLoanProducts().getIsPercentagePercentage() == null)
                return;

            boolean isPenaltyPercentage = loansDisbursed.getLoanApplications().getLoanProducts().getIsPercentagePercentage();

            double penaltyOwed = 0.0;
            if (isPenaltyPercentage) {
                penaltyOwed = (amount * penaltyValue) / 100;
            } else {
                penaltyOwed = penaltyValue;
            }

            String penaltyPeriod = loansDisbursed.getLoanApplications().getLoanProducts().getPenaltyPeriod();
            List<LoanPenalty> loanPenaltyList = loanPenaltyRepository.findAllByLoansDisbursedAndMemberIdOrderByCreatedOnDesc(loansDisbursed, loansDisbursed.getMemberId());

            if (!loanPenaltyList.isEmpty()) return;

            addLoanPenalty(dateFormat, loansDisbursed, penaltyOwed, penaltyPeriod);
        }
    }

    private void addLoanPenalty(SimpleDateFormat dateFormat, LoansDisbursed loansDisbursed, double penaltyOwed, String penaltyPeriod) {
        LoanPenalty loanPenalty = new LoanPenalty();
        loanPenalty.setDueAmount(loanPenalty.getDueAmount());
        loanPenalty.setPenaltyAmount(penaltyOwed);
        loanPenalty.setLoansDisbursed(loansDisbursed);
        loanPenalty.setMemberId(loansDisbursed.getMemberId());
        loanPenalty.setPaymentStatus(PaymentEnum.DEFAULTED.name());
        loanPenalty.setLoanDueDate(loansDisbursed.getDuedate());

        long milliseconds = new Date().getTime() - loansDisbursed.getDuedate().getTime();
        long days = TimeUnit.MICROSECONDS.toDays(milliseconds);

        Calendar calendar = getCalendar(loanPenalty, days);

        if (calendar == null) return;
        loanPenalty.setPaymentPeriod(penaltyPeriod);
        loanPenalty.setDueAmount(loanPenalty.getDueAmount() + penaltyOwed);
        loanPenalty.setExpectedPaymentDate(dateFormat.format(calendar.getTime()));

        loanPenaltyRepository.save(loanPenalty);
    }

    private Calendar getCalendar(LoanPenalty loanPenalty, long days) {
        Calendar calendar = Calendar.getInstance();

        switch (loanPenalty.getPaymentPeriod().toLowerCase()) {
            case "daily":
                if (!(days >= 1))
                    return null;
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                return calendar;
            case "weekly":
                if (!(days >= 7))
                    return null;
                calendar.set(Calendar.DAY_OF_MONTH, 7);
                return calendar;
            case "monthly":
                if (!(days >= 30))
                    return null;
                calendar.set(Calendar.DAY_OF_MONTH, 30);
                return calendar;
            default:
                return null;
        }
    }
}
