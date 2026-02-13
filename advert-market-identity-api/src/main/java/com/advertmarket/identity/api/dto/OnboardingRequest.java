package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request body for completing user onboarding.
 *
 * @param interests selected interest tags
 */
@Schema(description = "User onboarding completion request")
public record OnboardingRequest(
        @Schema(description = "Selected interest tags",
                example = "[\"tech\", \"gaming\"]")
        @NotEmpty @Size(max = 50)
        @NonNull List<@NotBlank @Size(max = 100) String> interests
) {

    /** Defensive copy constructor. */
    public OnboardingRequest {
        interests = List.copyOf(interests);
    }
}
