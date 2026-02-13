package com.advertmarket.integration.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.marketplace.api.model.PostType;
import com.advertmarket.marketplace.channel.web.ReferenceDataController;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates that all PostType enum values have complete
 * localized labels in ReferenceDataController.
 */
@DisplayName("Locale completeness tests")
class LocaleCompletenessTest {

    private static final Set<String> REQUIRED_LOCALES =
            Set.of("ru", "en");

    @Test
    @DisplayName("Every PostType must have labels for all required locales")
    @SuppressWarnings("unchecked")
    void postTypeLabelsShouldCoverAllEnumValues() throws Exception {
        Field field = ReferenceDataController.class
                .getDeclaredField("POST_TYPE_LABELS");
        field.setAccessible(true);
        var labels = (Map<PostType, Map<String, String>>)
                field.get(null);

        for (PostType postType : PostType.values()) {
            assertThat(labels)
                    .as("POST_TYPE_LABELS must contain %s", postType)
                    .containsKey(postType);

            Map<String, String> localeMap = labels.get(postType);
            assertThat(localeMap.keySet())
                    .as("Labels for %s must include all required locales",
                            postType)
                    .containsAll(REQUIRED_LOCALES);

            for (String locale : REQUIRED_LOCALES) {
                assertThat(localeMap.get(locale))
                        .as("Label for %s/%s must not be blank",
                                postType, locale)
                        .isNotBlank();
            }
        }
    }
}
