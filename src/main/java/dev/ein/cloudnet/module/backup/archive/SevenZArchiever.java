package dev.ein.cloudnet.module.backup.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZMethod;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;

public class SevenZArchiever extends Archiver {

	@Override
	public File compress(File tmpFolder, File... content) throws IOException {
		File f = new File(tmpFolder, UUID.randomUUID().toString());
		if (!f.exists()) {
			boolean success = f.createNewFile();
			if(!success) throw new IOException("Could not create file " + f.getAbsolutePath());
		}

		try (SevenZOutputFile file = new SevenZOutputFile(f)) {
			file.setContentCompression(SevenZMethod.LZMA2);
			
			for(File c : content) fill("", file, c);
			
			file.finish();
		}

		return f;
	}

	private void fill(String prefix, SevenZOutputFile file, File c) throws IOException {
		if(c.isDirectory()) {
			file.putArchiveEntry(file.createArchiveEntry(c, prefix + c.getName() + "/"));
			file.closeArchiveEntry();
			for(File f : Objects.requireNonNull(c.listFiles())) {
				fill(prefix + c.getName() + "/", file, f);
			}
		}
		if(c.isFile()) {
			SevenZArchiveEntry entry = file.createArchiveEntry(c, prefix + c.getName());
			file.putArchiveEntry(entry);
			try (FileInputStream fis = new FileInputStream(c)) {
				int len;
				byte[] buffer = new byte[1024];
				while((len = fis.read(buffer)) > 0) {
					file.write(buffer, 0, len);
				}
			}
			file.closeArchiveEntry();
		}
	}

	@Override
	public void decompresseTo(File zip, File targetFolder) throws IOException {
		try (SevenZFile file = SevenZFile.builder().setFile(zip).setPassword(password.toCharArray()).get()) {

			if (!targetFolder.exists()) {
				boolean success = targetFolder.mkdirs();
				if(!success) throw new IOException("Could not create directory " + targetFolder.getAbsolutePath());
			}

			int len;
			byte[] buffer = new byte[1024];

			SevenZArchiveEntry ze = null;
			while ((ze = file.getNextEntry()) != null) {

				File t = new File(targetFolder, ze.getName());

				if (ze.isDirectory()) {
					if (!t.exists()) {
						boolean success = t.mkdirs();
						if(!success) throw new IOException("Could not create directory " + t.getAbsolutePath());
					}
				} else {
					if (!t.exists()) {
						boolean success = t.createNewFile();
						if(!success) throw new IOException("Could not create file " + t.getAbsolutePath());
					}

					try (FileOutputStream fos = new FileOutputStream(t)) {
						while ((len = file.read(buffer)) > 0) {
							fos.write(buffer, 0, len);
							fos.flush();
						}
					}
				}

			}
		}
	}

}
