package dev.ein.cloudnet.module.backup.data;

import lombok.experimental.UtilityClass;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@UtilityClass
public class ChecksumUtil {
    public byte[] getSha265Checksum(File file) {
        byte[] buffer = new byte[8192];
        int count;
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            while((count = bis.read(buffer)) > 0) {
                digest.update(buffer, 0, count);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return digest.digest();
    }

    public String getSha265ChecksumString(File file) {
        return getAsHexString(getSha265Checksum(file));
    }

    public String getAsHexString(byte[] checksum) {
        return HexFormat.of().formatHex(checksum);
    }
}
