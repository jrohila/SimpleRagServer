package io.github.jrohila.simpleragserver.service;

import io.github.jrohila.simpleragserver.domain.DocumentEntity;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Singleton
public class FileStorageService {

    private final Path rootLocation;

    public FileStorageService(@Property(name = "storage.filesystem.root", defaultValue = "./upload") String filesystemRoot) {
        this.rootLocation = Paths.get(filesystemRoot).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location: " + filesystemRoot, e);
        }
    }

    public void setContent(DocumentEntity entity, InputStream content) {
        try {
            if (content == null) {
                throw new IllegalArgumentException("Content cannot be null");
            }
            
            String contentId = entity.getContentId();
            if (contentId == null) {
                throw new IllegalArgumentException("ContentId cannot be null");
            }

            Path destinationFile = this.rootLocation.resolve(contentId).normalize().toAbsolutePath();
            
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new SecurityException("Cannot store file outside storage directory");
            }

            Files.copy(content, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public InputStream getContent(DocumentEntity entity) {
        try {
            String contentId = entity.getContentId();
            if (contentId == null) {
                return null;
            }

            Path file = rootLocation.resolve(contentId).normalize();
            
            if (!Files.exists(file)) {
                return null;
            }

            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file", e);
        }
    }

    public void unsetContent(DocumentEntity entity) {
        try {
            String contentId = entity.getContentId();
            if (contentId != null) {
                Path file = rootLocation.resolve(contentId).normalize();
                Files.deleteIfExists(file);
                entity.setContentId(null);
                entity.setContentLen(null);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }
}
