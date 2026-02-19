package com.gonerp.imagemanager.controller;

import com.gonerp.config.JwtTokenProvider;
import com.gonerp.imagemanager.dto.ImageInfoResponse;
import com.gonerp.imagemanager.service.ImageInfoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ImageInfoController.class)
class ImageInfoControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private ImageInfoService imageInfoService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @WithMockUser
    void findAll_returnsImages() throws Exception {
        ImageInfoResponse image = ImageInfoResponse.builder()
                .id(1L).name("Test Image").url("/api/images/files/test.jpg").build();
        when(imageInfoService.findAll(any(), any())).thenReturn(new PageImpl<>(List.of(image)));

        mockMvc.perform(get("/api/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Image"));
    }

    @Test
    @WithMockUser
    void create_multipartRequest_returnsCreated() throws Exception {
        ImageInfoResponse response = ImageInfoResponse.builder()
                .id(2L).name("New Image").url("/api/images/files/abc123.jpg").build();
        when(imageInfoService.create(any(), any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-content".getBytes());

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .param("name", "New Image")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Image"))
                .andExpect(jsonPath("$.data.url").value("/api/images/files/abc123.jpg"));
    }

    @Test
    @WithMockUser
    void findById_existingImage_returnsImage() throws Exception {
        ImageInfoResponse image = ImageInfoResponse.builder()
                .id(1L).name("Test Image").url("/api/images/files/test.jpg").build();
        when(imageInfoService.findById(1L)).thenReturn(image);

        mockMvc.perform(get("/api/images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser
    void delete_existingImage_returnsOk() throws Exception {
        doNothing().when(imageInfoService).delete(1L);

        mockMvc.perform(delete("/api/images/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void findAll_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/images"))
                .andExpect(status().isUnauthorized());
    }
}
