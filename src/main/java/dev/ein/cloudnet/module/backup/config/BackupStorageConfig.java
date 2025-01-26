package dev.ein.cloudnet.module.backup.config;

import lombok.NonNull;

public record BackupStorageConfig(
		boolean enabled,
		boolean backupOnStartup,
		@NonNull String secure_temp_folder
) {
	public static BackupStorageConfig getDefault() {
		return new BackupStorageConfig(
				false,
				true,
				".tmp"
		);
	}
}
