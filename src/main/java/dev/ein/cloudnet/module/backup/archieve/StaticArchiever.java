package dev.ein.cloudnet.module.backup.archieve;

import java.io.File;
import java.io.IOException;

public class StaticArchiever {

	public static File compress(File tmpFolder, File... content) throws IOException {
		return AbstractArchiever.getInstance().compress(tmpFolder, content);
	}

	public static File compress_include(File tmpFolder, File[] files, String... include) throws IOException {
		return AbstractArchiever.getInstance().compress_include(tmpFolder, files, include);
	}

	public static File compress_exclude(File tmpFolder, File[] files, String... exclude) throws IOException {
		return AbstractArchiever.getInstance().compress_exclude(tmpFolder, files, exclude);
	}

	public static void decompresseTo(File zip, File targetFolder) throws IOException {
		AbstractArchiever.getInstance().decompresseTo(zip, targetFolder);
	}

}
