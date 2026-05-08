package com.nuv.tfgrs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class StartupLogging {

    private final Environment environment;

    @Value("${server.port}")
    private String port;

    public StartupLogging(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String[] activeProfiles = environment.getActiveProfiles();
        String mode = activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
        log.info("Backend started successfully");
        log.info("Listening on port {}", port);
        log.info("Environment mode {}", mode);
        log.debug("Active profiles: {}", Arrays.toString(activeProfiles));
    }
}
