package org.readutf.buildformat;

import org.readutf.buildformat.settings.BuildSetting;

import java.time.LocalDateTime;
import java.util.Map;

public record BuildMeta(
        String name,
        LocalDateTime creationTimestamp,
        String checksum,
        int version,
        String format,
        Map<String, BuildSetting<?>> metadata
) {
}
