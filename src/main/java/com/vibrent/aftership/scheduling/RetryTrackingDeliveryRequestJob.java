package com.vibrent.aftership.scheduling;

import com.vibrent.aftership.domain.TrackingRequestError;
import com.vibrent.aftership.dto.RetryRequestDTO;
import com.vibrent.aftership.messaging.producer.impl.RetryTrackingDeliveryRequestProducer;
import com.vibrent.aftership.repository.TrackingRequestErrorRepository;
import com.vibrent.aftership.util.JacksonUtil;
import com.vibrent.vxp.workflow.MessageHeaderDto;
import com.vibrent.vxp.workflow.TrackDeliveryRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

@Slf4j
@Component
public class RetryTrackingDeliveryRequestJob implements Job {
    private final TrackingRequestErrorRepository trackingRequestErrorRepository;
    private final RetryTrackingDeliveryRequestProducer retryTrackingDeliveryRequestProducer;
    private final Integer maxRetryCount;

    public RetryTrackingDeliveryRequestJob(TrackingRequestErrorRepository trackingRequestErrorRepository, RetryTrackingDeliveryRequestProducer retryTrackingDeliveryRequestProducer,
                                           @Value("${afterShip.maxRetryCount}") Integer maxRetryCount) {
        this.trackingRequestErrorRepository = trackingRequestErrorRepository;
        this.retryTrackingDeliveryRequestProducer = retryTrackingDeliveryRequestProducer;
        this.maxRetryCount = maxRetryCount;
    }



    @Override
    @Transactional
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Long startTime = System.currentTimeMillis();
        log.info("Aftership | Started Retry Tracking Delivery Request Job Execution {}", startTime);
        Stream<TrackingRequestError> trackingRequestErrors = trackingRequestErrorRepository.findByRetryCountLessThan(maxRetryCount);
        trackingRequestErrors
                .forEach(trackingRequestError ->
                {
                    try {
                        TrackDeliveryRequestDto trackDeliveryRequestDto = JacksonUtil.getMapper().readValue(trackingRequestError.getTrackDeliveryRequest(), TrackDeliveryRequestDto.class);
                        MessageHeaderDto messageHeaderDto = JacksonUtil.getMapper().readValue(trackingRequestError.getHeader(), MessageHeaderDto.class);
                        log.info("Retrying tracking ID {} ", trackDeliveryRequestDto.getTrackingID());
                        RetryRequestDTO retryRequestDTO = new RetryRequestDTO(trackDeliveryRequestDto, messageHeaderDto);
                        retryTrackingDeliveryRequestProducer.send(retryRequestDTO);
                    } catch (Exception e) {
                        log.warn("Aftership | Failed to send error tracking request for Tracking id : {}, Exception {}", trackingRequestError.getTrackingId(), e);
                    }
                });

        log.info("Aftership | Completes Retry Tracking Delivery Request Job Execution. Total time taken: {} ", System.currentTimeMillis() - startTime);
    }
}
