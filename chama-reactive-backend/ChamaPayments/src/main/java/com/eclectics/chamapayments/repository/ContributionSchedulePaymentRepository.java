package com.eclectics.chamapayments.repository;

import com.eclectics.chamapayments.model.ContributionSchedulePayment;
import com.eclectics.chamapayments.model.jpaInterfaces.UpcomingContributionsProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ContributionSchedulePaymentRepository extends JpaRepository<ContributionSchedulePayment, Long> {

    int countByContributionScheduledId(String param);

    List<ContributionSchedulePayment> findAllByContributionId(Long contributionId);

    @Query(value = "SELECT * FROM contribution_schedule_payment csp WHERE to_date(csp.expected_contribution_date, 'dd/MM/YYYY') >= current_date AND csp.contribution_id = :contributionId", nativeQuery = true)
    List<ContributionSchedulePayment> findUpcomingContributionById(Long contributionId);

    ContributionSchedulePayment findByContributionScheduledId(String scheduledId);

    @Query(value = "SELECT * FROM contribution_schedule_payment WHERE created_on BETWEEN DATE_SUB(NOW(), INTERVAL 7 DAY) AND NOW();", nativeQuery = true)
    List<ContributionSchedulePayment> getWeeklyScheduledPayments();

    @Query(value = "SELECT cp.* FROM contribution_schedule_payment cp LEFT JOIN contributions_tbl ct on (cp.contribution_id = ct.id) where to_date(expected_contribution_date,'dd/MM/YYYY') < current_date AND ct.schedule_type =:scheduleTypeId", nativeQuery = true)
    List<ContributionSchedulePayment> findDefaultedScheduledPayment(@Param("scheduleTypeId") Long scheduleTypeId);

    List<ContributionSchedulePayment> findAllByCreatedOnBetweenAndSoftDelete(Date startDate, Date endDate, boolean softDelete, Pageable pageable);

    @Query(nativeQuery = true, value = "select c.name                                                     as contributionname,\n" +
            "       csp.contribution_scheduled_id                              as schedulepaymentid,\n" +
            "       c.contribution_amount                                      as amount,\n" +
            "       g.id                                                       as groupid,\n" +
            "       (c.contribution_amount - (select coalesce(sum(cp.amount), 0)\n" +
            "                                 from contribution_payment cp\n" +
            "                                 where cp.schedule_payment_id = csp.contribution_scheduled_id\n" +
            "                                   and cp.phone_number = m.imsi)) as remainder,\n" +
            "       csp.expected_contribution_date                             as expectedpaymentdate\n" +
            "from contribution_schedule_payment csp\n" +
            "         inner join contributions_tbl c on c.id = csp.contribution_id\n" +
            "         inner join groups_tbl g on g.id = c.member_group_id\n" +
            "         inner join group_membership_tbl gmt on gmt.group_id = g.id\n" +
            "         inner join members_tbl m on m.id = gmt.members_id\n" +
            "where gmt.members_id = :memberId\n" +
            "  and to_date(csp.expected_contribution_date, 'dd/MM/YYYY') >= now()\n" +
            "group by c.name, csp.contribution_scheduled_id, c.contribution_amount, g.id, m.id, csp.expected_contribution_date;")
    List<UpcomingContributionsProjection> findAllUserUpcomingContributions(long memberId);
}
