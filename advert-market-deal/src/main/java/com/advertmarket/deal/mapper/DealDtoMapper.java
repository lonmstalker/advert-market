package com.advertmarket.deal.mapper;

import com.advertmarket.deal.api.dto.DealDetailDto;
import com.advertmarket.deal.api.dto.DealDto;
import com.advertmarket.deal.api.dto.DealEventDto;
import com.advertmarket.deal.api.dto.DealEventRecord;
import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.DealStatus;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for deal persistence records into API DTOs.
 */
@Mapper(componentModel = "spring")
public interface DealDtoMapper {

    /** Maps persistence record to list item DTO. */
    DealDto toDto(DealRecord record);

    /** Maps persistence record + timeline to full detail DTO. */
    @Mapping(target = "timeline", source = "timeline")
    DealDetailDto toDetailDto(DealRecord record,
                              List<DealEventDto> timeline);

    /** Maps persistence event record to timeline DTO. */
    @Mapping(target = "id",
            expression = "java(requiredEventId(record))")
    DealEventDto toEventDto(DealEventRecord record);

    /**
     * Extracts required DB event id from {@link DealEventRecord}.
     */
    default long requiredEventId(DealEventRecord record) {
        return java.util.Objects.requireNonNull(record.id(),
                "deal_event.id must not be null")
                .longValue();
    }

    /**
     * Wraps raw UUID into domain {@link DealId}.
     */
    default DealId toDealId(UUID value) {
        return DealId.of(value);
    }

    /**
     * Parses DB status code into {@link DealStatus}.
     *
     * <p>Returns {@code null} for {@code null} or unknown values to keep
     * existing behavior for legacy/invalid statuses.
     */
    default DealStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return DealStatus.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
