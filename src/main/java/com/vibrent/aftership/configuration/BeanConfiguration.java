package com.vibrent.aftership.configuration;

import com.aftership.sdk.AfterShip;
import com.aftership.sdk.model.AftershipOption;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {

    @Bean
    public AfterShip afterShip(@Value("${afterShip.baseUrl}") String afterShipBaseUrl,
                               @Value("${afterShip.apiKey}") String afterShipApiKey) {
        AftershipOption option = new AftershipOption();
        option.setEndpoint(afterShipBaseUrl);
        return new AfterShip(afterShipApiKey, option);
    }

    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }
}
