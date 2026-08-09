package org.example.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    String saveFile(String directory, MultipartFile file);

    void deleteFile(String filePath);
}
