package dev.ein.cloudnet.module.backup.command;

import dev.derklaro.aerogel.Inject;
import dev.derklaro.aerogel.Singleton;
import dev.ein.cloudnet.module.backup.backup.AdvancedBackupInfo;
import dev.ein.cloudnet.module.backup.backup.BackupInfo;
import dev.ein.cloudnet.module.backup.backup.BackupSystem;
import dev.ein.cloudnet.module.backup.backup.IBackupService;
import eu.cloudnetservice.common.column.ColumnFormatter;
import eu.cloudnetservice.common.column.RowedFormatter;
import eu.cloudnetservice.common.language.I18n;
import eu.cloudnetservice.driver.registry.injection.Service;
import eu.cloudnetservice.node.command.annotation.CommandAlias;
import eu.cloudnetservice.node.command.annotation.Description;
import eu.cloudnetservice.node.command.exception.ArgumentNotAvailableException;
import eu.cloudnetservice.node.command.source.CommandSource;
import lombok.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.parser.Parser;
import org.incendo.cloud.annotations.suggestion.Suggestions;
import org.incendo.cloud.context.CommandInput;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@Singleton
@CommandAlias({ "backups" })
@Permission("backups.command")
@Description("module-backup-command-description")
public class BackupCommand {
	private final BackupSystem backupSystem;

	@Inject
	public BackupCommand(@NonNull @Service BackupSystem backupSystem) {
		this.backupSystem = backupSystem;
	}

	private static final RowedFormatter<BackupInfo> ENTRY_LIST_FORMATTER = RowedFormatter.<BackupInfo>builder()
			.defaultFormatter(
					ColumnFormatter.builder().columnTitles(new String[] { "id", "cloudnet-version", "date" }).build())
			.column(BackupInfo::id).column(BackupInfo::cloudNetVersion)
			.column((info) -> new Date(info.date()).toString()).build();

	private static final RowedFormatter<AdvancedBackupInfo> ENTRY_INFO_FORMATTER = RowedFormatter
			.<AdvancedBackupInfo>builder()
			.defaultFormatter(ColumnFormatter.builder()
					.columnTitles(new String[] { "id", "cloudnet-version", "services", "date" }).build())
			.column(AdvancedBackupInfo::id).column(AdvancedBackupInfo::cloudNetVersion)
			.column((info) -> Arrays.toString(info.services().stream().map(IBackupService::getName).toArray()))
			.column((info) -> new Date(info.date()).toString()).build();

	@Parser(name = "serviceParser", suggestions = "serviceSuggestions")
	public IBackupService serviceParser(@NonNull CommandInput input) {
		String name = input.readString();

		for (IBackupService srv : backupSystem.getServices()) {

			if (srv.getName().equalsIgnoreCase(name))
				return srv;

		}

		throw new ArgumentNotAvailableException("The Backup-Service \"" + name + "\" was not found.");
	}

	@Suggestions("serviceSuggestions")
	public List<String> serviceSuggestions() {
		return backupSystem.getServices().stream().map(IBackupService::getName).toList();
	}

	@Command("backup|backups create|new [service]")
	public void createBackup(@NonNull CommandSource source,
							 @Argument(value = "service", parserName = "serviceParser") IBackupService service) {
		if (source.checkPermission("backups.create")) {
			if (service == null) {
				try {
					backupSystem.startBackup(source);
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backup failed. " + e);
				}
			} else {
				try {
					backupSystem.startBackup(source, service);
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backup failed. " + e);
				}
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@Command("backup|backups list|l [service]")
	public void listBackups(@NonNull CommandSource source,
							@Argument(value = "service", parserName = "serviceParser") IBackupService service) {
		if (source.checkPermission("backups.list")) {
			if (service == null) {
				var infos = listBackupServices();

				source.sendMessage(ENTRY_LIST_FORMATTER.format(infos.values()));
			} else {
				try {
					source.sendMessage(ENTRY_LIST_FORMATTER.format(service.listBackups()));
				} catch (IOException e) {
					e.printStackTrace();
					source.sendMessage("Backups list failed. " + e);
				}
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@Command("backup|backups info|i <id>")
	public void backupInfo(CommandSource source,
			@Argument(value = "id", parserName = "backupParser") AdvancedBackupInfo info) {
		if (source == null)
			throw new NullPointerException("source is marked non-null but is null");

		if (info == null)
			throw new NullPointerException("info is marked non-null but is null");

		if (source.checkPermission("backups.info")) {
			source.sendMessage(ENTRY_INFO_FORMATTER.format(Collections.singleton(info)));
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@Suggestions("backupSuggestions")
	public @NonNull Stream<String> backupSuggestions() {
		return listBackupServices().keySet().stream();
	}

	private Map<String, BackupInfo> listBackupServices() {
		Map<String, BackupInfo> infos = new HashMap<>();
		for (IBackupService srv : backupSystem.getServices()) {
			try {
				for (BackupInfo info : srv.listBackups()) {
					if (infos.containsKey(info.id()))
						continue;
					infos.put(info.id(), info);
				}
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
		return infos;
	}

	@Parser(name = "backupParser", suggestions = "backupSuggestions")
	public AdvancedBackupInfo backupParser(@NonNull CommandInput input) {
		String name = input.readString();

		List<IBackupService> services = new ArrayList<>();
		BackupInfo info = null;

		for (IBackupService srv : backupSystem.getServices()) {

			try {
				if (!srv.exists(name))
					continue;

				info = srv.get(name);
				services.add(srv);
			} catch (Throwable e) {
				e.printStackTrace();
			}

		}

		if (info == null)
			throw new ArgumentNotAvailableException("The Backup-Service \"" + name + "\" was not found.");

		return new AdvancedBackupInfo(info.id(), info.cloudNetVersion(), info.date(), services);
	}

	@Command("backup|backups restore|load <id> <service>")
	public void restoreBackup(
			@NonNull CommandSource source,
			@NonNull @Argument(value = "id", parserName = "backupParser") AdvancedBackupInfo info,
			@NonNull @Argument(value = "service", parserName = "serviceParser") IBackupService service) {

		if (source.checkPermission("backups.restore")) {
			try {
				backupSystem.restoreBackup(source, service, info.id());
			} catch (IOException e) {
				e.printStackTrace();
				source.sendMessage("Backup failed. " + e);
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

	@Command("backup|backups gc|garbagecollect")
	public void garbageCollect(
			@NonNull CommandSource source
	) {
		if (source.checkPermission("backups.gc")) {
			try {
				backupSystem.garbageCollect(source);
			} catch (IOException e) {
				e.printStackTrace();
				source.sendMessage("Backup failed. " + e);
			}
		} else {
			source.sendMessage(I18n.trans("missing-command-permission"));
		}
	}

}
