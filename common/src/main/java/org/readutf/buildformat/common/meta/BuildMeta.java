package org.readutf.buildformat.common.meta;

import java.util.List;
import org.readutf.buildformat.common.format.BuildFormatData;

public record BuildMeta(
        String name,
        String description,
        List<String> tags,
        List<BuildFormatData> formats
) {
}
