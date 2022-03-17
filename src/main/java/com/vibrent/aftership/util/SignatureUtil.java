package com.vibrent.aftership.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
public class SignatureUtil {

    public static final String HMAC_SHA_256 = "HmacSHA256";

    private SignatureUtil() {
    }

    public static boolean isSignatureValid(String hMacSha256header, String requestBody, String secretKey) {
        if (!StringUtils.hasText(hMacSha256header)) {
            log.warn("afterShip: Empty or null Hmac-Sha256 header received for signature validation.");
            return false;
        }

        if (!StringUtils.hasText(requestBody)) {
            log.warn("afterShip: Empty or null request body received for signature validation.");
            return false;
        }

        return hMacSha256header.equalsIgnoreCase(generateSignature(requestBody, secretKey));
    }

    public static String generateSignature(@NonNull String data, @NonNull String clientSecret) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            SecretKeySpec spec = new SecretKeySpec(clientSecret.getBytes(), HMAC_SHA_256);
            mac.init(spec);
            byte[] byteHMAC = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(byteHMAC);
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            log.warn("afterShip: Error during AfterShip signature check.", e);
        }

        return null;
    }
}
