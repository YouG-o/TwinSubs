package com.substack.infrastructure.io;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Scans directories recursively and collects supported media files (.mkv, .mp4).
 */
public final class FileScanner {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".mkv", ".mp4");

    /**
     * Scans a list of input paths (files or directories) recursively.
     *
     * @param inputPaths List of files or directories dropped by the user.
     * @return Flattened list of discovered supported media file paths.
     * @throws IOException If an I/O error occurs during directory traversal.
     */
    public List<Path> scanPaths(List<Path> inputPaths) throws IOException {
        Objects.requireNonNull(inputPaths, "Input paths list cannot be null");
        List<Path> discoveredFiles = new ArrayList<>();

        for (Path inputPath : inputPaths) {
            if (!Files.exists(inputPath)) {
                continue;
            }

            if (Files.isDirectory(inputPath)) {
                scanDirectoryRecursively(inputPath, discoveredFiles);
            } else if (isSupportedMediaFile(inputPath)) {
                discoveredFiles.add(inputPath.toAbsolutePath().normalize());
            }
        }

        return discoveredFiles;
    }

    private void scanDirectoryRecursively(Path rootDir, List<Path> resultList) throws IOException {
        Files.walkFileTree(rootDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (isSupportedMediaFile(file)) {
                    resultList.add(file.toAbsolutePath().normalize());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                // Ignore inaccessible files/directories gracefully
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private boolean isSupportedMediaFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return SUPPORTED_EXTENSIONS.stream().anyMatch(fileName::endsWith);
    }
}