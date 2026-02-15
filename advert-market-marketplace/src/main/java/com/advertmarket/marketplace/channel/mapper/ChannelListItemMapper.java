package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.marketplace.api.dto.ChannelListItem;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link ChannelRow} to {@link ChannelListItem}.
 */
@Mapper(componentModel = "spring")
public interface ChannelListItemMapper {

    /** Maps search projection to lightweight list item DTO. */
    @Mapping(target = "categories", source = "categories")
    ChannelListItem toDto(ChannelRow row, List<String> categories);
}

