package org.readutf.buildformat;

import org.readutf.buildformat.settings.BuildMetadata;

import java.time.LocalDateTime;

public record BuildMeta(
        String name,
        LocalDateTime creationTimestamp,
        int checksum,
        int version,
        String format,
        BuildMetadata metadata
) {
}
