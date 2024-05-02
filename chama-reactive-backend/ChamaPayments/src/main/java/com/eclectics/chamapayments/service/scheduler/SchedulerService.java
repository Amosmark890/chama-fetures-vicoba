package com.eclectics.chamapayments.service.scheduler;

public interface SchedulerService <E> {
    void  addCronJobToScheduler( E tasks);
    void  addFixedRateToScheduler(E tasks);
    void  removeScheduledTask(String jobId);
}
