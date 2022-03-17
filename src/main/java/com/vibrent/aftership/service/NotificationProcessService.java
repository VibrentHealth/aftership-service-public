package com.vibrent.aftership.service;

import com.aftership.sdk.model.tracking.Tracking;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.dto.NotificationDTO;

public interface NotificationProcessService {

    void process(NotificationDTO notificationDTO);
    void process(Tracking tracking, TrackingRequest trackingRequest);
}
