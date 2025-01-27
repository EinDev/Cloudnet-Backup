package dev.ein.cloudnet.module.backup.archive;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import dev.ein.cloudnet.module.backup.config.ArchiverConfig;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zip4jArchiever extends Archiver {
    private final ArchiverConfig config;
	private static final Logger LOGGER = LoggerFactory.getLogger(Zip4jArchiever.class);

    public Zip4jArchiever(ArchiverConfig config) {
        this.config = config;
    }

	@Override
	public File compress(File tmpFolder, File... content) throws IOException {
		final ZipParameters zipParameters = new ZipParameters();
		zipParameters.setCompressionMethod(CompressionMethod.DEFLATE);
		zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);
		char[] password = null;
		if(config.encrypt()) {
			zipParameters.setEncryptFiles(true);
			if(config.password() != null) {
				password = config.password().toCharArray();
			}
			zipParameters.setEncryptionMethod(EncryptionMethod.AES);
			zipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
		}

		File f = new File(tmpFolder, UUID.randomUUID() + ".zip");

		try (ZipFile zf = new ZipFile(f, password)) {

			for (File c : content) {
				try {
					if (c.isFile()) {
						zf.addFile(c, zipParameters);
					}
					if (c.isDirectory()) {
						zf.addFolder(c, zipParameters);
					}
				} catch (ZipException e) {
                    LOGGER.error("Error zipping file: {}", c.getPath(), e);
				}
			}

		}

		return f;
	}

	@Override
	public void decompresseTo(File zip, File targetFolder) throws IOException {

		try (ZipFile zf = new ZipFile(zip, password.toCharArray())) {
			zf.extractAll(targetFolder.getAbsolutePath());
		}

	}

}
