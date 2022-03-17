package com.vibrent.aftership.dto;

import com.vibrent.aftership.enums.ExternalEventEnum;
import com.vibrent.aftership.enums.ExternalEventSourceEnum;
import com.vibrent.aftership.enums.ExternalServiceTypeEnum;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
public class ExternalLogDTO implements Serializable {

    private static final long serialVersionUID = -8954216103123749461L;

    @NotNull
    private ExternalServiceTypeEnum service;

    @NotNull
    private RequestMethod httpMethod;

    @NotNull
    private String requestUrl;

    private String requestHeaders;

    private String requestBody;

    private String responseBody;

    private Integer responseCode;

    private Long requestTimestamp;

    private Long responseTimestamp;

    private Long internalId;

    private String externalId;

    private ExternalEventEnum eventType;

    private ExternalEventSourceEnum eventSource;

    private String description;
}
