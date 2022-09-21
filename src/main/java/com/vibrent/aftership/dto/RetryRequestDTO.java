package com.vibrent.aftership.dto;

import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequestDTO {
    TrackDeliveryRequestVo trackDeliveryRequestVo;
    MessageHeaderDto messageHeaderDto;
}
