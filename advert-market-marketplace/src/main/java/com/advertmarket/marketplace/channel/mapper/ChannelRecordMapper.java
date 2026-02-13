package com.advertmarket.marketplace.channel.mapper;

import com.advertmarket.db.generated.tables.records.ChannelsRecord;
import com.advertmarket.marketplace.api.dto.ChannelDetailResponse;
import com.advertmarket.marketplace.api.dto.ChannelResponse;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for jOOQ {@link ChannelsRecord} to marketplace DTOs.
 */
@Mapper(componentModel = "spring")
public interface ChannelRecordMapper {

    /** Maps record to response DTO. */
    @Mapping(target = "categories", source = "categories")
    ChannelResponse toResponse(ChannelsRecord record,
                               List<String> categories);

    /** Maps record to detail DTO. */
    @Mapping(target = "categories", source = "categories")
    @Mapping(target = "pricingRules", source = "pricingRules")
    ChannelDetailResponse toDetail(ChannelsRecord record,
                                   List<String> categories,
                                   List<PricingRuleDto> pricingRules);
}
