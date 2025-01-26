package dev.ein.cloudnet.module.backup.data;


import java.nio.file.Path;
import java.util.HexFormat;

/**
 * Represents metadata information about a file. This can be a file on disk or in a datastore.
 */
public interface FileMeta {
    long getLastModified();
    byte[] getHash();
    default String getHashString() {
        return HexFormat.of().formatHex(getHash());
    }
    long getSize();
    Path getPath();
}
