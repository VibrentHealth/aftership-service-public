package com.vibrent.aftership.service;

import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;

public interface TrackingRequestService {
    boolean createTrackDeliveryRequest(TrackDeliveryRequestVo trackDeliveryRequestVo, MessageHeaderDto messageHeader);
}
