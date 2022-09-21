package com.vibrent.aftership.util;

import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.TimeZone;

@Slf4j
public class DateTimeUtil {

    private DateTimeUtil() {
        //private constructor
    }

    public static Long getTimestampFromStringISODate(String dateTime, ZoneId zoneId) {
        Long timestamp = null;

        if (dateTime != null) {
            final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE)
                    .optionalStart()
                    .optionalStart()
                    .appendLiteral(' ')
                    .optionalEnd()
                    .optionalStart()
                    .appendLiteral('T')
                    .optionalEnd()
                    .appendOptional(DateTimeFormatter.ISO_TIME)
                    .toFormatter();

            timestamp = LocalDate.parse(dateTime, formatter).atStartOfDay().atZone(zoneId).toInstant().toEpochMilli();
        }
        return timestamp;
    }

    public static ZoneId getTimeZoneFromString(String timezoneString) {
        try {
            return ZoneId.of(timezoneString);
        } catch (DateTimeException e) {
            log.warn("AfterShip: Error while getting zoneId for the Timezone: {} using Java TimeZone for short forms of timezone. ", timezoneString, e);
            //For short forms, getting zoneId
            return TimeZone.getTimeZone(timezoneString).toZoneId();
        }
    }

}
