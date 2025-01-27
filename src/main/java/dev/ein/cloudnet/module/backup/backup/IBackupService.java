package dev.ein.cloudnet.module.backup.backup;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import dev.ein.cloudnet.module.backup.data.BlobStorage;
import eu.cloudnetservice.node.command.source.CommandSource;
import lombok.NonNull;

public interface IBackupService {

	public String getName();

	public void initialize() throws IOException;

	public void start(CommandSource source, Backup backup, BackupInfo info, BlobStorage blobStorage) throws IOException;

	public Backup restore(CommandSource source, File tmp, String updateId, BlobStorage blobStorage) throws IOException;

	public boolean exists(String updateId) throws IOException;

	public List<BackupInfo> listBackups() throws IOException;

	public long calculateRestoreMemory(String updateId) throws IOException;

	public BackupInfo get(String name) throws IOException;

	public Stream<byte[]> getBlobsToKeep();

    void deleteBackup(@NonNull AdvancedBackupInfo info);
}
