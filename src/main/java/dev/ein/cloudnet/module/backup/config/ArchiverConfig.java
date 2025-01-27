package dev.ein.cloudnet.module.backup.config;

public record ArchiverConfig(
        boolean encrypt,
        String password,
        Type type
) {

    public enum Type {
        ZIP
    }
}
