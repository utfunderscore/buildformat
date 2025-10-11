package org.readutf.buildformat.common.schematic;

import java.util.List;
import org.readutf.buildformat.common.exception.BuildFormatException;

public interface BuildSchematicStore {

    void save(BuildData buildData) throws BuildFormatException;

    /**
     * Load the latest version of the build schematic
     * @param name - the name of the build schematic
     * @return the schematic data
     * @throws BuildFormatException - if the schematic is not found or cannot be loaded
     */
    BuildData load(String name) throws BuildFormatException;

    List<String> history(String name) throws BuildFormatException;

}
