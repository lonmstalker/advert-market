package com.advertmarket.identity.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Request to update the user's TON wallet address.
 *
 * @param tonAddress TON address in non-bounceable raw format (EQ.../UQ... prefix)
 */
@Schema(description = "TON wallet address update request")
public record UpdateTonAddressRequest(
        @Schema(description = "TON wallet address",
                example = "UQBx7fEd1KyD5MHoDNFnVSXxw...")
        @NotBlank
        @Pattern(regexp = "[EU]Q[A-Za-z0-9_-]{46}",
                message = "Invalid TON address format")
        @NonNull String tonAddress
) {
}
