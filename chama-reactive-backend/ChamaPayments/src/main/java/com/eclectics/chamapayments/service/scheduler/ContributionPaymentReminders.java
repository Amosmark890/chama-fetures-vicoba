package com.eclectics.chamapayments.service.scheduler;

import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.ScheduleTypes;
import com.eclectics.chamapayments.repository.*;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.service.NotificationService;
import com.eclectics.chamapayments.wrappers.response.MemberWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContributionPaymentReminders {

    private final ContributionsPaymentRepository contributionsPaymentRepository;

    private final ContributionLoanRepository contributionLoanRepository;

    private final LoanPenaltyPaymentRepository loanPenaltyPaymentRepository;

    private final ScheduleTypeRepository scheduleTypeRepository;

    private final ContributionRepository contributionRepository;

    private final NotificationService notificationService;

    private final ChamaKycService chamaKycService;

    @Autowired
    @Qualifier(value = "remindersExecutor")
    private Executor executor;

    /**
     * Send contribution payment reminders to members of a group.
     * Cron job runs at 11 O'clock every day.
     */
//    @Scheduled(cron = "0 0 11,15,22 * * *")
    @Scheduled(cron = "0 0 11 * * *")//run every day 11AM
    @SchedulerLock(name = "executeReminders", lockAtMostFor = "30m")
    void executeReminders() {
        log.info("started reminders...");
        executor.execute(this::reminderForWeeklyContribution);
        executor.execute(this::reminderForMonthlyContribution);
        executor.execute(this::reminderForQuarterlyContribution);
        executor.execute(this::reminderForYearlyContribution);
    }

    /**
     * Send reminder for weekly contributions.
     */
    @Transactional
    public void reminderForWeeklyContribution() {
        Optional<ScheduleTypes> scheduleType = scheduleTypeRepository.findById(1L);

        scheduleType.ifPresentOrElse(type -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(type);

            contributions.forEach(contribution -> {
                if (contribution.getContributionAmount() < 1 || !contribution.isActive() || contribution.getReminder() < 1)
                    return;
                LocalDate now = LocalDate.now();
                LocalDate dueDate = contribution.getDuedate();
                Integer reminder = contribution.getReminder();

                // Default reminder is 2 days. SMS notifications will be sent to members for 2 days before the due date.
                if (now.plusDays(reminder).equals(dueDate)
                        || now.plusDays(reminder - 1L).equals(dueDate)) { // default is 2
                    // send reminder sms
                    sendReminder(contribution, type);
                }

                if (dueDate.toString().equals(now.toString())) {
                    LocalDate updatedDueDate = dueDate.plusDays(7);
                    contribution.setDuedate(updatedDueDate);
                    contributionRepository.save(contribution);
                    log.info("Update contribution {} due date to {} from {}", contribution.getName(), updatedDueDate, dueDate);
                }
            });
        }, () -> log.info("Cannot find weekly schedule type..."));
    }

    /**
     * Send reminder for monthly contribution.
     */
    @Transactional
    public void reminderForMonthlyContribution() {
        Optional<ScheduleTypes> scheduleType = scheduleTypeRepository.findById(3L);

        scheduleType.ifPresentOrElse(type -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(type);

            contributions.forEach(contribution -> {
                if (contribution.getContributionAmount() < 1) return;
                LocalDate now = LocalDate.now();
                LocalDate dueDate = contribution.getDuedate();
                Integer reminder = contribution.getReminder();

                log.info("The due date: {} and Now: {}", dueDate.toString(), now.toString());
                // Default reminder is 2 days. SMS notifications will be sent to members for 2 days before the due date.
                if (now.plusDays(reminder).equals(dueDate) || now.plusDays(reminder - 1L).equals(dueDate)) { // default is 2
                    // send reminder sms
                    sendReminder(contribution, type);
                }

                // is the day to pay for contribution
                if (dueDate.toString().equals(now.toString())) {
                    LocalDate updatedDueDate = dueDate.plusDays(30);
                    contribution.setDuedate(updatedDueDate);
                    contributionRepository.save(contribution);
                }
            });
        }, () -> log.info("Cannot find monthly schedule type..."));
    }

    /**
     * Send reminder for yearly contributions.
     */
    @Transactional
    public void reminderForYearlyContribution() {
        Optional<ScheduleTypes> scheduleType = scheduleTypeRepository.findById(4L);

        scheduleType.ifPresentOrElse(type -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(type);

            contributions.forEach(contribution -> {
                if (contribution.getContributionAmount() < 1) return;
                LocalDate now = LocalDate.now();
                LocalDate dueDate = contribution.getDuedate();
                Integer reminder = contribution.getReminder();

                // Default reminder is 2 days. SMS notifications will be sent to members for 2 days before the due date.
                if (now.plusDays(reminder).isEqual(dueDate) || now.plusDays(reminder - 1L).isEqual(dueDate)) { // default is 2
                    // send reminder sms
                    sendReminder(contribution, type);
                }

                if (dueDate.toString().equals(now.toString())) {
                    // is the day to pay for contribution
                    LocalDate updatedDueDate = dueDate.plusYears(1);
                    contribution.setDuedate(updatedDueDate);
                    contributionRepository.save(contribution);
                }
            });
        }, () -> log.info("Cannot find yearly schedule type..."));
    }

    /**
     * Send reminder for quarterly contributions.
     */
    @Transactional
    public void reminderForQuarterlyContribution() {
        Optional<ScheduleTypes> scheduleType = scheduleTypeRepository.findById(6L);

        scheduleType.ifPresentOrElse(type -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(type);

            contributions.forEach(contribution -> {
                if (contribution.getContributionAmount() < 1) return;
                LocalDate now = LocalDate.now();
                LocalDate dueDate = contribution.getDuedate();
                Integer reminder = contribution.getReminder();

                // Default reminder is 2 days. SMS notifications will be sent to members for 2 days before the due date.
                if (now.plusDays(reminder).equals(dueDate) || now.plusDays(reminder - 1L).equals(dueDate)) { // default is 2
                    // send reminder sms
                    sendReminder(contribution, type);
                }

                if (dueDate.toString().equals(now.toString())) {
                    // is the day to pay for contribution
                    LocalDate updatedDueDate = dueDate.plusMonths(3);
                    contribution.setDuedate(updatedDueDate);
                    contributionRepository.save(contribution);
                }
            });
        }, () -> log.info("Cannot find quarterly schedule type..."));
    }

    /**
     * Publish sms events to Kafka.
     *
     * @param contribution  the contribution
     * @param scheduleTypes the schedule type
     */
    @Async
    @Transactional
    public void sendReminder(Contributions contribution, ScheduleTypes scheduleTypes) {
        Flux<MemberWrapper> groupMembers = chamaKycService.getFluxGroupMembers(contribution.getMemberGroupId());

        groupMembers.toStream().forEach(member -> notificationService.sendReminderMessage(member, scheduleTypes.getName(), contribution.getName(), contribution.getContributionAmount().intValue(), member.getLanguage()));
    }
}
