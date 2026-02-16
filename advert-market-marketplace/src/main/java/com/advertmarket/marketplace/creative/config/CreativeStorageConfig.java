package com.advertmarket.marketplace.creative.config;

import com.advertmarket.marketplace.creative.storage.CreativeMediaStorage;
import com.advertmarket.marketplace.creative.storage.S3CreativeMediaStorage;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Wires S3-compatible storage adapter for creative media uploads.
 */
@Configuration
@EnableConfigurationProperties(CreativeStorageProperties.class)
public class CreativeStorageConfig {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.marketplace.creatives.storage",
            name = "enabled",
            havingValue = "true")
    S3Client creativeS3Client(CreativeStorageProperties properties) {
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build());

        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(properties.endpoint()));
        }

        if (properties.accessKey() != null && !properties.accessKey().isBlank()
                && properties.secretKey() != null && !properties.secretKey().isBlank()) {
            builder = builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    properties.accessKey(),
                                    properties.secretKey())));
        } else {
            builder = builder.credentialsProvider(
                    DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    @ConditionalOnProperty(
            prefix = "app.marketplace.creatives.storage",
            name = "enabled",
            havingValue = "true")
    CreativeMediaStorage s3CreativeMediaStorage(
            S3Client s3Client,
            CreativeStorageProperties properties) {
        return new S3CreativeMediaStorage(s3Client, properties);
    }

    @Bean
    @ConditionalOnMissingBean(CreativeMediaStorage.class)
    CreativeMediaStorage disabledCreativeMediaStorage() {
        return (ownerUserId, mediaAssetId, mediaType, file) -> {
            throw new DomainException(
                    ErrorCodes.SERVICE_UNAVAILABLE,
                    "Creative media storage is disabled");
        };
    }
}
