package dev.ein.cloudnet.module.backup.data;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FolderBlobStorage implements BlobStorage {
    private final Path folder;

    public FolderBlobStorage(Path folder) {
        this.folder = folder;
    }

    @Override
    public boolean store(String hash, File data) throws IOException {
        String group = hash.substring(0, 4);
        Path groupFolder = folder.resolve(group);
        groupFolder.toFile().mkdirs();
        Path targetFile = groupFolder.resolve(hash);
        if(targetFile.toFile().exists()) return false;
        FileUtils.copyFile(data, targetFile.toFile());
        return true;
    }

    @Override
    public void fetch(String hash, File target) throws IOException {
        String group = hash.substring(0, 4);
        Path groupFolder = folder.resolve(group);
        Path sourceFile = groupFolder.resolve(hash);
        FileUtils.copyFile(sourceFile.toFile(), target);
    }

    @Override
    public GarbageCollectionResult garbageCollect(Set<String> hashesToKeep) throws IOException {
        AtomicInteger filesDeleted = new AtomicInteger(0);
        AtomicLong bytesFreed = new AtomicLong(0);

        try (var stream = Files.walk(folder)) {
            stream.filter(Files::isRegularFile)
                .forEach(file -> {
                    if (!hashesToKeep.contains(file.getFileName().toString())) {
                        long size = file.toFile().length();
                        boolean success = file.toFile().delete();
                        if(success) {
                            bytesFreed.addAndGet(size);
                            filesDeleted.incrementAndGet();
                        }
                    }
                });
        }
        return new GarbageCollectionResult(filesDeleted.get(), bytesFreed.get());
    }
}
