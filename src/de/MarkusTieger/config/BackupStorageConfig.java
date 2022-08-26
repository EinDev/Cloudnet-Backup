package de.MarkusTieger.config;

public record BackupStorageConfig(boolean enabled, boolean backupOnStartup,
		MySQLConnectionConfig mysql, String secure_temp_folder) {

}
