package org.readutf.buildformat.common;

import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.schematic.BuildSchematic;

public record Build(
    BuildMeta buildMeta,
    BuildSchematic buildSchematic
) {

}
