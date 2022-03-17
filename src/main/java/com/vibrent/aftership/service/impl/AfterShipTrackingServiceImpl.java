package com.vibrent.aftership.service.impl;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.exception.AftershipException;
import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.exception.AfterShipNonRetriableException;
import com.vibrent.aftership.exception.AfterShipRetriableException;
import com.vibrent.aftership.service.AfterShipTrackingService;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class AfterShipTrackingServiceImpl implements AfterShipTrackingService {

    private final AfterShip afterShip;
    private final List<String> retryStatusCodes;

    public AfterShipTrackingServiceImpl(AfterShip afterShip, @NotNull @Value("${afterShip.retryStatusCodes}") String retryStatusCodes) {
        this.afterShip = afterShip;
        this.retryStatusCodes = Arrays.asList(retryStatusCodes.replaceAll("\\s", "").split(","));
    }

    @Override
    @Timed(value = "createTrackingApiTime", description = "Time taken to execute create tracking API")
    public Tracking createTracking(@NotNull NewTracking newTracking) {
        try {
            Tracking tracking = afterShip.getTrackingEndpoint().createTracking(newTracking);
            log.info("AfterShip: Create Track Delivery request is successful. Tracking response: {}", tracking);
            return tracking;
        } catch (AftershipException e) {
            log.warn("AfterShip: Failed to Create new tracking with status code:{}, Tracking: {}", e.getCode(), newTracking, e);
            if (retryStatusCodes.contains(String.valueOf(e.getCode()))) {
                throw new AfterShipRetriableException(e.getMessage(), e.getCode(), e);
            } else {
                throw new AfterShipNonRetriableException(e.getMessage(), e.getCode(), e);
            }
        }
    }
}
