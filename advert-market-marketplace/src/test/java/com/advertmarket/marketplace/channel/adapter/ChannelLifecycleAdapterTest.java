package com.advertmarket.marketplace.channel.adapter;

import static com.advertmarket.db.generated.tables.Channels.CHANNELS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.ChannelOwnerInfo;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

@DisplayName("ChannelLifecycleAdapter")
class ChannelLifecycleAdapterTest {

    private final DSLContext dsl =
            mock(DSLContext.class, Answers.RETURNS_DEEP_STUBS);
    private final ChannelLifecycleAdapter adapter =
            new ChannelLifecycleAdapter(dsl);

    @Test
    @DisplayName("deactivateByTelegramId returns true when row updated")
    void deactivate_returnsTrue_whenRowUpdated() {
        when(dsl.update(CHANNELS)
                .set(any(org.jooq.Field.class), (Object) any())
                .set(any(org.jooq.Field.class), any(org.jooq.Field.class))
                .set(any(org.jooq.Field.class), (Object) any())
                .where(any(org.jooq.Condition.class))
                .and(any(org.jooq.Condition.class))
                .execute()).thenReturn(1);

        assertThat(adapter.deactivateByTelegramId(-100L)).isTrue();
    }

    @Test
    @DisplayName("deactivateByTelegramId returns false when no row matched")
    void deactivate_returnsFalse_whenNoRowMatched() {
        when(dsl.update(CHANNELS)
                .set(any(org.jooq.Field.class), (Object) any())
                .set(any(org.jooq.Field.class), any(org.jooq.Field.class))
                .set(any(org.jooq.Field.class), (Object) any())
                .where(any(org.jooq.Condition.class))
                .and(any(org.jooq.Condition.class))
                .execute()).thenReturn(0);

        assertThat(adapter.deactivateByTelegramId(-100L)).isFalse();
    }

    @Test
    @DisplayName("reactivateByTelegramId returns true when row updated")
    void reactivate_returnsTrue_whenRowUpdated() {
        when(dsl.update(CHANNELS)
                .set(any(org.jooq.Field.class), (Object) any())
                .set(any(org.jooq.Field.class), any(org.jooq.Field.class))
                .set(any(org.jooq.Field.class), (Object) any())
                .where(any(org.jooq.Condition.class))
                .and(any(org.jooq.Condition.class))
                .execute()).thenReturn(1);

        assertThat(adapter.reactivateByTelegramId(-100L)).isTrue();
    }

    @Test
    @DisplayName("findOwnerByTelegramId returns empty for unknown channel")
    @SuppressWarnings("unchecked")
    void findOwner_returnsEmpty_forUnknownChannel() {
        when(dsl.select(any(org.jooq.SelectField.class),
                        any(org.jooq.SelectField.class),
                        any(org.jooq.SelectField.class))
                .from(CHANNELS)
                .where(any(org.jooq.Condition.class))
                .fetchOptional(any(RecordMapper.class)))
                .thenReturn(Optional.empty());

        assertThat(adapter.findOwnerByTelegramId(-999L)).isEmpty();
    }

    @Test
    @DisplayName("findOwnerByTelegramId returns info for registered channel")
    @SuppressWarnings("unchecked")
    void findOwner_returnsInfo_forRegisteredChannel() {
        when(dsl.select(any(org.jooq.SelectField.class),
                        any(org.jooq.SelectField.class),
                        any(org.jooq.SelectField.class))
                .from(CHANNELS)
                .where(any(org.jooq.Condition.class))
                .fetchOptional(any(RecordMapper.class)))
                .thenReturn(Optional.of(
                        new ChannelOwnerInfo(-100L, 42L, "Test")));

        Optional<ChannelOwnerInfo> result =
                adapter.findOwnerByTelegramId(-100L);
        assertThat(result).isPresent();
        assertThat(result.get().ownerId()).isEqualTo(42L);
        assertThat(result.get().title()).isEqualTo("Test");
    }
}