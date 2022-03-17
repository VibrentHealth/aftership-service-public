package com.vibrent.aftership.configuration;

import com.vibrent.aftership.scheduling.GetTrackingJob;
import com.vibrent.aftership.scheduling.RetryTrackingDeliveryRequestJob;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.text.ParseException;

@Configuration
public class JobConfiguration {

    private final String retryTrackingDeliveryCron;
    private final String getTrackingCron;

    public JobConfiguration(@Value("${afterShip.cron.retryTrackingDeliveryCron}") String retryTrackingDeliveryCron,
                            @Value("${afterShip.cron.getTrackingCron}") String getTrackingCron){
        this.retryTrackingDeliveryCron = retryTrackingDeliveryCron;
        this.getTrackingCron = getTrackingCron;
    }


    @Bean
    public JobDetail retryTrackingDeliveryRequestJobDetails() {
        return JobBuilder.newJob().ofType(RetryTrackingDeliveryRequestJob.class)
                .storeDurably()
                .withIdentity("Retry_Tracking_Delivery_Request_Job")
                .withDescription("Retry Tracking Delivery Requests on Error")
                .build();
    }

    @Bean
    public Trigger retryTrackingDeliveryRequestTrigger(JobDetail retryTrackingDeliveryRequestJobDetails) {
        return TriggerBuilder.newTrigger().forJob(retryTrackingDeliveryRequestJobDetails)
                .withIdentity("Retry_Tracking_Delivery_Request_Trigger")
                .withDescription("Invoke Retry Tracking Delivery Request Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(retryTrackingDeliveryCron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Scheduler rescheduleCronJob(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException, ParseException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("Retry_Tracking_Delivery_Request_Trigger");
        CronTriggerImpl trigger = (CronTriggerImpl) scheduler.getTrigger(triggerKey);
        trigger.setCronExpression(retryTrackingDeliveryCron);
        scheduler.rescheduleJob(triggerKey, trigger);
        return scheduler;
    }

    @Bean
    public JobDetail getTrackingJobDetails() {
        return JobBuilder.newJob().ofType(GetTrackingJob.class)
                .storeDurably()
                .withIdentity("Get_Tracking_Details_Job")
                .withDescription("Get Tracking Details Job")
                .build();
    }

    @Bean
    public Trigger getTrackingJobDetailsTrigger(JobDetail getTrackingJobDetails) {
        return TriggerBuilder.newTrigger().forJob(getTrackingJobDetails)
                .withIdentity("Get_Tracking_Details_Trigger")
                .withDescription("Invoke Get Tracking Details Trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(getTrackingCron)
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    @Bean
    public Scheduler rescheduleGetTrackingCronJob(SchedulerFactoryBean schedulerFactoryBean) throws SchedulerException, ParseException {
        Scheduler scheduler = schedulerFactoryBean.getScheduler();
        TriggerKey triggerKey = new TriggerKey("Get_Tracking_Details_Trigger");
        CronTriggerImpl trigger = (CronTriggerImpl) scheduler.getTrigger(triggerKey);
        trigger.setCronExpression(getTrackingCron);
        scheduler.rescheduleJob(triggerKey, trigger);
        return scheduler;
    }


}
