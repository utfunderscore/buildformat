package org.readutf.buildformat.common.meta;

import java.util.List;
import org.readutf.buildformat.common.format.BuildFormatChecksum;

public record BuildMeta(
        String name,
        String description,
        int version,
        List<String> tags,
        List<BuildFormatChecksum> formats
) {



}
