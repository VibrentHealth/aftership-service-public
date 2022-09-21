package com.vibrent.aftership.web;

import com.aftership.sdk.utils.JsonUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.vibrent.aftership.dto.NotificationDTO;
import com.vibrent.aftership.service.NotificationProcessService;
import com.vibrent.aftership.util.SignatureUtil;
import io.micrometer.core.annotation.Timed;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.vibrent.aftership.constants.AfterShipConstants.HEADER_HMAC_SHA256_V4_4_3;
import static com.vibrent.aftership.constants.AfterShipConstants.HEADER_HMAC_SHA256_V4_4_4;

@RestController
@RequestMapping("/api")
@Slf4j
public class AfterShipCallbackResource {

    private final String webHookSecret;
    private final NotificationProcessService notificationProcessService;

    public AfterShipCallbackResource(@Value("${afterShip.webhookSecret}") String webHookSecret,
                                     NotificationProcessService notificationProcessService) {
        this.webHookSecret = webHookSecret;
        this.notificationProcessService = notificationProcessService;
    }

    @PostMapping(value = "/aftership/notification")
    public ResponseEntity<Void> notificationCallback(HttpEntity<String> httpEntity) {
        return processNotificationCallback(httpEntity);
    }

    @Timed(value = "notificationWebhookTime", description = "Time taken to process notification webhook")
    private ResponseEntity<Void> processNotificationCallback(HttpEntity<String> httpEntity) {

        //Validate the signature
        if (!SignatureUtil.isSignatureValid(getHMacHeader(httpEntity.getHeaders()), httpEntity.getBody(), webHookSecret)) {
            log.warn("afterShip: Tracking webhook notification signature validation failed. {}", httpEntity.getBody());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            JsonElement jsonElement = JsonParser.parseString(httpEntity.getBody());
            NotificationDTO dto = JsonUtils.getGson().fromJson(jsonElement, NotificationDTO.class);
            log.info("afterShip: Tracking webhook notification received = {}", dto);

            //Process notification dto
            this.notificationProcessService.process(dto);
        } catch (Exception e) {
            log.warn("afterShip: Failed to process Webhook notification -{}", httpEntity.getBody(), e);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private static String getHMacHeader(@NonNull HttpHeaders httpHeaders) {

        String hMacHeader = httpHeaders.getFirst(HEADER_HMAC_SHA256_V4_4_3);

        return StringUtils.hasText(hMacHeader) ? hMacHeader
                : httpHeaders.getFirst(HEADER_HMAC_SHA256_V4_4_4);
    }
}
