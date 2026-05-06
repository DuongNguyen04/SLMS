package com.example.slms.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.slms.exception.BusinessException;
import com.example.slms.exception.ValidationException;

@Service
public class ReportStorageService {

    private final Path basePath;

    public ReportStorageService(@Value("${slms.report.storage-path:storage/reports}") String storagePath) {
        this.basePath = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
        } catch (IOException ex) {
            throw new BusinessException("Unable to initialize report storage", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path resolveFile(String fileName) {
        validateFileName(fileName);
        return basePath.resolve(fileName).normalize();
    }

    public Resource loadAsResource(String fileName) {
        try {
            Path filePath = resolveFile(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new BusinessException("Report file not found", HttpStatus.NOT_FOUND);
            }
            return resource;
        } catch (MalformedURLException ex) {
            throw new BusinessException("Report file not found", HttpStatus.NOT_FOUND);
        }
    }

    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new ValidationException("fileName is required");
        }

        String trimmed = fileName.trim();
        if (!trimmed.matches("[A-Za-z0-9_.-]+")) {
            throw new ValidationException("Invalid report file name");
        }
    }
}
