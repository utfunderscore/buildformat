package org.readutf.buildformat.common;

import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.schematic.BuildData;

public record Build(
    BuildMeta buildMeta,
    BuildData buildData
) {

}
