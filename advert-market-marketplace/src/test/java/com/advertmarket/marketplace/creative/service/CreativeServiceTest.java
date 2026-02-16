package com.advertmarket.marketplace.creative.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.marketplace.api.dto.creative.CreativeDraftDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeEntityDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeInlineButtonDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeTextEntityType;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.marketplace.api.port.CreativeMediaAssetRepository;
import com.advertmarket.marketplace.api.port.CreativeRepository;
import com.advertmarket.marketplace.creative.storage.CreativeMediaStorage;
import com.advertmarket.marketplace.creative.storage.StoredCreativeMedia;
import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreativeService")
class CreativeServiceTest {

    private static final long USER_ID = 42L;

    @Mock
    private CreativeRepository creativeRepository;

    @Mock
    private CreativeMediaAssetRepository mediaAssetRepository;

    @Mock
    private CreativeMediaStorage mediaStorage;

    private CreativeService service;

    @BeforeEach
    void setUp() {
        service = new CreativeService(
                creativeRepository,
                mediaAssetRepository,
                mediaStorage);
    }

    @Test
    @DisplayName("Should throw CREATIVE_NOT_FOUND when creative is missing")
    void shouldThrowWhenCreativeMissing() {
        when(creativeRepository.findByOwnerAndId(USER_ID, "missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(USER_ID, "missing"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw CREATIVE_NOT_FOUND when update target is missing")
    void shouldThrowWhenUpdateTargetMissing() {
        when(creativeRepository.update(anyLong(), eq("missing"), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(
                USER_ID, "missing", upsertRequest()))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw CREATIVE_NOT_FOUND when delete target is missing")
    void shouldThrowWhenDeleteTargetMissing() {
        when(creativeRepository.softDelete(USER_ID, "missing"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.delete(USER_ID, "missing"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should throw CREATIVE_NOT_FOUND when media asset is missing")
    void shouldThrowWhenDeleteMediaTargetMissing() {
        when(mediaAssetRepository.softDelete(USER_ID, "missing"))
                .thenReturn(false);

        assertThatThrownBy(() -> service.deleteMedia(USER_ID, "missing"))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_NOT_FOUND);
    }

    @Test
    @DisplayName("Should reject invalid MIME for selected media type")
    void shouldRejectInvalidMime() {
        var file = new MockMultipartFile(
                "file",
                "payload.txt",
                "text/plain",
                "not-an-image".getBytes());

        assertThatThrownBy(() -> service.uploadMedia(
                USER_ID, file, CreativeMediaType.PHOTO, null))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_INVALID_FORMAT);

        verify(mediaStorage, never()).store(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Should reject oversized media")
    void shouldRejectOversizedMedia() {
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        var file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                oversized);

        assertThatThrownBy(() -> service.uploadMedia(
                USER_ID, file, CreativeMediaType.PHOTO, null))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCodes.CREATIVE_TOO_LARGE);

        verify(mediaStorage, never()).store(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("Should upload media and persist metadata")
    void shouldUploadAndPersistMedia() {
        var file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "jpeg-bytes".getBytes());
        when(mediaStorage.store(anyLong(), any(), any(), any()))
                .thenReturn(new StoredCreativeMedia(
                        "https://cdn.example.com/u42/banner.jpg",
                        "https://cdn.example.com/u42/banner-thumb.jpg",
                        "image/jpeg",
                        (long) file.getSize(),
                        "banner.jpg"));
        when(mediaAssetRepository.create(anyLong(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1));

        CreativeMediaAssetDto uploaded = service.uploadMedia(
                USER_ID, file, CreativeMediaType.PHOTO, "Banner");

        assertThat(uploaded.url())
                .isEqualTo("https://cdn.example.com/u42/banner.jpg");
        assertThat(uploaded.type()).isEqualTo(CreativeMediaType.PHOTO);
        assertThat(uploaded.caption()).isEqualTo("Banner");
        assertThat(uploaded.id()).isNotBlank();
        UUID.fromString(uploaded.id());

        verify(mediaStorage).store(eq(USER_ID), any(), eq(CreativeMediaType.PHOTO), eq(file));
        verify(mediaAssetRepository).create(eq(USER_ID), any());
    }

    @Test
    @DisplayName("Should return versions for existing creative")
    void shouldReturnVersions() {
        when(creativeRepository.findByOwnerAndId(USER_ID, "template-1"))
                .thenReturn(Optional.of(template()));
        when(creativeRepository.findVersions(USER_ID, "template-1"))
                .thenReturn(List.of(new CreativeVersionDto(
                        1, template().draft(), OffsetDateTime.now())));

        var versions = service.versions(USER_ID, "template-1");

        assertThat(versions).hasSize(1);
        assertThat(versions.getFirst().version()).isEqualTo(1);
    }

    private static CreativeUpsertRequest upsertRequest() {
        return new CreativeUpsertRequest(
                "Template",
                "Body",
                List.of(new CreativeEntityDto(
                        CreativeTextEntityType.BOLD, 0, 4, null, null)),
                List.of(),
                List.of(List.of(new CreativeInlineButtonDto(
                        "btn-1", "Open", "https://example.com"))),
                false);
    }

    private static CreativeTemplateDto template() {
        var now = OffsetDateTime.now();
        return new CreativeTemplateDto(
                "template-1",
                "Template",
                new CreativeDraftDto(
                        "Body",
                        List.of(new CreativeEntityDto(
                                CreativeTextEntityType.BOLD,
                                0,
                                4,
                                null,
                                null)),
                        List.of(),
                        List.of(List.of(new CreativeInlineButtonDto(
                                "btn-1",
                                "Open",
                                "https://example.com"))),
                        false),
                1,
                now,
                now);
    }
}
