package com.advertmarket.deal.mapper;

import com.advertmarket.deal.api.dto.DealRecord;
import com.advertmarket.shared.model.DealStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.jooq.JSONB;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link DealRow} to {@link DealRecord}.
 */
@Mapper(componentModel = "spring")
public interface DealRecordMapper {

    /** Maps DB projection row to persistence record. */
    @Mapping(target = "createdAt",
            expression = "java(java.util.Objects.requireNonNull(row.createdAt()).toInstant())")
    @Mapping(target = "updatedAt",
            expression = "java(java.util.Objects.requireNonNull(row.updatedAt()).toInstant())")
    DealRecord toRecord(DealRow row);

    /**
     * Converts DB status code to {@link DealStatus}.
     */
    default DealStatus toDealStatus(String value) {
        return DealStatus.valueOf(value);
    }

    /**
     * Extracts raw JSON string from jOOQ {@link JSON} wrapper.
     */
    default String jsonData(JSONB value) {
        return value != null ? value.data() : null;
    }

    /**
     * Converts optional {@link OffsetDateTime} value to {@link Instant}.
     */
    default Instant toInstant(OffsetDateTime value) {
        return value != null ? value.toInstant() : null;
    }
}
