package dev.ein.cloudnet.module.backup.config;

import lombok.NonNull;

public record ArchieverConfig(
        @NonNull String archieverClassName
) {

}
