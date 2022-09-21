package com.vibrent.aftership.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vibrent.vxp.workflow.ParticipantDetailsDto;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author jigar.patel
 */
@Data
public class TrackDeliveryRequestVo implements Serializable {

    @NotNull
    @JsonProperty("carrierCode")
    @JsonAlias("provider")
    private String carrierCode;

    @JsonProperty("fulfillmentOrderId")
    private Long fulfillmentOrderID;

    @NotNull
    @JsonProperty("participant")
    private ParticipantDetailsDto participant;

    @NotNull
    @JsonProperty("trackingId")
    private String trackingID;

}
