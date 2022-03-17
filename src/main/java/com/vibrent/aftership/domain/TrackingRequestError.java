package com.vibrent.aftership.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "tracking_request_error")
@Data
@Audited(targetAuditMode = NOT_AUDITED)
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class TrackingRequestError extends Auditable implements Serializable {

    private static final long serialVersionUID = 7137762033297887389L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_id")
    @NotNull
    private String trackingId;

    @Column(name = "error_code")
    private Integer errorCode;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "track_delivery_request")
    private String trackDeliveryRequest;

    @Column(name = "header")
    private String header;


}
