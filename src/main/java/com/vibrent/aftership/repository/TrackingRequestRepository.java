package com.vibrent.aftership.repository;

import com.vibrent.aftership.domain.TrackingRequest;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface TrackingRequestRepository extends CrudRepository<TrackingRequest, Long> {

    Optional<TrackingRequest> findByTrackingId(String trackingId);

    Stream<TrackingRequest> findAllByStatusNotInAndUpdatedOnLessThan(List<String> excludeStatusList, Long updatedOn);
}
