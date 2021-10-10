package io.github.pawitp.ftpmediaserver.media;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * Abstraction for file operations
 */
public interface MediaSource {

    /**
     * Change current directory
     */
    void cwd(String name) throws IOException;

    /**
     * List files and sub-directories in the specified directory
     */
    List<Media> list(String name) throws IOException;

    /**
     * Get the size of a media file
     */
    long size(String name) throws IOException;

    /**
     * Get a media file
     */
    void retrieve(String name, long restartOffset, WritableByteChannel target) throws IOException;

}
