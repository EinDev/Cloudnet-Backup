package dev.ein.cloudnet.module.backup.archive;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import dev.ein.cloudnet.module.backup.config.ArchiverConfig;
import lombok.Getter;
import lombok.Setter;

public abstract class Archiver {

	@Setter
	protected static String password;

	@Getter
	@Setter
	private static Archiver instance;
	
	public static void init(ArchiverConfig config, String defaultPassword) {
        if (config instanceof ArchiverConfig archiverConfig) {
            instance = new Zip4jArchiever(archiverConfig);
			if(archiverConfig.password() == null) {
				password = defaultPassword;
			}
        } else {
            throw new IllegalStateException("Unexpected archiver: " + config.getClass());
        }
	}

	public abstract File compress(File tmpFolder, File... content) throws IOException;

	public File compress_include(File tmpFolder, File[] files, String... include) throws IOException {
		return compress(tmpFolder, Arrays.stream(files).filter(
				(f) -> Arrays.stream(include).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public File compress_exclude(File tmpFolder, File[] files, String... exclude) throws IOException {
		return compress(tmpFolder, Arrays.stream(files).filter(
				(f) -> !Arrays.stream(exclude).map(String::toLowerCase).toList().contains(f.getName().toLowerCase()))
				.toList().toArray(new File[0]));
	}

	public abstract void decompresseTo(File zip, File targetFolder) throws IOException;
}
