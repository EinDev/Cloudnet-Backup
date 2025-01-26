package dev.ein.cloudnet.module.backup;

import dev.ein.cloudnet.module.backup.archieve.AbstractArchiever;
import dev.ein.cloudnet.module.backup.archieve.Zip4jArchiever;
import dev.ein.cloudnet.module.backup.backup.BackupSystem;
import dev.ein.cloudnet.module.backup.backup.IBackupService;
import dev.ein.cloudnet.module.backup.command.BackupCommand;
import dev.ein.cloudnet.module.backup.config.BackupStorageConfig;
import dev.ein.cloudnet.module.backup.sql.SQLBackupService;
import eu.cloudnetservice.driver.document.DocumentFactory;
import eu.cloudnetservice.driver.module.ModuleLifeCycle;
import eu.cloudnetservice.driver.module.ModuleTask;
import eu.cloudnetservice.driver.module.driver.DriverModule;
import eu.cloudnetservice.driver.registry.ServiceRegistry;
import eu.cloudnetservice.node.command.CommandProvider;
import eu.cloudnetservice.node.config.Configuration;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloudNetBackupModule extends DriverModule {

	@Getter
	private volatile BackupStorageConfig cfg;

	private final List<IBackupService> services = new ArrayList<>();

	@ModuleTask(order = 64, lifecycle = ModuleLifeCycle.LOADED)
	public void initConfig() {
		this.cfg = this.readConfig(BackupStorageConfig.class, BackupStorageConfig::getDefault, DocumentFactory.json());
	}

	@ModuleTask(order = 32, lifecycle = ModuleLifeCycle.STARTED)
	public void finishStartup(
			@NonNull Configuration configuration,
			@NonNull ServiceRegistry registry,
			@NonNull SQLBackupService sqlBackupService
	) {
		if(cfg.enabled()) {

			if (!cfg.enabled())
				return;

			AbstractArchiever.setPassword(configuration.clusterConfig().clusterId().toString());
			AbstractArchiever.setInstance(new Zip4jArchiever());

			try {
				sqlBackupService.initialize();
				services.add(sqlBackupService);
			} catch (IOException e) {
				e.printStackTrace();
			}


			BackupSystem system = new BackupSystem(new File("."), services);
			registry.registerProvider(BackupSystem.class, "BackupSystem", system);

			if (cfg.backupOnStartup()) {
				try {
					system.startBackup(null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@ModuleTask(order = 16, lifecycle = ModuleLifeCycle.STARTED)
	public void registerCommand(
			@NonNull CommandProvider commandProvider,
			@NonNull BackupCommand command
	) {
		commandProvider.register(command);
	}

	@ModuleTask(order = 32, lifecycle = ModuleLifeCycle.UNLOADED)
	public void finishShutdown(
			@NonNull ServiceRegistry registry
	) {
		registry.unregisterProvider(BackupSystem.class, "BackupSystem");
	}

	@ModuleTask(lifecycle = ModuleLifeCycle.RELOADING)
	public void reload() {
		initConfig();
	}

}
