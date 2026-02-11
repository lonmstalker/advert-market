package com.advertmarket.app.admin;

import com.advertmarket.communication.canary.CanaryRouter;
import com.advertmarket.shared.deploy.CanaryProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated admin endpoint for canary percent control.
 * Auth via static bearer token from environment (CANARY_ADMIN_TOKEN).
 */
@Slf4j
@RestController
@RequestMapping("/internal/v1/canary")
@Tag(name = "Admin", description = "Internal admin endpoints")
public class CanaryAdminController {

    private final CanaryRouter canaryRouter;
    private final String adminToken;

    /** Creates the controller with canary router and properties. */
    public CanaryAdminController(CanaryRouter canaryRouter, CanaryProperties canaryProperties) {
        this.canaryRouter = canaryRouter;
        this.adminToken = canaryProperties.adminToken();
    }

    /** Returns current canary status. */
    @Operation(summary = "Get canary status")
    @ApiResponse(responseCode = "200",
            description = "Current canary configuration")
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

    /** Updates canary percent and/or salt. */
    @Operation(summary = "Update canary settings")
    @ApiResponse(responseCode = "200",
            description = "Updated canary configuration")
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

    /** Current canary deployment status. */
    public record CanaryStatus(int percent, String salt, Instant timestamp) {
    }

    /** Request to update canary settings. */
    public record CanaryUpdate(Integer percent, String salt) {
    }
}
