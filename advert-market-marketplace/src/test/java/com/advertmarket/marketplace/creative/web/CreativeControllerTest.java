package com.advertmarket.marketplace.creative.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.advertmarket.marketplace.api.dto.creative.CreativeDraftDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeEntityDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeInlineButtonDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaAssetDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeMediaType;
import com.advertmarket.marketplace.api.dto.creative.CreativeTemplateDto;
import com.advertmarket.marketplace.api.dto.creative.CreativeTextEntityType;
import com.advertmarket.marketplace.api.dto.creative.CreativeUpsertRequest;
import com.advertmarket.marketplace.api.dto.creative.CreativeVersionDto;
import com.advertmarket.marketplace.creative.service.CreativeService;
import com.advertmarket.shared.model.UserId;
import com.advertmarket.shared.pagination.CursorPage;
import com.advertmarket.shared.security.PrincipalAuthentication;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("CreativeController — /api/v1/creatives endpoints")
@ExtendWith(MockitoExtension.class)
class CreativeControllerTest {

    private static final long USER_ID = 123L;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreativeService creativeService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CreativeController(creativeService))
                .build();

        SecurityContextHolder.getContext()
                .setAuthentication(testAuth());
    }

    @Test
    @DisplayName("Should return paginated creative list")
    void shouldListCreatives() throws Exception {
        when(creativeService.list(anyLong(),
                nullable(String.class), anyInt()))
                .thenReturn(new CursorPage<>(List.of(template()), null));

        mockMvc.perform(get("/api/v1/creatives")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value("template-1"))
                .andExpect(jsonPath("$.items[0].title").value("Template"));

        verify(creativeService).list(USER_ID, null, 20);
    }

    @Test
    @DisplayName("Should create creative template")
    void shouldCreateCreative() throws Exception {
        when(creativeService.create(anyLong(), any()))
                .thenReturn(template());

        mockMvc.perform(post("/api/v1/creatives")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upsertRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("template-1"))
                .andExpect(jsonPath("$.draft.keyboardRows[0][0].text").value("Open"));
    }

    @Test
    @DisplayName("Should update creative template")
    void shouldUpdateCreative() throws Exception {
        when(creativeService.update(anyLong(), anyString(), any()))
                .thenReturn(template());

        mockMvc.perform(put("/api/v1/creatives/{id}", "template-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(upsertRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("template-1"));
    }

    @Test
    @DisplayName("Should delete creative template")
    void shouldDeleteCreative() throws Exception {
        mockMvc.perform(delete("/api/v1/creatives/{id}", "template-1"))
                .andExpect(status().isNoContent());

        verify(creativeService).delete(USER_ID, "template-1");
    }

    @Test
    @DisplayName("Should return creative versions")
    void shouldListVersions() throws Exception {
        when(creativeService.versions(anyLong(), anyString()))
                .thenReturn(List.of(new CreativeVersionDto(1, draft(), OffsetDateTime.now())));

        mockMvc.perform(get("/api/v1/creatives/{id}/versions", "template-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value(1));
    }

    @Test
    @DisplayName("Should upload media asset")
    void shouldUploadMedia() throws Exception {
        when(creativeService.uploadMedia(anyLong(), any(), any(), anyString()))
                .thenReturn(media());
        var file = new MockMultipartFile(
                "file",
                "banner.jpg",
                "image/jpeg",
                "fake-image".getBytes());

        mockMvc.perform(multipart("/api/v1/creatives/media")
                        .file(file)
                        .param("mediaType", "PHOTO")
                        .param("caption", "Banner image"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("media-1"))
                .andExpect(jsonPath("$.type").value("PHOTO"));
    }

    private CreativeUpsertRequest upsertRequest() {
        return new CreativeUpsertRequest(
                "Template",
                "Hello",
                List.of(new CreativeEntityDto(CreativeTextEntityType.BOLD, 0, 5, null, null)),
                List.of(media()),
                List.of(List.of(new CreativeInlineButtonDto(
                        "btn-1",
                        "Open",
                        "https://example.com"))),
                false);
    }

    private CreativeTemplateDto template() {
        var now = OffsetDateTime.now();
        return new CreativeTemplateDto(
                "template-1",
                "Template",
                draft(),
                1,
                now,
                now);
    }

    private CreativeDraftDto draft() {
        return new CreativeDraftDto(
                "Hello",
                List.of(new CreativeEntityDto(CreativeTextEntityType.BOLD, 0, 5, null, null)),
                List.of(media()),
                List.of(List.of(new CreativeInlineButtonDto(
                        "btn-1",
                        "Open",
                        "https://example.com"))),
                false);
    }

    private CreativeMediaAssetDto media() {
        return new CreativeMediaAssetDto(
                "media-1",
                CreativeMediaType.PHOTO,
                "https://cdn.example.com/media-1.jpg",
                null,
                "banner.jpg",
                "4 KB",
                "image/jpeg",
                4096L,
                "Banner image");
    }

    private static PrincipalAuthentication testAuth() {
        return new PrincipalAuthentication() {
            @Override
            public UserId getUserId() {
                return new UserId(USER_ID);
            }

            @Override
            public String getJti() {
                return "creative-controller-test-jti";
            }

            @Override
            public boolean isOperator() {
                return false;
            }

            @Override
            public long getTokenExpSeconds() {
                return 0;
            }

            @Override
            public Collection<? extends GrantedAuthority> getAuthorities() {
                return List.of();
            }

            @Override
            public Object getCredentials() {
                return null;
            }

            @Override
            public Object getDetails() {
                return null;
            }

            @Override
            public Object getPrincipal() {
                return new UserId(USER_ID);
            }

            @Override
            public boolean isAuthenticated() {
                return true;
            }

            @Override
            public void setAuthenticated(boolean isAuthenticated) {
            }

            @Override
            public String getName() {
                return String.valueOf(USER_ID);
            }
        };
    }
}
