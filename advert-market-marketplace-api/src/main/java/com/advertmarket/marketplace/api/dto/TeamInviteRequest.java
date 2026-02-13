package com.advertmarket.marketplace.api.dto;

import com.advertmarket.marketplace.api.model.ChannelRight;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request to invite a user to the channel team.
 *
 * @param userId target user ID
 * @param rights set of rights to grant
 */
@Schema(description = "Team invite request")
public record TeamInviteRequest(
        @Positive long userId,
        @NotNull @NonNull Set<@NonNull ChannelRight> rights
) {

    /** Creates a request with a defensive copy of rights. */
    public TeamInviteRequest {
        rights = Set.copyOf(rights);
    }
}
