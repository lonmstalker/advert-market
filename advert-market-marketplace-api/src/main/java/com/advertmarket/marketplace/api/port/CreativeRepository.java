package com.advertmarket.marketplace.api.port;

import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.shared.pagination.CursorPage;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Persistence port for user creative templates.
 */
public interface CreativeRepository {

    /**
     * Returns owner-scoped creative templates using cursor pagination.
     */
    @NonNull
    CursorPage<CreativeTemplateDto> findByOwner(
            long ownerUserId,
            @Nullable String cursor,
            int limit);

    /**
     * Returns a single template by owner and identifier.
     */
    @NonNull
    Optional<CreativeTemplateDto> findByOwnerAndId(
            long ownerUserId,
            @NonNull String templateId);

    /**
     * Creates a new owner-scoped creative template.
     */
    @NonNull
    CreativeTemplateDto create(
            long ownerUserId,
            @NonNull CreativeUpsertRequest request);

    /**
     * Updates an existing owner-scoped template.
     */
    @NonNull
    Optional<CreativeTemplateDto> update(
            long ownerUserId,
            @NonNull String templateId,
            @NonNull CreativeUpsertRequest request);

    /**
     * Marks a template as deleted and returns {@code true} when a row was updated.
     */
    boolean softDelete(
            long ownerUserId,
            @NonNull String templateId);

    /**
     * Returns version history for a template.
     */
    @NonNull
    List<CreativeVersionDto> findVersions(
            long ownerUserId,
            @NonNull String templateId);
}
