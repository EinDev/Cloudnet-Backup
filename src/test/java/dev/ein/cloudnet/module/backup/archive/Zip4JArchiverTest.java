package dev.ein.cloudnet.module.backup.archive;

import dev.ein.cloudnet.module.backup.config.ArchiverConfig;
import dev.ein.cloudnet.module.backup.data.ChecksumUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Zip4JArchiverTest {
    @ParameterizedTest
    @ValueSource(strings = { "server1", "server2" })
    void testIdempotence(String sampleServiceName, @TempDir Path tmpFolder) throws IOException {
        Zip4jArchiever archiver = new Zip4jArchiever(new ArchiverConfig(false, null, ArchiverConfig.Type.ZIP));
        File targetFiles = new File(this.getClass().getClassLoader().getResource("samples/services/" + sampleServiceName).getFile());
        File target = archiver.compress(tmpFolder.toFile(), targetFiles);
        target.deleteOnExit();
        File target2 = archiver.compress(tmpFolder.toFile(), targetFiles);
        target2.deleteOnExit();
        Assertions.assertArrayEquals(
                ChecksumUtil.getSha265Checksum(target),
                ChecksumUtil.getSha265Checksum(target2),
                "Checksums of same archive differ");
    }
}
