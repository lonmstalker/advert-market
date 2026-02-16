package com.advertmarket.integration.marketplace;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.CreativeHttpTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import java.nio.charset.StandardCharsets;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

/**
 * Verifies creative media upload degrades gracefully when MinIO is unavailable.
 */
@SpringBootTest(
        classes = CreativeHttpTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Creative media HTTP integration without MinIO")
class CreativeMediaStorageUnavailableIntegrationTest {

    private static final long USER_ID = 102L;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
        registry.add("app.marketplace.creatives.storage.enabled", () -> "true");
        registry.add("app.marketplace.creatives.storage.endpoint", () -> "http://127.0.0.1:9");
        registry.add("app.marketplace.creatives.storage.region", () -> "us-east-1");
        registry.add("app.marketplace.creatives.storage.bucket", () -> "creative-media");
        registry.add("app.marketplace.creatives.storage.access-key", () -> "minio");
        registry.add("app.marketplace.creatives.storage.secret-key", () -> "minio123");
        registry.add("app.marketplace.creatives.storage.public-base-url",
                () -> "http://127.0.0.1:9/creative-media");
        registry.add("app.marketplace.creatives.storage.key-prefix", () -> "creatives");
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private DSLContext dsl;

    private WebTestClient webClient;

    @BeforeEach
    void setUp() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
        DatabaseSupport.cleanAllTables(dsl);
        TestDataFactory.upsertUser(dsl, USER_ID);
    }

    @Test
    @DisplayName("POST /api/v1/creatives/media returns 503 when storage is absent")
    void uploadMediaReturns503WhenStorageUnavailable() {
        var multipartBuilder = new MultipartBodyBuilder();
        multipartBuilder.part("mediaType", "PHOTO");
        var file = new ByteArrayResource("png-bytes".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "banner.png";
            }
        };
        multipartBuilder.part("file", file).contentType(MediaType.IMAGE_PNG);

        webClient.post()
                .uri("/api/v1/creatives/media")
                .headers(h -> h.setBearerAuth(jwt(USER_ID)))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBuilder.build()))
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.error_code").isEqualTo("SERVICE_UNAVAILABLE");
    }

    @Test
    @DisplayName("POST /api/v1/creatives still creates template when media storage is absent")
    void createTemplateWorksWithoutStorage() {
        String payload = """
                {
                  "title": "No MinIO Template",
                  "text": "Creative body",
                  "entities": [],
                  "media": [],
                  "keyboardRows": [],
                  "disableWebPagePreview": false
                }
                """;

        webClient.post()
                .uri("/api/v1/creatives")
                .headers(h -> h.setBearerAuth(jwt(USER_ID)))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.title").isEqualTo("No MinIO Template");
    }

    private String jwt(long userId) {
        return TestDataFactory.jwt(jwtTokenProvider, userId);
    }
}
