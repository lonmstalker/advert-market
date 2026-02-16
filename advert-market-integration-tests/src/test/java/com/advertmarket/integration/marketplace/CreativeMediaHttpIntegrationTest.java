package com.advertmarket.integration.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import com.advertmarket.identity.security.JwtTokenProvider;
import com.advertmarket.integration.marketplace.config.CreativeHttpTestConfig;
import com.advertmarket.integration.support.ContainerProperties;
import com.advertmarket.integration.support.DatabaseSupport;
import com.advertmarket.integration.support.TestDataFactory;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import java.net.URI;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

/**
 * Verifies creative media upload works against a live MinIO instance.
 */
@SpringBootTest(
        classes = CreativeHttpTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@DisplayName("Creative media HTTP integration with MinIO")
class CreativeMediaHttpIntegrationTest {

    private static final long USER_ID = 101L;
    private static final int MINIO_API_PORT = 9000;
    private static final String BUCKET = "creative-media";
    private static final String ACCESS_KEY = "minio";
    private static final String SECRET_KEY = "minio123";

    @SuppressWarnings("resource")
    private static final GenericContainer<?> MINIO =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withExposedPorts(MINIO_API_PORT)
                    .withEnv("MINIO_ROOT_USER", ACCESS_KEY)
                    .withEnv("MINIO_ROOT_PASSWORD", SECRET_KEY)
                    .withCommand("server", "/data", "--console-address", ":9001");

    static {
        MINIO.start();
        ensureBucketExists();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        ContainerProperties.registerAll(registry);
        registry.add("app.marketplace.creatives.storage.enabled", () -> "true");
        registry.add("app.marketplace.creatives.storage.endpoint",
                CreativeMediaHttpIntegrationTest::minioEndpoint);
        registry.add("app.marketplace.creatives.storage.region", () -> "us-east-1");
        registry.add("app.marketplace.creatives.storage.bucket", () -> BUCKET);
        registry.add("app.marketplace.creatives.storage.access-key", () -> ACCESS_KEY);
        registry.add("app.marketplace.creatives.storage.secret-key", () -> SECRET_KEY);
        registry.add("app.marketplace.creatives.storage.public-base-url",
                () -> minioEndpoint() + "/" + BUCKET);
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
    @DisplayName("POST /api/v1/creatives/media stores object in MinIO and returns media DTO")
    void uploadMediaStoresObjectInMinio() {
        var multipartBuilder = new MultipartBodyBuilder();
        multipartBuilder.part("mediaType", "PHOTO");
        multipartBuilder.part("caption", "Banner");
        var file = new ByteArrayResource("png-bytes".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "banner.png";
            }
        };
        multipartBuilder.part("file", file).contentType(MediaType.IMAGE_PNG);

        CreativeMediaAssetDto response = webClient.post()
                .uri("/api/v1/creatives/media")
                .headers(h -> h.setBearerAuth(jwt(USER_ID)))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(multipartBuilder.build()))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CreativeMediaAssetDto.class)
                .returnResult()
                .getResponseBody();

        assertThat(response).isNotNull();
        assertThat(response.type().name()).isEqualTo("PHOTO");
        assertThat(response.url())
                .contains("/creative-media/creatives/photo/u-" + USER_ID + "/");
        assertThat(response.caption()).isEqualTo("Banner");

        try (S3Client client = minioClient()) {
            var listed = client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(BUCKET)
                    .prefix("creatives/photo/u-" + USER_ID + "/")
                    .build());
            assertThat(listed.contents()).hasSize(1);
        }
    }

    private String jwt(long userId) {
        return TestDataFactory.jwt(jwtTokenProvider, userId);
    }

    private static String minioEndpoint() {
        return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(MINIO_API_PORT);
    }

    private static S3Client minioClient() {
        return S3Client.builder()
                .endpointOverride(URI.create(minioEndpoint()))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    private static void ensureBucketExists() {
        try (S3Client client = minioClient()) {
            client.createBucket(CreateBucketRequest.builder()
                    .bucket(BUCKET)
                    .build());
        }
    }
}
