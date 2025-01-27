package dev.ein.cloudnet.module.backup.sql;

import dev.derklaro.aerogel.Inject;
import dev.ein.cloudnet.module.backup.backup.AdvancedBackupInfo;
import dev.ein.cloudnet.module.backup.backup.Backup;
import dev.ein.cloudnet.module.backup.backup.BackupInfo;
import dev.ein.cloudnet.module.backup.backup.IBackupService;
import dev.ein.cloudnet.module.backup.data.BlobStorage;
import dev.ein.cloudnet.module.backup.data.FileFileMeta;
import dev.ein.cloudnet.module.backup.data.FileMeta;
import eu.cloudnetservice.driver.database.Database;
import eu.cloudnetservice.driver.document.Document;
import eu.cloudnetservice.node.command.source.CommandSource;
import eu.cloudnetservice.node.database.NodeDatabaseProvider;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SQLBackupService implements IBackupService {

	private final @NonNull NodeDatabaseProvider databaseProvider;
	private static final String DB_NAME = "cloudnet_backups";
	private Database backupsDb;

	private static final int SPLIT_SIZE = 8 * 1048576;

	@Inject
	public SQLBackupService(@NonNull NodeDatabaseProvider databaseProvider) {
		this.databaseProvider = databaseProvider;
	}

	public void initialize() throws IOException {
		backupsDb = databaseProvider.database(DB_NAME);
	}

	@Override
	public void start(CommandSource source, Backup backup, BackupInfo info, BlobStorage blobStorage) throws IOException {

		String updateId = info.id();

		String prefix = "[SQL-Backup-Service] ";

		Context context = new Context(source, prefix, blobStorage, false);
		context.sendMessage("Starting Backup with ID: " + info.id() + "...");
		Document.Mutable meta = Document.newJsonDocument();
		meta.append("version", info.cloudNetVersion());
		meta.append("time", info.date());

		{
			context.sendMessage("Storing Templates...");
			Document templatesDoc = context.addCompressedFiles(backup.templates());
			meta.append("templates", templatesDoc);
			context.sendMessage("Added Templates done.");
		}
		{
			context.sendMessage("Storing Regions...");
			Document regionsDoc = context.addUncompressedFiles(backup.regions());
			meta.append("regions", regionsDoc);
			context.sendMessage("Added Regions done.");
		}
		{
			context.sendMessage("Storing Worlds...");
			Document worldsDoc = context.addCompressedFiles(backup.worlds());
			meta.append("worlds", worldsDoc);
			context.sendMessage("Added Worlds done.");
		}
		{
			context.sendMessage("Storing Playerdata...");
			Document playerdataDoc = context.addUncompressedFiles(backup.playerdata());
			meta.append("playerdata", playerdataDoc);
			context.sendMessage("Added Playerdata done.");
		}
		{
			context.sendMessage("Storing Extra-Files...");
			Document extraFilesDoc = context.addCompressedFiles(backup.extra_files());
			meta.append("extra_files", extraFilesDoc);
			context.sendMessage("Added Extra-Files done.");
		}
		context.sendMessage("Storing meta...");
		backupsDb.insert(updateId, meta);
		context.sendMessage("Backup with ID: " + info.id() + " finished.");
	}

	@Override
	public Backup restore(CommandSource source, File tmp, String updateId, BlobStorage blobStorage) throws IOException {

		if (!exists(updateId))
			throw new IOException("Update with Id \"" + updateId + "\" does not exists.");

		String prefix = "[MySQL-Backup-Service] ";
		source.sendMessage(prefix + "Restoring Backup with ID: " + updateId + "...");
		Document data = backupsDb.get(updateId);
		if(data == null) throw new IOException("Update with Id \"" + updateId + "\" does not exists.");

		Context ctx = new Context(source, prefix, blobStorage, false);

		ctx.sendMessage("Fetching Templates...");
		Map<String, File> templates = ctx.fetchCompressed(data.readDocument("templates"));

		ctx.sendMessage("Fetching Regions...");
		Map<String, List<File>> regions = ctx.fetchUncompressed(data.readDocument("regions"));

		ctx.sendMessage("Fetching Worlds...");
		Map<String, File> worlds = ctx.fetchCompressed(data.readDocument("worlds"));

		ctx.sendMessage("Fetching Playerdata...");
		Map<String, List<File>> playerdata = ctx.fetchUncompressed(data.readDocument("playerdata"));

		ctx.sendMessage("Fetching Extra-Files...");
		Map<String, File> extraFiles = ctx.fetchCompressed(data.readDocument("extra_files"));

		return new Backup(templates, regions, worlds, playerdata, extraFiles);
	}

	private Set<String> getFileIds(Document... docs) {
		return Arrays.stream(docs).flatMap(this::flattenDocumentValues).collect(Collectors.toSet());
	}

	private Stream<String> flattenDocumentValues(Document doc) {
		return doc.keys().stream().flatMap(key -> {
			if (doc.getString(key) != null) {
				return Stream.of(doc.getString(key));
			} else {
				return flattenDocumentValues(doc.readDocument(key));
			}
		});
	}

	private Document fetchDb(Database database, String updateId, String humanReadableName) {
		Document doc = database.get(updateId);
		if (doc == null) {
			throw new RuntimeException(String.format("%s for update-id %s not found!", humanReadableName, updateId));
		}
		return doc;
	}

	@Override
	public List<BackupInfo> listBackups() throws IOException {
		return backupsDb.keys().parallelStream()
				.map(this::get)
				.toList();
	}

	@Override
	public long calculateRestoreMemory(String updateId) {
		return 536870912L;
	}

	@Override
	public boolean exists(String updateId) throws IOException {
		return backupsDb.contains(updateId);
	}

	@Override
	public String getName() {
		return "sql";
	}

	@Override
	public BackupInfo get(String updateId) {
		Document doc = backupsDb.get(updateId);
		assert doc != null;
		return new BackupInfo(updateId, doc.getString("version"), doc.getLong("time"));
	}

	@Override
	public Stream<byte[]> getBlobsToKeep() {
		var entries = backupsDb.entries();
		Context ctx = new Context(null, "", null, true);
		return entries.values().stream().flatMap(doc -> Stream.of(
                ctx.fetchHashesCompressed(doc.readDocument("templates")),
                ctx.fetchHashesUncompressed(doc.readDocument("regions")),
                ctx.fetchHashesCompressed(doc.readDocument("worlds")),
                ctx.fetchHashesUncompressed(doc.readDocument("playerdata")),
                ctx.fetchHashesCompressed(doc.readDocument("extra_files"))
        ).flatMap(s -> s));
	}

	record Context(CommandSource commandSource, String prefix, BlobStorage blobStorage, boolean metaOnly) {
		public void sendMessage(String message) {
			commandSource.sendMessage(prefix + message);
		}

		public Document addFile(File value) throws IOException {
			FileMeta meta = FileFileMeta.fromFile(value);
			blobStorage.store(meta.getHashString(), value);
			return Document.newJsonDocument()
					.append("hash", meta.getHash())
					.append("size", meta.getSize())
					.append("lastModified", meta.getLastModified())
					.append("path", meta.getPath());
		}

		public byte[] fetchHash(Document doc) {
			return doc.readObject("hash", byte[].class);
		}

		public void fetchFile(Document doc, File target) throws IOException {
			Path path = doc.readObject("path", Path.class);
			String hash = HexFormat.of().formatHex(fetchHash(doc));

			if(!metaOnly) blobStorage.fetch(hash, target);
		}

		public Document addCompressedFiles(Map<String, File> files) throws IOException {
			Document.Mutable filesDoc = Document.newJsonDocument();
			for (Map.Entry<String, File> entry : files.entrySet()) {
				filesDoc.append(entry.getKey(), addFile(entry.getValue()));
			}
			return filesDoc;
		}

		public Document addUncompressedFiles(Map<String, List<File>> files) throws IOException {
			Document.Mutable doc = Document.newJsonDocument();
			for (Map.Entry<String, List<File>> entry : files.entrySet()) {
				Document.Mutable folderDoc = Document.newJsonDocument();
				for (File file : entry.getValue()) {
					folderDoc.append(file.getName(), addFile(file));
				}
				doc.append(entry.getKey(), folderDoc);
			}
			return doc;
		}

		public @NotNull Map<@NonNull String, @NotNull File> fetchCompressed(Document doc) {
			return doc.keys().parallelStream().map(key -> {
                try {
					File f = File.createTempFile("cloudnet_backup_", ".compressed");
                    fetchFile(doc.readDocument(key), f);
					return new AbstractMap.SimpleEntry<>(key, f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
			}).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
		}

		public @NonNull Map<@NonNull String, @NonNull List<File>> fetchUncompressed(Document doc) {
			return doc.keys().parallelStream().map(outerKey ->  {
				Document outerDoc = doc.readDocument(outerKey);
				List<File> files = outerDoc.keys().parallelStream().map(innerKey -> {
					try {
						File f = File.createTempFile("cloudnet_backup_", ".bin");
						fetchFile(doc.readDocument(innerKey), f);
						return f;
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}).toList();
				return new AbstractMap.SimpleEntry<>(outerKey, files);
			}).collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
		}

		public @NotNull Stream<byte[]> fetchHashesCompressed(Document doc) {
			return doc.keys().parallelStream()
					.map(key -> fetchHash(doc.readDocument(key)));
		}

		public @NonNull Stream<byte[]> fetchHashesUncompressed(Document doc) {
			return doc.keys().parallelStream()
					.flatMap(key -> {
						Document outerDoc = doc.readDocument(key);
						return fetchHashesCompressed(outerDoc);
					});
		}
	}
}
