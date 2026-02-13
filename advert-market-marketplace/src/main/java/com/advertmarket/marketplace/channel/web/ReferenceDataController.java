package com.advertmarket.marketplace.channel.web;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import com.advertmarket.marketplace.api.dto.PostTypeDto;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public reference data endpoints for categories and post types.
 */
@Tag(name = "Reference Data")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReferenceDataController {

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

    private final CategoryRepository categoryRepository;

    @Operation(summary = "List all active categories")
    @GetMapping("/categories")
    @NonNull
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAllActive();
    }

    @Operation(summary = "List all post types with labels")
    @GetMapping("/post-types")
    @NonNull
    public List<PostTypeDto> listPostTypes() {
        return Arrays.stream(PostType.values())
                .map(pt -> new PostTypeDto(pt,
                        POST_TYPE_LABELS.getOrDefault(pt, Map.of())))
                .toList();
    }
}
