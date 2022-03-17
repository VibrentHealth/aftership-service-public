package com.vibrent.aftership.service;

import com.aftership.sdk.model.tracking.NewTracking;
import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.exception.AfterShipException;

import javax.validation.constraints.NotNull;

public interface AfterShipTrackingService {

    /**
     * @param newTracking
     * @return
     * @throws AfterShipException
     */
    Tracking createTracking(@NotNull NewTracking newTracking);
}
