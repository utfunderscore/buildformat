package org.readutf.buildformat;

import java.time.LocalDateTime;

public record BuildMeta(
        String name,
        LocalDateTime creationTimestamp,
        String checksum,
        int version,
        String format
) {
}
