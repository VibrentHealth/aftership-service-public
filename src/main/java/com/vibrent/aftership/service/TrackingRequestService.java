package com.vibrent.aftership.service;

import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;

public interface TrackingRequestService {
    boolean createTrackDeliveryRequest(TrackDeliveryRequestDto trackDeliveryRequestDto, MessageHeaderDto messageHeader);
}
