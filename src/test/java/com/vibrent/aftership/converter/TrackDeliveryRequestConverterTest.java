package com.vibrent.aftership.converter;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vibrent.aftership.vo.TrackDeliveryRequestVo;
import com.vibrent.vxp.workflow.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class TrackDeliveryRequestConverterTest {

    private TrackDeliveryRequestConverter trackDeliveryRequestConverter;

    private FulfillmentTrackDeliveryRequestDto fulfillmentTrackDeliveryRequestDto;
    private TrackDeliveryRequestDto trackDeliveryRequestDto;

    @BeforeEach
    void setUp() {
        trackDeliveryRequestConverter = new TrackDeliveryRequestConverter();
        initializeFulfillmentTrackDeliveryRequestDto();
        initializeTrackDeliveryRequestDto();
    }

    @Test
    void convertToTrackDeliveryRequestWhenValidFulfillmentTrackDeliveryRequest() {
        TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(fulfillmentTrackDeliveryRequestDto);
        assertEquals("tracking_number_1", trackDeliveryRequestVo.getTrackingID());
        assertEquals(ProviderEnum.USPS.name(), trackDeliveryRequestVo.getCarrierCode());
        assertEquals(123456L, trackDeliveryRequestVo.getParticipant().getVibrentID());
    }

    @Test
    void warnWhenFulfillmentTrackDeliveryRequestIsNull() {
        fulfillmentTrackDeliveryRequestDto = null;
        Logger logger = (Logger) LoggerFactory.getLogger(TrackDeliveryRequestConverter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(fulfillmentTrackDeliveryRequestDto);

        assertNull(trackDeliveryRequestVo);
        var logList = listAppender.list;
        assertEquals("WARN", logList.get(0).getLevel().toString());
    }

    @Test
    void convertToTrackDeliveryRequestWhenValidTrackDeliveryRequest() {
        TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(trackDeliveryRequestDto);
        assertEquals("tracking_number_1", trackDeliveryRequestVo.getTrackingID());
        assertEquals(ProviderEnum.USPS.name(), trackDeliveryRequestVo.getCarrierCode());
        assertEquals(123456L, trackDeliveryRequestVo.getParticipant().getVibrentID());
    }

    @Test
    void warnWhenTrackDeliveryRequestIsNull() {
        trackDeliveryRequestDto = null;
        Logger logger = (Logger) LoggerFactory.getLogger(TrackDeliveryRequestConverter.class)   ;
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();

        TrackDeliveryRequestVo trackDeliveryRequestVo = trackDeliveryRequestConverter.toTrackDeliveryRequestVo(trackDeliveryRequestDto);

        assertNull(trackDeliveryRequestVo);
        var logList = listAppender.list;
        assertEquals("WARN", logList.get(0).getLevel().toString());
    }

    private void initializeFulfillmentTrackDeliveryRequestDto() {
        fulfillmentTrackDeliveryRequestDto = new FulfillmentTrackDeliveryRequestDto();
        fulfillmentTrackDeliveryRequestDto.setTrackingID("tracking_number_1");
        fulfillmentTrackDeliveryRequestDto.setParticipant(new ParticipantDetailsDto("test_ext", 123456L));
        fulfillmentTrackDeliveryRequestDto.setFulfillmentOrderID(1L);
        fulfillmentTrackDeliveryRequestDto.setCarrierCode("USPS");
    }

    private void initializeTrackDeliveryRequestDto() {
        trackDeliveryRequestDto = new TrackDeliveryRequestDto();
        trackDeliveryRequestDto.setTrackingID("tracking_number_1");
        ParticipantDto participantDto = new ParticipantDto();
        participantDto.setExternalID("test_ext");
        participantDto.setVibrentID(123456L);
        trackDeliveryRequestDto.setParticipant(participantDto);
        trackDeliveryRequestDto.setProvider(ProviderEnum.USPS);
    }


}