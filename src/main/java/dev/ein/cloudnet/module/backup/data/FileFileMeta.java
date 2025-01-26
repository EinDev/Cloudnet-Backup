package dev.ein.cloudnet.module.backup.data;

import lombok.Getter;

import java.io.File;
import java.nio.file.Path;


public class FileFileMeta implements FileMeta {
    @Getter
    private final File file;
    @Getter
    private final long lastModified;
    @Getter
    private final long size;

    private byte[] hash;

    public FileFileMeta(File file, long lastModified, long size) {
        this.file = file;
        this.lastModified = lastModified;
        this.size = size;

    }

    @Override
    public byte[] getHash() {
        if(hash == null) {
            hash = ChecksumUtil.getSha265Checksum(getPath().toFile());
        }
        return hash;
    }

    @Override
    public Path getPath() {
        return file.toPath();
    }

    public static FileFileMeta fromFile(File file) {
        return new FileFileMeta(file, file.lastModified(), file.length());
    }
}
