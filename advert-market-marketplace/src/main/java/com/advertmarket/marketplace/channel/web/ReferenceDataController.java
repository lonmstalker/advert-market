package com.advertmarket.marketplace.channel.web;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import com.advertmarket.marketplace.api.dto.PostTypeDto;
import com.advertmarket.marketplace.channel.usecase.ReferenceDataUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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

    private final ReferenceDataUseCase referenceDataUseCase;

    /** Lists active categories. */
    @Operation(summary = "List all active categories")
    @GetMapping("/categories")
    @NonNull
    public List<CategoryDto> listCategories() {
        return referenceDataUseCase.listCategories();
    }

    /** Lists all post types with localized labels. */
    @Operation(summary = "List all post types with labels")
    @GetMapping("/post-types")
    @NonNull
    public List<PostTypeDto> listPostTypes() {
        return referenceDataUseCase.listPostTypes();
    }
}
