package com.vibrent.aftership.scheduling;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.exception.AftershipException;
import com.aftership.sdk.model.tracking.SlugTrackingNumber;
import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.dto.NotificationDTO;
import com.vibrent.aftership.enums.CarrierResponseType;
import com.vibrent.aftership.repository.TrackingRequestRepository;
import com.vibrent.aftership.service.ExternalLogService;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.aftership.util.JacksonUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
                    String slug = null;
                            try {
                                slug = getSlugFromCarrierResponse(trackingRequest.getCarrierResponse(), trackingRequest.getCarrierResponseType());
                                if (!StringUtils.hasText(slug)) {
                                    log.warn("AfterShip| Carrier response don't have slug value for tracking id : {}", trackingRequest.getTrackingId());
                                    return;
                                }
                                SlugTrackingNumber slugTrackingNumber = new SlugTrackingNumber(slug, trackingRequest.getTrackingId());
                                Tracking tracking = afterShip.getTrackingEndpoint().getTracking(slugTrackingNumber, null);
                                externalLogService.send(slugTrackingNumber, tracking, System.currentTimeMillis(), "Aftership | Successfully fetched latest tracking.", HttpStatus.OK.value());
                                notificationProcessService.process(tracking, trackingRequest);
                                log.info("Aftership | Fetched tracking for Tracking no: {}, Latest Status: {}", tracking.getTrackingNumber(), tracking.getTag());

                            } catch (AftershipException e) {
                                externalLogService.send(trackingRequest.getTrackingId(), slug , System.currentTimeMillis(), "Aftership | Exception whiling fetching latest tracking.", e.getCode());
                                log.warn("AfterShip | AfterShip exception while fetching tracking id {} , Exception: {}", trackingRequest.getTrackingId(), e.getMessage(), e);
                            } catch (Exception e) {
                                externalLogService.send(trackingRequest.getTrackingId(), slug, System.currentTimeMillis(), "Aftership | Exception whiling fetching latest tracking.", HttpStatus.INTERNAL_SERVER_ERROR.value());
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


    // Tracking request may contains different slug values, hence extracting slug value from carrier response
    public static String getSlugFromCarrierResponse(String carrierResponse, String carrierResponseType) {
        String slug = null;
        if (StringUtils.hasText(carrierResponse)) {
            try {
                if (CarrierResponseType.NOTIFICATION.toString().equalsIgnoreCase(carrierResponseType)) {
                    NotificationDTO carrierResponseDTO = JacksonUtil.getMapper().readValue(carrierResponse, NotificationDTO.class);
                    slug = carrierResponseDTO.getMsg().getSlug();
                } else if (CarrierResponseType.TRACKING.toString().equalsIgnoreCase(carrierResponseType)) {
                    Tracking messageDto = JacksonUtil.getMapper().readValue(carrierResponse, Tracking.class);
                    slug = messageDto.getSlug();
                }

            } catch (JsonProcessingException e) {
                log.warn("AfterShip| Error while extracting slug from carrier response ", e);
            }
        }
        return slug;
    }
}
