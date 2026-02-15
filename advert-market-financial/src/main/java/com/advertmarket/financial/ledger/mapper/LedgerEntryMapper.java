package com.advertmarket.financial.ledger.mapper;

import com.advertmarket.db.generated.tables.records.LedgerEntriesRecord;
import com.advertmarket.financial.api.model.LedgerEntry;
import com.advertmarket.shared.model.AccountId;
import com.advertmarket.shared.model.DealId;
import com.advertmarket.shared.model.EntryType;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for ledger entry records.
 */
@Mapper(componentModel = "spring")
public interface LedgerEntryMapper {

    /** Maps jOOQ record to read-model entry. */
    @Mapping(target = "dealId",
            expression = "java(toDealId(record.getDealId()))")
    @Mapping(target = "accountId",
            expression = "java(new AccountId(record.getAccountId()))")
    @Mapping(target = "entryType",
            expression = "java(EntryType.valueOf(record.getEntryType()))")
    @Mapping(target = "createdAt",
            expression = "java(toInstantRequired(record.getCreatedAt(), record.getId()))")
    LedgerEntry toEntry(LedgerEntriesRecord record);

    /**
     * Converts optional deal UUID to domain {@link DealId}.
     */
    default DealId toDealId(UUID value) {
        return value != null ? DealId.of(value) : null;
    }

    /**
     * Converts required {@link OffsetDateTime} to {@link Instant}.
     */
    default Instant toInstantRequired(OffsetDateTime value, Long id) {
        return Objects.requireNonNull(value,
                "created_at must not be null for ledger entry " + id)
                .toInstant();
    }
}
