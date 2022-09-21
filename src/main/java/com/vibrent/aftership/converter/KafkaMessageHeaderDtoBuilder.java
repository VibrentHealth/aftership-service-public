package com.vibrent.aftership.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.vibrent.aftership.domain.TrackingRequest;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
public class KafkaMessageHeaderDtoBuilder {

    private static final String VXP_HEADER_VERSION = "2.1.3";
    private static final String VXP_MESSAGE_SPEC_VERSION = "2.1.2";
    private static final String SOURCE_AFTER_SHIP = "AfterShip";

    private KafkaMessageHeaderDtoBuilder(){
    }
    /**
     *
     * @param trackingRequest
     * @return
     */
    public static MessageHeaderDto getMessageHeaderDto(TrackingRequest trackingRequest) {
        try {
            if (!StringUtils.isEmpty(trackingRequest.getHeader())) {
                return JacksonUtil.getMapper().readValue(trackingRequest.getHeader(), MessageHeaderDto.class);
            }
        } catch (JsonProcessingException e) {
            log.warn("AfterShip: Error while parsing Header value String as MessageHeaderDto", e);
        }
        return null;
    }

    /**
     *
     * @param savedMessageHeaderDto
     * @param vibrentID
     * @return
     */
    public static MessageHeaderDto populateMessageHeaderDtoFromDto(MessageHeaderDto savedMessageHeaderDto, long vibrentID, MessageSpecificationEnum vxpMessageSpec) {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(savedMessageHeaderDto.getVxpHeaderVersion());
        messageHeaderDto.setVxpWorkflowName(savedMessageHeaderDto.getVxpWorkflowName());
        messageHeaderDto.setVxpMessageSpec(vxpMessageSpec);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(savedMessageHeaderDto.getVxpMessageSpecVersion());
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID(savedMessageHeaderDto.getVxpWorkflowInstanceID());
        messageHeaderDto.setVxpPattern(savedMessageHeaderDto.getVxpPattern());
        messageHeaderDto.setVxpUserID(vibrentID);
        messageHeaderDto.setVxpTrigger(savedMessageHeaderDto.getVxpTrigger());
        messageHeaderDto.setVxpTenantID(savedMessageHeaderDto.getVxpTenantID());
        messageHeaderDto.setVxpProgramID(savedMessageHeaderDto.getVxpProgramID());
        return messageHeaderDto;
    }

    /**
     *
     * @param vibrentID
     * @return
     */
    public static MessageHeaderDto populateDefaultMessageHeaderDto(long vibrentID, MessageSpecificationEnum vxpMessageSpec) {
        MessageHeaderDto messageHeaderDto = new MessageHeaderDto();
        messageHeaderDto.setVxpMessageID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpHeaderVersion(VXP_HEADER_VERSION);
        messageHeaderDto.setVxpWorkflowName(WorkflowNameEnum.SALIVARY_KIT_ORDER);
        messageHeaderDto.setVxpMessageSpec(vxpMessageSpec);
        messageHeaderDto.setVxpMessageTimestamp(System.currentTimeMillis());
        messageHeaderDto.setSource(SOURCE_AFTER_SHIP);
        messageHeaderDto.setVxpMessageSpecVersion(VXP_MESSAGE_SPEC_VERSION);
        messageHeaderDto.setVxpOriginator(RequestOriginatorEnum.PTBE);
        messageHeaderDto.setVxpWorkflowInstanceID(UUID.randomUUID().toString());
        messageHeaderDto.setVxpPattern(IntegrationPatternEnum.WORKFLOW);
        messageHeaderDto.setVxpUserID(vibrentID);
        messageHeaderDto.setVxpTrigger(ContextTypeEnum.EVENT);
        return messageHeaderDto;
    }
}
