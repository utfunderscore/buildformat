package org.readutf.buildformat.settings;

import java.util.Map;

public record BuildMetadata(
        Map<String, BuildSetting<?>> requirements
) {
}
