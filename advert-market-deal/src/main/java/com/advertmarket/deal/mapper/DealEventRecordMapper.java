package com.advertmarket.deal.mapper;

import com.advertmarket.deal.api.dto.DealEventRecord;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.jooq.JSON;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for {@link DealEventRow} to {@link DealEventRecord}.
 */
@Mapper(componentModel = "spring")
public interface DealEventRecordMapper {

    /** Maps DB projection row to persistence event record. */
    @Mapping(target = "payload", source = "payload")
    @Mapping(target = "createdAt",
            expression = "java(requiredInstant(row.createdAt()))")
    DealEventRecord toRecord(DealEventRow row);

    /**
     * Extracts JSON payload string from jOOQ {@link JSON} wrapper.
     * Defaults to {@code "{}"} when DB value is {@code null}.
     */
    default String payload(JSON value) {
        return value != null ? value.data() : "{}";
    }

    /**
     * Converts required {@link OffsetDateTime} to {@link Instant}.
     */
    default Instant requiredInstant(OffsetDateTime value) {
        return Objects.requireNonNull(value).toInstant();
    }
}
