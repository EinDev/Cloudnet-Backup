package dev.ein.cloudnet.module.backup.data;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public interface BlobStorage {
    /**
     * Stores the given file *content* in the BlobStorage, associating it with the specified hash.
     * The metadata should be stored somewhere else.
     *
     * @param hash The unique hash value used to identify the file in the BlobStorage.
     * @param data The file to be stored in the BlobStorage.
     * @return {@code true} if the file is successfully stored, {@code false} if the file was skipped because it is
     *         already in the pool.
     * @throws IOException If an I/O error occurs during the storage process, such as
     *                     failure to create directories or file collisions.
     */
    boolean store(String hash, File data) throws IOException;

    /**
     * Fetches the content of a file from the BlobStorage and writes it to the specified target file.
     *
     * @param hash   The hash of the file to be fetched
     * @param target The target path where the fetched content will be stored.
     */
    void fetch(String hash, File target) throws IOException;

    GarbageCollectionResult garbageCollect(Set<String> hashesToKeep) throws IOException;
}
