package com.advertmarket.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request body for completing user onboarding.
 *
 * @param interests selected interest tags
 */
public record OnboardingRequest(
        @NotEmpty @Size(max = 50)
        List<@NotBlank @Size(max = 100) String> interests
) {

    /** Defensive copy constructor. */
    public OnboardingRequest {
        interests = List.copyOf(interests);
    }
}
