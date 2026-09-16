package org.example.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.example.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileServiceImplTest {

    @TempDir
    private Path tempDir;

    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {

        fileService = new FileServiceImpl();
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
    }

    @Test
    void saveFile_allowedImageType_savesUnderGeneratedNameAndReturnsRelativePath() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", "fake-png-bytes".getBytes());

        String relativePath = fileService.saveFile("avatars", file);

        assertThat(relativePath).startsWith("avatars/").endsWith(".png");
        assertThat(Files.exists(tempDir.resolve(relativePath))).isTrue();
    }

    @Test
    void saveFile_unsupportedContentType_throwsBadRequestExceptionAndSavesNothing() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.svg", "image/svg+xml", "<svg></svg>".getBytes());

        assertThatThrownBy(() -> fileService.saveFile("avatars", file))
                .isInstanceOf(BadRequestException.class);

        assertThat(Files.exists(tempDir.resolve("avatars"))).isFalse();
    }

    @Test
    void saveFile_nullContentType_throwsBadRequestException() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar", null, "bytes".getBytes());

        assertThatThrownBy(() -> fileService.saveFile("avatars", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void saveFile_emptyFile_throwsBadRequestException() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.saveFile("avatars", file))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteFile_existingFile_removesIt() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.jpg", "image/jpeg", "fake-jpg-bytes".getBytes());
        String relativePath = fileService.saveFile("avatars", file);

        fileService.deleteFile(relativePath);

        assertThat(Files.exists(tempDir.resolve(relativePath))).isFalse();
    }
}
