package com.chaekchaek.incident;

import com.chaekchaek.incident.config.SlackProperties;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class IncidentAnalyzerApplication {
    public static void main(String[] args) {
        SpringApplication.run(IncidentAnalyzerApplication.class, args);
    }

    @Bean
    MeterBinder slackMetrics(SlackProperties properties) {
        return registry -> registry.gauge("incident_slack_enabled", properties, value -> value.enabled() ? 1 : 0);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
