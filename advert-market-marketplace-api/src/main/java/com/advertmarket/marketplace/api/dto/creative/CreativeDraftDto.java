package com.advertmarket.marketplace.api.dto.creative;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Draft payload persisted inside creative templates.
 *
 * @param text plain text source
 * @param entities text formatting entities
 * @param media attached media assets
 * @param keyboardRows inline keyboard rows
 * @param disableWebPagePreview whether Telegram URL preview should be disabled
 */
public record CreativeDraftDto(
        @NotBlank @Size(max = 4096) String text,
        @NotNull @Size(max = 256) List<@Valid CreativeEntityDto> entities,
        @NotNull @Size(max = 10) List<@Valid CreativeMediaAssetDto> media,
        @NotNull @Size(max = 5)
        List<@NotNull @Size(min = 1, max = 5)
        List<@Valid CreativeInlineButtonDto>> keyboardRows,
        boolean disableWebPagePreview
) {
}
