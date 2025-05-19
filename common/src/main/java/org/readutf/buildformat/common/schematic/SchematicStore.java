package org.readutf.buildformat.common.schematic;

import java.util.List;
import org.readutf.buildformat.common.exception.BuildFormatException;

public interface SchematicStore {

    void save(BuildSchematic buildSchematic) throws BuildFormatException;

    /**
     * Load the latest version of the build schematic
     * @param name - the name of the build schematic
     * @return the schematic data
     * @throws BuildFormatException - if the schematic is not found or cannot be loaded
     */
    BuildSchematic load(String name) throws BuildFormatException;

    List<String> history(String name) throws BuildFormatException;

}
