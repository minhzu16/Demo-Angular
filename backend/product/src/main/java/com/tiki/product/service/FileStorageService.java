package com.tiki.product.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;


@Service
public class FileStorageService {
    private final Path root;

    public FileStorageService(@Value("${file.upload-dir:/tmp/uploads}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IOException("Cannot create upload directory at " + this.root, e);
        }
    }

    public String save(MultipartFile file) throws IOException {
        String ext = extractExtension(file.getOriginalFilename()).toLowerCase();
        
        // ✅ BUG 49 FIX: Prevent dangerous file uploads (only allow images)
        if (!ext.matches("^(jpg|jpeg|png|gif|webp)$")) {
            throw new IllegalArgumentException("Invalid file format. Only images (JPG, PNG, GIF, WEBP) are allowed.");
        }
        
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
