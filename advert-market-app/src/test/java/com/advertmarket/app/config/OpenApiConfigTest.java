package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OpenAPI configuration customizers.
 */
class OpenApiConfigTest {

    @Test
    void excludeInternalPathsCustomizer_removesInternalPaths() {
        OpenAPI openApi = new OpenAPI().paths(new Paths()
                .addPathItem("/api/v1/channels", new PathItem())
                .addPathItem("/internal/v1/canary", new PathItem())
                .addPathItem("/internal/v1/worker-events", new PathItem()));

        OpenApiConfig config = new OpenApiConfig();
        config.excludeInternalPathsCustomizer().customise(openApi);

        assertThat(openApi.getPaths())
                .containsKey("/api/v1/channels")
                .doesNotContainKeys(
                        "/internal/v1/canary",
                        "/internal/v1/worker-events");
    }
}
