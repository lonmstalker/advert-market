package com.advertmarket.marketplace.pricing.mapper;

import com.advertmarket.db.generated.tables.records.ChannelPricingRulesRecord;
import com.advertmarket.marketplace.api.dto.PricingRuleDto;
import com.advertmarket.marketplace.api.model.PostType;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for jOOQ {@link ChannelPricingRulesRecord} to {@link PricingRuleDto}.
 */
@Mapper(componentModel = "spring")
public interface PricingRuleRecordMapper {

    @Mapping(target = "postTypes", source = "postTypes")
    PricingRuleDto toDto(ChannelPricingRulesRecord record,
                         Set<PostType> postTypes);
}
