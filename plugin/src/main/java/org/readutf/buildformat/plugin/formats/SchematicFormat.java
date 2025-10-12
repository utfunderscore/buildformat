package org.readutf.buildformat.plugin.formats;

import com.sk89q.worldedit.extent.clipboard.Clipboard;

public interface SchematicFormat {

    byte[] convert(Clipboard clipboard) throws Exception;



}
