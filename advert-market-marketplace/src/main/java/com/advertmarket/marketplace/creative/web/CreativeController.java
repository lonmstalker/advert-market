package com.advertmarket.marketplace.creative.web;

import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.marketplace.creative.service.CreativeService;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.SecurityContextUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Personal creative template library endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/creatives")
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Creatives", description = "Creative template library")
public class CreativeController {

    private final CreativeService creativeService;

    /**
     * Lists current user's creative templates.
     */
    @GetMapping
    @Operation(summary = "List creatives")
    @ApiResponse(responseCode = "200", description = "Creative page")
    public CursorPage<CreativeTemplateDto> list(
            @RequestParam(value = "cursor", required = false) @Nullable String cursor,
            @RequestParam(value = "limit", defaultValue = "20")
            @Min(1) @Max(100) int limit) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.list(ownerUserId, cursor, limit);
    }

    /**
     * Creates a new creative template for current user.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create creative template")
    @ApiResponse(responseCode = "201", description = "Creative created")
    public CreativeTemplateDto create(
            @Valid @RequestBody CreativeUpsertRequest request) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.create(ownerUserId, request);
    }

    /**
     * Returns a single creative template.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get creative template")
    @ApiResponse(responseCode = "200", description = "Creative found")
    @ApiResponse(responseCode = "404", description = "Creative not found")
    public CreativeTemplateDto get(@PathVariable("id") String templateId) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.get(ownerUserId, templateId);
    }

    /**
     * Updates an existing creative template.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update creative template")
    @ApiResponse(responseCode = "200", description = "Creative updated")
    @ApiResponse(responseCode = "404", description = "Creative not found")
    public CreativeTemplateDto update(
            @PathVariable("id") String templateId,
            @Valid @RequestBody CreativeUpsertRequest request) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.update(ownerUserId, templateId, request);
    }

    /**
     * Soft-deletes a creative template.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete creative template")
    @ApiResponse(responseCode = "204", description = "Creative deleted")
    @ApiResponse(responseCode = "404", description = "Creative not found")
    public void delete(@PathVariable("id") String templateId) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        creativeService.delete(ownerUserId, templateId);
    }

    /**
     * Returns version history for a template.
     */
    @GetMapping("/{id}/versions")
    @Operation(summary = "List creative versions")
    @ApiResponse(responseCode = "200", description = "Version history")
    @ApiResponse(responseCode = "404", description = "Creative not found")
    public List<CreativeVersionDto> versions(
            @PathVariable("id") String templateId) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.versions(ownerUserId, templateId);
    }

    /**
     * Uploads a media file into creative library.
     */
    @PostMapping(value = "/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload creative media")
    @ApiResponse(responseCode = "201", description = "Media uploaded")
    @ApiResponse(responseCode = "422", description = "Invalid media format")
    @ApiResponse(responseCode = "413", description = "Media too large")
    public CreativeMediaAssetDto uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mediaType") CreativeMediaType mediaType,
            @RequestParam(value = "caption", required = false) String caption) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        return creativeService.uploadMedia(ownerUserId, file, mediaType, caption);
    }

    /**
     * Soft-deletes uploaded media metadata.
     */
    @DeleteMapping("/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete creative media")
    @ApiResponse(responseCode = "204", description = "Media deleted")
    @ApiResponse(responseCode = "404", description = "Media not found")
    public void deleteMedia(@PathVariable("mediaId") String mediaId) {
        long ownerUserId = SecurityContextUtil.currentUserId().value();
        creativeService.deleteMedia(ownerUserId, mediaId);
    }
}
