package com.vibrent.aftership.dto;

import com.aftership.sdk.model.tracking.Tracking;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * NotificationDTO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationDTO   {
  @JsonProperty("event_id")
  private String eventId;

  @JsonProperty("event")
  private String event;

  @JsonProperty("is_tracking_first_tag")
  private Boolean isTrackingFirstTag;

  @JsonProperty("msg")
  private Tracking msg;

  @JsonProperty("ts")
  private BigDecimal ts;
}

