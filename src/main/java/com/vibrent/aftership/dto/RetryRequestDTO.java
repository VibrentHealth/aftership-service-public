package com.vibrent.aftership.dto;

import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequestDTO {
    TrackDeliveryRequestDto trackDeliveryRequestDto;
    MessageHeaderDto messageHeaderDto;
}
