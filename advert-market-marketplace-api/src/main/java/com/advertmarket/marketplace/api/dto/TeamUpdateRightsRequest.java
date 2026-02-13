package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.ChannelRight;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request to update team member rights.
 *
 * @param rights new set of rights
 */
@Schema(description = "Team rights update request")
public record TeamUpdateRightsRequest(
        @NotNull @NonNull Set<@NonNull ChannelRight> rights
) {
}
