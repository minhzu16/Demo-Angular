package com.tiki.product.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
public class FileStorageService {
    private final Path root = Paths.get("uploads");

    public FileStorageService() throws IOException {
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
    }

    public String save(MultipartFile file) throws IOException {
        String ext = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + (ext.isEmpty() ? "" : ("." + ext));
        Path target = root.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }

    private String extractExtension(String name) {
        if (name == null) return "";
        int idx = name.lastIndexOf('.');
        return idx > 0 ? name.substring(idx + 1) : "";
    }
}
