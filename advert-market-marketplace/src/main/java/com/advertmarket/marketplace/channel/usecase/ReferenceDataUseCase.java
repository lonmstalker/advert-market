package com.advertmarket.marketplace.channel.usecase;

import com.advertmarket.marketplace.api.dto.CategoryDto;
import com.advertmarket.marketplace.api.dto.PostTypeDto;
import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.api.port.CategoryRepository;
import com.advertmarket.marketplace.channel.mapper.PostTypeDtoFactory;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;

/**
 * Application use-case for public reference data.
 */
@Service
@RequiredArgsConstructor
public class ReferenceDataUseCase {

    private final CategoryRepository categoryRepository;
    private final PostTypeDtoFactory postTypeDtoFactory;

    /** Lists active categories. */
    @NonNull
    public List<CategoryDto> listCategories() {
        return categoryRepository.findAllActive();
    }

    /** Lists all post types with localized labels. */
    @NonNull
    public List<PostTypeDto> listPostTypes() {
        return Arrays.stream(PostType.values())
                .map(postTypeDtoFactory::toDto)
                .toList();
    }
}

