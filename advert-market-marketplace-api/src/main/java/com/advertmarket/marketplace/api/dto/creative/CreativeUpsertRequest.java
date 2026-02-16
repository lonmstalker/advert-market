package com.advertmarket.marketplace.api.dto.creative;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request payload for create/update creative template operations.
 *
 * @param title template title
 * @param text plain text source
 * @param entities text formatting entities
 * @param media attached media assets
 * @param keyboardRows inline keyboard rows
 * @param disableWebPagePreview whether Telegram URL preview should be disabled
 */
public record CreativeUpsertRequest(
        @NotBlank @Size(max = 100) String title,
        @NotBlank @Size(max = 4096) String text,
        @NotNull @Size(max = 256) List<@Valid CreativeEntityDto> entities,
        @NotNull @Size(max = 10) List<@Valid CreativeMediaAssetDto> media,
        @NotNull @Size(max = 5)
        List<@NotNull @Size(min = 1, max = 5)
        List<@Valid CreativeInlineButtonDto>> keyboardRows,
        boolean disableWebPagePreview
) {
    /**
     * Converts request fields into persistent draft representation.
     */
    public CreativeDraftDto toDraft() {
        return new CreativeDraftDto(
                text,
                entities,
                media,
                keyboardRows,
                disableWebPagePreview);
    }
}
