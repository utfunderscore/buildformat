package org.readutf.buildformat.plugin.formats.impl;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer;
import org.readutf.buildformat.plugin.formats.SchematicFormat;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class SpongeV3Format implements SchematicFormat {

    @Override
    public byte[] convert(Clipboard clipboard) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream);
        SpongeSchematicV3Writer writer = new SpongeSchematicV3Writer(new DataOutputStream(gzipStream));

        writer.write(clipboard);
        gzipStream.close();

        return outputStream.toByteArray();
    }
}
