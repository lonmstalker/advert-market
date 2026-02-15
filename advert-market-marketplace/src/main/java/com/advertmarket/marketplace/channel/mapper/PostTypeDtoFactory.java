package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.marketplace.api.dto.PostTypeDto;
import com.advertmarket.marketplace.api.model.PostType;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Component;

/**
 * Maps {@link PostType} to {@link PostTypeDto} with localized labels.
 */
@Component
public class PostTypeDtoFactory {

    private static final Map<PostType, Map<String, String>> POST_TYPE_LABELS = Map.ofEntries(
            Map.entry(PostType.REPOST, Map.of("ru", "Репост", "en", "Repost")),
            Map.entry(PostType.NATIVE, Map.of("ru", "Нативная реклама", "en", "Native ad")),
            Map.entry(PostType.STORY, Map.of("ru", "Сторис", "en", "Story")),
            Map.entry(PostType.INTEGRATION, Map.of("ru", "Интеграция", "en", "Integration")),
            Map.entry(PostType.REVIEW, Map.of("ru", "Обзор", "en", "Review")),
            Map.entry(PostType.MENTION, Map.of("ru", "Упоминание", "en", "Mention")),
            Map.entry(PostType.GIVEAWAY, Map.of("ru", "Розыгрыш", "en", "Giveaway")),
            Map.entry(PostType.PINNED, Map.of("ru", "Закреп", "en", "Pinned post")),
            Map.entry(PostType.POLL, Map.of("ru", "Опрос", "en", "Poll")),
            Map.entry(PostType.FORWARD, Map.of("ru", "Пересылка", "en", "Forward"))
    );

    /** Exposes labels for architecture-level tests. */
    @NonNull
    public static Map<PostType, Map<String, String>> labels() {
        return Map.copyOf(POST_TYPE_LABELS);
    }

    /** Maps a post type to DTO (with empty labels map fallback). */
    @NonNull
    public PostTypeDto toDto(@NonNull PostType postType) {
        return new PostTypeDto(
                postType,
                POST_TYPE_LABELS.getOrDefault(postType, Map.of()));
    }
}
