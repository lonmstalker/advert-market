package com.advertmarket.marketplace.creative.repository;

import static com.advertmarket.db.generated.tables.CreativeTemplateVersions.CREATIVE_TEMPLATE_VERSIONS;
import static com.advertmarket.db.generated.tables.CreativeTemplates.CREATIVE_TEMPLATES;

import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.shared.json.JsonFacade;
import com.advertmarket.shared.pagination.CursorCodec;
import com.advertmarket.shared.pagination.CursorPage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/**
 * Persistence adapter for creative templates and version history.
 */
@Repository
@RequiredArgsConstructor
public class JooqCreativeTemplateRepository implements CreativeRepository {

    private static final int MAX_LIMIT = 100;

    private final DSLContext dsl;
    private final JsonFacade jsonFacade;

    @Override
    @NonNull
    public CursorPage<CreativeTemplateDto> findByOwner(
            long ownerUserId,
            @Nullable String cursor,
            int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        Condition condition = CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId)
                .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse());

        if (cursor != null && !cursor.isBlank()) {
            Map<String, String> fields = CursorCodec.decode(cursor);
            OffsetDateTime updatedAt = OffsetDateTime.parse(
                    CreativeJsonConverter.readCursorField(fields, "updatedAt"));
            UUID id = UUID.fromString(
                    CreativeJsonConverter.readCursorField(fields, "id"));
            condition = condition.and(
                    CREATIVE_TEMPLATES.UPDATED_AT.lt(updatedAt)
                            .or(CREATIVE_TEMPLATES.UPDATED_AT.eq(updatedAt)
                                    .and(CREATIVE_TEMPLATES.ID.lt(id))));
        }

        var records = dsl.selectFrom(CREATIVE_TEMPLATES)
                .where(condition)
                .orderBy(CREATIVE_TEMPLATES.UPDATED_AT.desc(),
                        CREATIVE_TEMPLATES.ID.desc())
                .limit(safeLimit + 1)
                .fetch();

        boolean hasNext = records.size() > safeLimit;
        var pageRecords = hasNext ? records.subList(0, safeLimit) : records;
        String nextCursor = null;
        if (hasNext && !pageRecords.isEmpty()) {
            var last = pageRecords.get(pageRecords.size() - 1);
            nextCursor = CursorCodec.encode(Map.of(
                    "updatedAt", last.getUpdatedAt().toString(),
                    "id", last.getId().toString()));
        }

        return new CursorPage<>(
                pageRecords.stream().map(this::toTemplate).toList(),
                nextCursor);
    }

    @Override
    @NonNull
    public Optional<CreativeTemplateDto> findByOwnerAndId(
            long ownerUserId,
            @NonNull String templateId) {
        Optional<UUID> uuid = parseUuid(templateId);
        if (uuid.isEmpty()) {
            return Optional.empty();
        }
        return dsl.selectFrom(CREATIVE_TEMPLATES)
                .where(CREATIVE_TEMPLATES.ID.eq(uuid.get()))
                .and(CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse())
                .fetchOptional()
                .map(this::toTemplate);
    }

    @Override
    @NonNull
    public CreativeTemplateDto create(
            long ownerUserId,
            @NonNull CreativeUpsertRequest request) {
        UUID templateId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        int version = 1;
        var draftJson = CreativeJsonConverter.draftToJson(
                request.toDraft(),
                jsonFacade);

        var record = dsl.insertInto(CREATIVE_TEMPLATES)
                .set(CREATIVE_TEMPLATES.ID, templateId)
                .set(CREATIVE_TEMPLATES.OWNER_USER_ID, ownerUserId)
                .set(CREATIVE_TEMPLATES.TITLE, request.title())
                .set(CREATIVE_TEMPLATES.DRAFT, draftJson)
                .set(CREATIVE_TEMPLATES.VERSION, version)
                .set(CREATIVE_TEMPLATES.CREATED_AT, now)
                .set(CREATIVE_TEMPLATES.UPDATED_AT, now)
                .returning()
                .fetchSingle();

        dsl.insertInto(CREATIVE_TEMPLATE_VERSIONS)
                .set(CREATIVE_TEMPLATE_VERSIONS.TEMPLATE_ID, templateId)
                .set(CREATIVE_TEMPLATE_VERSIONS.VERSION, version)
                .set(CREATIVE_TEMPLATE_VERSIONS.DRAFT, draftJson)
                .set(CREATIVE_TEMPLATE_VERSIONS.CREATED_AT, now)
                .execute();

        return toTemplate(record);
    }

    @Override
    @NonNull
    public Optional<CreativeTemplateDto> update(
            long ownerUserId,
            @NonNull String templateId,
            @NonNull CreativeUpsertRequest request) {
        Optional<UUID> uuid = parseUuid(templateId);
        if (uuid.isEmpty()) {
            return Optional.empty();
        }

        var current = dsl.selectFrom(CREATIVE_TEMPLATES)
                .where(CREATIVE_TEMPLATES.ID.eq(uuid.get()))
                .and(CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse())
                .fetchOptional();
        if (current.isEmpty()) {
            return Optional.empty();
        }

        int nextVersion = current.get().getVersion() + 1;
        OffsetDateTime now = OffsetDateTime.now();
        var draftJson = CreativeJsonConverter.draftToJson(
                request.toDraft(),
                jsonFacade);

        var updated = dsl.update(CREATIVE_TEMPLATES)
                .set(CREATIVE_TEMPLATES.TITLE, request.title())
                .set(CREATIVE_TEMPLATES.DRAFT, draftJson)
                .set(CREATIVE_TEMPLATES.VERSION, nextVersion)
                .set(CREATIVE_TEMPLATES.UPDATED_AT, now)
                .where(CREATIVE_TEMPLATES.ID.eq(uuid.get()))
                .and(CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse())
                .returning()
                .fetchOptional();
        if (updated.isEmpty()) {
            return Optional.empty();
        }

        dsl.insertInto(CREATIVE_TEMPLATE_VERSIONS)
                .set(CREATIVE_TEMPLATE_VERSIONS.TEMPLATE_ID, uuid.get())
                .set(CREATIVE_TEMPLATE_VERSIONS.VERSION, nextVersion)
                .set(CREATIVE_TEMPLATE_VERSIONS.DRAFT, draftJson)
                .set(CREATIVE_TEMPLATE_VERSIONS.CREATED_AT, now)
                .execute();

        return updated.map(this::toTemplate);
    }

    @Override
    public boolean softDelete(
            long ownerUserId,
            @NonNull String templateId) {
        Optional<UUID> uuid = parseUuid(templateId);
        if (uuid.isEmpty()) {
            return false;
        }
        int rows = dsl.update(CREATIVE_TEMPLATES)
                .set(CREATIVE_TEMPLATES.IS_DELETED, true)
                .set(CREATIVE_TEMPLATES.DELETED_AT, OffsetDateTime.now())
                .set(CREATIVE_TEMPLATES.UPDATED_AT, OffsetDateTime.now())
                .where(CREATIVE_TEMPLATES.ID.eq(uuid.get()))
                .and(CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId))
                .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse())
                .execute();
        return rows > 0;
    }

    @Override
    @NonNull
    public List<CreativeVersionDto> findVersions(
            long ownerUserId,
            @NonNull String templateId) {
        Optional<UUID> uuid = parseUuid(templateId);
        if (uuid.isEmpty()) {
            return List.of();
        }
        return dsl.select(CREATIVE_TEMPLATE_VERSIONS.VERSION,
                        CREATIVE_TEMPLATE_VERSIONS.DRAFT,
                        CREATIVE_TEMPLATE_VERSIONS.CREATED_AT)
                .from(CREATIVE_TEMPLATE_VERSIONS)
                .where(CREATIVE_TEMPLATE_VERSIONS.TEMPLATE_ID.eq(uuid.get()))
                .andExists(dsl.selectOne()
                        .from(CREATIVE_TEMPLATES)
                        .where(CREATIVE_TEMPLATES.ID.eq(uuid.get()))
                        .and(CREATIVE_TEMPLATES.OWNER_USER_ID.eq(ownerUserId))
                        .and(CREATIVE_TEMPLATES.IS_DELETED.isFalse()))
                .orderBy(CREATIVE_TEMPLATE_VERSIONS.VERSION.desc())
                .fetch(record -> new CreativeVersionDto(
                        record.get(CREATIVE_TEMPLATE_VERSIONS.VERSION),
                        CreativeJsonConverter.draftFromJson(
                                record.get(CREATIVE_TEMPLATE_VERSIONS.DRAFT),
                                jsonFacade),
                        record.get(CREATIVE_TEMPLATE_VERSIONS.CREATED_AT)));
    }

    private CreativeTemplateDto toTemplate(
            com.advertmarket.db.generated.tables.records.CreativeTemplatesRecord record) {
        return new CreativeTemplateDto(
                record.getId().toString(),
                record.getTitle(),
                CreativeJsonConverter.draftFromJson(record.getDraft(), jsonFacade),
                record.getVersion(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }

    private Optional<UUID> parseUuid(String raw) {
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
