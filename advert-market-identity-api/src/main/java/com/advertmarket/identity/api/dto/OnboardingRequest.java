package com.advertmarket.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request body for completing user onboarding.
 *
 * @param interests selected interest tags
 */
public record OnboardingRequest(
        @NotEmpty List<@NotBlank String> interests
) {

    /** Defensive copy constructor. */
    public OnboardingRequest {
        interests = List.copyOf(interests);
    }
}
