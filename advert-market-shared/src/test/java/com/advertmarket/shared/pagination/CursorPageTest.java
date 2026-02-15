package com.advertmarket.shared.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CursorPage pagination wrapper")
class CursorPageTest {

    @Test
    @DisplayName("Items are defensively copied")
    void items_areDefensivelyCopied() {
        var mutable = new ArrayList<>(List.of("a", "b"));
        var page = new CursorPage<>(mutable, null);

        mutable.add("c");
        assertThat(page.items()).hasSize(2);
    }

    @Test
    @DisplayName("Items list is unmodifiable")
    void items_areUnmodifiable() {
        var page = new CursorPage<>(List.of("a"), "cursor");
        assertThatThrownBy(() -> page.items().add("b"))
                .isInstanceOf(
                        UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("hasMore is true when nextCursor is present")
    void hasMore_trueWhenCursorPresent() {
        var page = new CursorPage<>(List.of("a"), "next");
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    @DisplayName("hasMore is false when nextCursor is null")
    void hasMore_falseWhenCursorNull() {
        var page = new CursorPage<>(List.of("a"), null);
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    @DisplayName("empty() returns page with no items")
    void empty_returnsEmptyPage() {
        CursorPage<String> page = CursorPage.empty();
        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
        assertThat(page.hasMore()).isFalse();
    }

    @Test
    @DisplayName("Empty string cursor is treated as hasMore")
    void emptyCursor_isTreatedAsHasMore() {
        var page = new CursorPage<>(List.of("a"), "");
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    @DisplayName("JSON includes nextCursor even when null (non_null override)")
    void json_includesNextCursorWhenNull() throws Exception {
        var mapper = new ObjectMapper();
        mapper.setDefaultPropertyInclusion(
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        var page = CursorPage.empty();
        String json = mapper.writeValueAsString(page);

        assertThat(json).contains("\"nextCursor\":null");
        assertThat(json).contains("\"hasNext\":false");
        assertThat(json).contains("\"items\":[]");
    }

    @Test
    @DisplayName("JSON includes nextCursor when present")
    void json_includesNextCursorWhenPresent() throws Exception {
        var mapper = new ObjectMapper();
        mapper.setDefaultPropertyInclusion(
                com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

        var page = new CursorPage<>(List.of("a"), "abc123");
        String json = mapper.writeValueAsString(page);

        assertThat(json).contains("\"nextCursor\":\"abc123\"");
        assertThat(json).contains("\"hasNext\":true");
    }
}
