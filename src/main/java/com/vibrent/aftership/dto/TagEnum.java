package com.vibrent.aftership.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets TagEnum
 */
public enum TagEnum {

  INFO_RECEIVED("InfoReceived"),

  IN_TRANSIT("InTransit"),

  OUT_FOR_DELIVERY("OutForDelivery"),

  ATTEMPT_FAIL("AttemptFail"),

  DELIVERED("Delivered"),

  AVAILABLE_FOR_PICKUP("AvailableForPickup"),

  EXCEPTION("Exception"),

  EXPIRED("Expired"),

  PENDING("Pending");

  private final String value;

  TagEnum(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static TagEnum fromValue(String value) {
    for (TagEnum b : TagEnum.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    return null;
  }
}

