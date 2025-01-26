package dev.ein.cloudnet.module.backup.data;

import org.apache.commons.io.FileUtils;

public record GarbageCollectionResult(int blobsDeleted, long freedBytes) {
    public GarbageCollectionResult {
        if (blobsDeleted < 0) {
            throw new IllegalArgumentException("blobsDeleted must be >= 0");
        }
        if (freedBytes < 0) {
            throw new IllegalArgumentException("freedBytes must be >= 0");
        }
    }

    public String bytesAsString() {
        return FileUtils.byteCountToDisplaySize(freedBytes);
    }
}
