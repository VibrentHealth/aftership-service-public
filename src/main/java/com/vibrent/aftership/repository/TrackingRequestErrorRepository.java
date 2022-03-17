package com.vibrent.aftership.repository;

import com.vibrent.aftership.domain.TrackingRequestError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.stream.Stream;

public interface TrackingRequestErrorRepository extends JpaRepository<TrackingRequestError, Long> {

    Optional<TrackingRequestError> findByTrackingId(String trackingId);

    void deleteByTrackingId(String trackingID);

    Stream<TrackingRequestError> findByRetryCountLessThan(Integer retryCount);
}
