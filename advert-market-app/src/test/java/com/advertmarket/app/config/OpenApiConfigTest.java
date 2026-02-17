package com.advertmarket.app.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for OpenAPI configuration customizers.
 */
class OpenApiConfigTest {

    @Test
    void openApi_containsBearerAuthScheme() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openApi = config.openApi();

        assertThat(openApi.getInfo().getTitle()).isEqualTo("Advert Market API");
        assertThat(openApi.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(openApi.getComponents().getSecuritySchemes().get("bearerAuth").getScheme())
                .isEqualTo("bearer");
    }
}
