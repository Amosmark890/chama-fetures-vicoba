package com.eclectics.chamapayments.service.scheduler;

import com.eclectics.chamapayments.model.ContributionSchedulePayment;
import com.eclectics.chamapayments.model.Contributions;
import com.eclectics.chamapayments.model.ScheduleTypes;
import com.eclectics.chamapayments.repository.ContributionRepository;
import com.eclectics.chamapayments.repository.ContributionSchedulePaymentRepository;
import com.eclectics.chamapayments.repository.ScheduleTypeRepository;
import com.eclectics.chamapayments.service.ChamaKycService;
import com.eclectics.chamapayments.wrappers.response.GroupWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Handles the creation of upcoming contributions for every group and its contributions.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ContributionPaymentScheduler {

    private final ChamaKycService chamaKycService;
    private final ScheduleTypeRepository scheduleTypeRepository;
    private final ContributionRepository contributionRepository;
    @Autowired
    private ContributionSchedulePaymentRepository contributionSchedulePaymentRepository;

    /**
     * Scheduler to update daily upcoming contributions.
     */
    void scheduleDailyPayment() {
        Optional<ScheduleTypes> scheduleTypesOptional = scheduleTypeRepository.findById(2L);

        scheduleTypesOptional.ifPresentOrElse(scheduleType -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleTypeAndActiveTrue(scheduleType);

            contributions.parallelStream()
                    .forEach(contribution -> {
                        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contribution.getMemberGroupId());

                        if (groupWrapper == null) return;

                        if (!groupWrapper.isActive()) return;

                        Date date = getDailyDueDate();
                        String paymentScheduleId = generateDailyPaymentScheduleId(contribution);

                        saveContributionSchedulePayment(contribution, date, paymentScheduleId);
                    });
        }, () -> log.info("Schedule type not found... On daily payment scheduling."));
    }

    /**
     * Schedule upcoming weekly payments.
     */
    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "scheduleWeeklyPayment", lockAtMostFor = "2m")
    @Transactional
    public void scheduleWeeklyPayment() {
        Optional<ScheduleTypes> scheduleTypeOptional = scheduleTypeRepository.findById(1L);

        scheduleTypeOptional.ifPresent(scheduleType -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(scheduleType);

            contributions.forEach(contribution -> {
                GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contribution.getMemberGroupId());

                if (groupWrapper == null) return;

                if (!groupWrapper.isActive()) return;

                log.info("The group: " + groupWrapper.getName());

                Date weeklyDueDate = getWeeklyDueDate(contribution.getDuedate());
                String paymentScheduledId = generateWeeklyPaymentScheduledId(weeklyDueDate, contribution);

                saveContributionSchedulePayment(contribution, weeklyDueDate, paymentScheduledId);
            });
        });
    }

    private String generateWeeklyPaymentScheduledId(Date weeklyDueDate, Contributions contribution) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(weeklyDueDate);
        int week = calendar.get(Calendar.WEEK_OF_YEAR);
        int year = calendar.get(Calendar.YEAR);

        return "CHAW" + contribution.getId() + year + week;
    }

    private Date getWeeklyDueDate(LocalDate dueDate) {
        return new Date(dueDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000);
    }

    /**
     * Scheduler to update monthly upcoming contributions.
     */
    @Scheduled(fixedDelay = 60000)
    @SchedulerLock(name = "scheduleWeeklyPayment", lockAtMostFor = "2m")
    @Transactional
    public void scheduleMonthlyPayment() {
        Optional<ScheduleTypes> scheduleTypesOptional = scheduleTypeRepository.findById(3L);

        scheduleTypesOptional.ifPresentOrElse(scheduleType -> {
            List<Contributions> contributions = contributionRepository.findAllByScheduleType(scheduleType);

            contributions.parallelStream()
                    .forEach(contribution -> {
                        GroupWrapper groupWrapper = chamaKycService.getMonoGroupById(contribution.getMemberGroupId());

                        if (groupWrapper == null) return;

                        if (!groupWrapper.isActive()) return;

                        Date date = getMonthlyDueDate(contribution.getDuedate());
                        String paymentScheduleId = generateMonthlyPaymentScheduleId(date, contribution);

                        saveContributionSchedulePayment(contribution, date, paymentScheduleId);

                    });
        }, () -> log.info("Schedule type not found... On monthly payment scheduling."));
    }

    /**
     * Generate unique id for monthly scheduled payment.
     *
     * @param date         the monthly due date
     * @param contribution the contribution entity
     * @return a unique id
     */
    private String generateMonthlyPaymentScheduleId(Date date, Contributions contribution) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        return "CHAM" + contribution.getId() + year + month;
    }

    /**
     * Get the updated monthly due date
     *
     * @param dueDate the due date
     * @return an updated monthly date
     */
    private Date getMonthlyDueDate(LocalDate dueDate) {
        if (dueDate == null)
            return new Date();
        Calendar calendar = Calendar.getInstance();

        int currentDate = calendar.get(Calendar.DAY_OF_MONTH);
        if (currentDate == dueDate.getDayOfMonth()) {
            return new Date();
        }

        if (dueDate.getDayOfMonth() <= currentDate) {
            calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
        }
        calendar.set(Calendar.DAY_OF_MONTH, dueDate.getDayOfMonth());
        return calendar.getTime();
    }

    /**
     * Saves a contribution scheduled payment.
     *
     * @param contribution      the contribution
     * @param date              the due date
     * @param paymentScheduleId the unique id of the scheduled payment
     */
    private void saveContributionSchedulePayment(Contributions contribution, Date date, String paymentScheduleId) {
        SimpleDateFormat dateFor = new SimpleDateFormat("dd/MM/yyyy");
        String stringDate = dateFor.format(date);

        int count = contributionSchedulePaymentRepository.countByContributionScheduledId(paymentScheduleId);
        log.info("Scheduled Contributions count type... {} {}", count, paymentScheduleId);

        if (count == 0) { // make sure there is no duplicates for scheduled payments
            ContributionSchedulePayment contributionSchedulePayment = new ContributionSchedulePayment();

            contributionSchedulePayment.setContributionId(contribution.getId());
            contributionSchedulePayment.setContributionScheduledId(paymentScheduleId);
            contributionSchedulePayment.setExpectedContributionDate(stringDate);
            contributionSchedulePaymentRepository.save(contributionSchedulePayment);
        }
    }

    /**
     * Generate unique id for daily scheduled payment.
     *
     * @param contribution the contribution entity
     * @return a unique id
     */
    private String generateDailyPaymentScheduleId(Contributions contribution) {
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        date.add(Calendar.DAY_OF_MONTH, 1);

        int dayOfTheYear = date.get(Calendar.DAY_OF_YEAR);
        int year = date.get(Calendar.YEAR);
        return "CHAD" + contribution.getId() + dayOfTheYear + year;
    }

    /**
     * Get the daily due date
     *
     * @return a date
     */
    private Date getDailyDueDate() {
        Calendar date = Calendar.getInstance();
        date.setTime(new Date());
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        date.add(Calendar.DAY_OF_MONTH, 1);
        return date.getTime();
    }
}
