package io.github.pawitp.ftpmediaserver.media;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Media Source from the local file system
 */
public class FileMediaSource implements MediaSource {

    private final Path rootDirectory = Path.of("").toRealPath();
    private Path currentDirectory = rootDirectory;

    public FileMediaSource() throws IOException {
    }

    @Override
    public void cwd(String name) throws IOException {
        currentDirectory = safeResolve(name);
    }

    @Override
    public List<Media> list(String name) throws IOException {
        Path path = safeResolve(name);
        return Files.list(path)
                .map((child) -> new Media(
                        Files.isRegularFile(child) ? "file" : "dir",
                        child.getFileName().toString()))
                .collect(Collectors.toList());
    }

    @Override
    public long size(String name) throws IOException {
        Path path = safeResolve(name);
        if (!Files.isRegularFile(path)) {
            throw new IOException("Is not regular file");
        } else {
            return Files.size(path);
        }
    }

    @Override
    public void retrieve(String name, long restartOffset, WritableByteChannel target) throws IOException {
        Path path = safeResolve(name);
        try (FileChannel fc = FileChannel.open(path)) {
            long expectedCount = fc.size() - restartOffset;
            long actualCount = fc.transferTo(restartOffset, expectedCount, target);
            if (expectedCount != actualCount) {
                throw new IOException("Incomplete write");
            }
        }
    }

    private Path safeResolve(String name) throws IOException {
        Path newPath = currentDirectory.resolve(name).toRealPath();
        if (!newPath.startsWith(rootDirectory)) {
            throw new IOException("Path outside of root directory");
        }

        return newPath;
    }
}
