package com.gonerp.imagemanager.service;

import com.gonerp.imagemanager.dto.ImageInfoResponse;
import com.gonerp.imagemanager.model.ImageInfo;
import com.gonerp.imagemanager.repository.ImageInfoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageInfoServiceTest {

    @Mock
    private ImageInfoRepository imageInfoRepository;

    @InjectMocks
    private ImageInfoService imageInfoService;

    @TempDir
    Path tempDir;

    private ImageInfo testImage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageInfoService, "uploadDir", tempDir.toString());
        testImage = ImageInfo.builder()
                .id(1L)
                .name("Test Image")
                .url("/api/images/files/test.jpg")
                .build();
    }

    @Test
    void findAll_returnsPageOfImages() {
        Page<ImageInfo> page = new PageImpl<>(List.of(testImage));
        when(imageInfoRepository.findAllWithSearch(any(), any())).thenReturn(page);

        Page<ImageInfoResponse> result = imageInfoService.findAll(null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Image");
    }

    @Test
    void findById_existingImage_returnsImage() {
        when(imageInfoRepository.findById(1L)).thenReturn(Optional.of(testImage));

        ImageInfoResponse result = imageInfoService.findById(1L);

        assertThat(result.getName()).isEqualTo("Test Image");
        assertThat(result.getUrl()).isEqualTo("/api/images/files/test.jpg");
    }

    @Test
    void findById_nonExisting_throwsException() {
        when(imageInfoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageInfoService.findById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_withFile_storesFileAndReturnsResponse() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        when(imageInfoRepository.save(any(ImageInfo.class))).thenAnswer(inv -> {
            ImageInfo img = inv.getArgument(0);
            img.setId(2L);
            return img;
        });

        ImageInfoResponse result = imageInfoService.create("New Image", file);

        assertThat(result.getName()).isEqualTo("New Image");
        assertThat(result.getUrl()).startsWith("/api/images/files/");
        assertThat(result.getUrl()).endsWith(".jpg");
    }

    @Test
    void update_nameOnly_doesNotChangeFile() {
        when(imageInfoRepository.findById(1L)).thenReturn(Optional.of(testImage));
        when(imageInfoRepository.save(any(ImageInfo.class))).thenReturn(testImage);

        ImageInfoResponse result = imageInfoService.update(1L, "Updated Name", null);

        assertThat(result).isNotNull();
        verify(imageInfoRepository).save(testImage);
    }

    @Test
    void delete_existingImage_success() {
        when(imageInfoRepository.findById(1L)).thenReturn(Optional.of(testImage));

        imageInfoService.delete(1L);

        verify(imageInfoRepository).deleteById(1L);
    }

    @Test
    void delete_nonExisting_throwsException() {
        when(imageInfoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageInfoService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
