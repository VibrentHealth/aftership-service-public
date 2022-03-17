package com.vibrent.aftership.scheduling;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.exception.AftershipException;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.NotificationProcessService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@Component
public class GetTrackingJob implements Job {
    private final AfterShip afterShip;
    private final TrackingRequestRepository trackingRequestRepository;
    private final NotificationProcessService notificationProcessService;
    private final ExternalLogService externalLogService;
    private final Integer fetchTrackingBeforeDays;
    private final List<String> excludeStatusList;

    public GetTrackingJob(AfterShip afterShip,
                          TrackingRequestRepository trackingRequestRepository,
                          NotificationProcessService notificationProcessService,
                          ExternalLogService externalLogService, @Value("${afterShip.fetchTrackingBeforeDays}") Integer fetchTrackingBeforeDays,
                          @NotNull @Value("${afterShip.excludeStatus}") List<String> excludeStatusList) {
        this.afterShip = afterShip;
        this.trackingRequestRepository = trackingRequestRepository;
        this.notificationProcessService = notificationProcessService;
        this.externalLogService = externalLogService;
        this.fetchTrackingBeforeDays = fetchTrackingBeforeDays;
        this.excludeStatusList = excludeStatusList;
    }

    @Transactional
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long startTime = System.currentTimeMillis();
        log.info("Aftership | Started execution of GetTrackingJob at : {}", startTime);
        Stream<TrackingRequest> eligibleTrackingIds = getEligibleTrackingIds(getTimestamp());

        eligibleTrackingIds
                .forEach(trackingRequest -> {
                            try {
                                SlugTrackingNumber slugTrackingNumber = new SlugTrackingNumber(trackingRequest.getProvider().toValue(), trackingRequest.getTrackingId());
                                Tracking tracking = afterShip.getTrackingEndpoint().getTracking(slugTrackingNumber, null);
                                externalLogService.send(slugTrackingNumber, tracking, System.currentTimeMillis(), "Aftership | Successfully fetched latest tracking.", HttpStatus.OK.value());
                                notificationProcessService.process(tracking, trackingRequest);
                                log.info("Aftership | Fetched tracking for Tracking no: {}, Latest Status: {}", tracking.getTrackingNumber(), tracking.getTag());

                            } catch (AftershipException e) {
                                externalLogService.send(trackingRequest.getTrackingId(), trackingRequest.getProvider().toValue(), System.currentTimeMillis(), "Aftership | Exception whiling fetched latest tracking.", e.getCode());
                                log.warn("AfterShip | Exception while fetching tracking id {} , Exception: {}", trackingRequest.getTrackingId(), e.getMessage(), e);
                            } catch (Exception e) {
                                externalLogService.send(trackingRequest.getTrackingId(), trackingRequest.getProvider().toValue(), System.currentTimeMillis(), "Aftership | Exception whiling fetched latest tracking.", HttpStatus.INTERNAL_SERVER_ERROR.value());
                                log.warn("AfterShip | Exception while fetching tracking id {} , Exception: {}", trackingRequest.getTrackingId(), e.getMessage(), e);
                            }
                        }

                );
        log.info("Aftership | Completed execution of GetTrackingJob at : {} Total time take for execution: {} ms", System.currentTimeMillis(), System.currentTimeMillis() - startTime);
    }

    private Stream<TrackingRequest> getEligibleTrackingIds(Long updatedOn) {
        return trackingRequestRepository.findAllByStatusNotInAndUpdatedOnLessThan(excludeStatusList, updatedOn);
    }


    private Long getTimestamp() {
        return Instant.now().minus(fetchTrackingBeforeDays, ChronoUnit.DAYS).toEpochMilli();
    }
}
