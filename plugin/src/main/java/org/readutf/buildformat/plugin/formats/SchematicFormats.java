package org.readutf.buildformat.plugin.formats;

import org.readutf.buildformat.plugin.formats.impl.PolarFormat;
import org.readutf.buildformat.plugin.formats.impl.SpongeV3Format;

public enum SchematicFormats {
    SPONGE_V3(new SpongeV3Format()),
    POLAR(new PolarFormat());

    private final SchematicFormat format;

    SchematicFormats(SchematicFormat format) {
        this.format = format;
    }

    public SchematicFormat getFormat() {
        return format;
    }
}
