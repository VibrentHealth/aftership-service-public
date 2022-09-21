package com.vibrent.aftership.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateTimeUtilTest {

    @Test
    void getTimestampFromStringDateTest() {
        assertEquals(1656288000000L, DateTimeUtil.getTimestampFromStringISODate("2022-06-27", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1656288000000L, DateTimeUtil.getTimestampFromStringISODate("2022-06-27T00:00:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1656288000000L, DateTimeUtil.getTimestampFromStringISODate("2022-06-27T00:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.205-07:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.205-07:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.2-07:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.205", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.20", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21.2", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21-07:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07:21", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07-07:00", DateTimeUtil.getTimeZoneFromString("UTC")));
        assertEquals(1466640000000L, DateTimeUtil.getTimestampFromStringISODate("2016-06-23T09:07", DateTimeUtil.getTimeZoneFromString("UTC")));
    }

}