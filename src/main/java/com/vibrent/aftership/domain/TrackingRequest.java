package com.vibrent.aftership.domain;

import com.vibrent.vxp.workflow.OperationEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

@Entity
@Table(name = "tracking_request")
@Data
@EqualsAndHashCode(callSuper = false)
@Audited(targetAuditMode = NOT_AUDITED)
@Slf4j
public class TrackingRequest extends Auditable implements Serializable {

    private static final long serialVersionUID = 7137762033297887389L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private OperationEnum operation;

    @NotNull
    private String provider;

    @Column(name = "tracking_id")
    @NotNull
    private String trackingId;

    @Column
    private String participant;

    @Column(name = "status")
    private String status;

    @Column(name = "sub_status_code")
    private String subStatusCode;

    @Column(name = "sub_status_description")
    private String subStatusDescription;

    @Column(name = "carrier_response")
    private String carrierResponse;

    @Column(name = "carrier_response_type")
    private String carrierResponseType;

    @Column(name = "fulfillment_order_id")
    private Long fulfillmentOrderID;

    @Column
    private String header;

}
