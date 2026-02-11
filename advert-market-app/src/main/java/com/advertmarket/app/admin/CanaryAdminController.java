package com.advertmarket.app.admin;

import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.deploy.CanaryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Authenticated admin endpoint for canary percent control.
 * Auth via static bearer token from environment (CANARY_ADMIN_TOKEN).
 */
@RestController
@RequestMapping("/internal/v1/canary")
public class CanaryAdminController {

    private static final Logger log = LoggerFactory.getLogger(CanaryAdminController.class);

    private final CanaryRouter canaryRouter;
    private final String adminToken;

    public CanaryAdminController(CanaryRouter canaryRouter, CanaryProperties canaryProperties) {
        this.canaryRouter = canaryRouter;
        this.adminToken = canaryProperties.adminToken();
    }

    @GetMapping
    public ResponseEntity<CanaryStatus> getCanary(
            @RequestHeader("Authorization") String authorization) {
        if (!authenticate(authorization)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(new CanaryStatus(
                canaryRouter.getCanaryPercent(),
                canaryRouter.getSalt(),
                Instant.now()
        ));
    }

    @PutMapping
    public ResponseEntity<CanaryStatus> setCanary(
            @RequestHeader("Authorization") String authorization,
            @RequestBody CanaryUpdate update) {
        if (!authenticate(authorization)) {
            return ResponseEntity.status(401).build();
        }

        if (update.percent() != null) {
            canaryRouter.setCanaryPercent(update.percent());
            log.info("Admin set canary percent to {}%", update.percent());
        }
        if (update.salt() != null) {
            canaryRouter.setSalt(update.salt());
            log.info("Admin updated canary salt");
        }

        return ResponseEntity.ok(new CanaryStatus(
                canaryRouter.getCanaryPercent(),
                canaryRouter.getSalt(),
                Instant.now()
        ));
    }

    private boolean authenticate(String authorization) {
        if (adminToken == null || adminToken.isEmpty()) {
            log.warn("CANARY_ADMIN_TOKEN is not configured, denying access");
            return false;
        }
        return ("Bearer " + adminToken).equals(authorization);
    }

    public record CanaryStatus(int percent, String salt, Instant timestamp) {
    }

    public record CanaryUpdate(Integer percent, String salt) {
    }
}