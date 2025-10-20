package org.readutf.buildformat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.readutf.buildformat.exception.BuildSerializeException;

public class BuildCodec {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String serialize(BuildFormat buildFormat) throws BuildSerializeException {
        try {
            return objectMapper.writeValueAsString(buildFormat);
        } catch (Exception e) {
            throw new BuildSerializeException("Failed to serialize BuildFormat", e);
        }
    }

    public static BuildFormat deserialize(String serialized) throws BuildSerializeException {
        try {
            return objectMapper.readValue(serialized, BuildFormat.class);
        } catch (Exception e) {
            throw new BuildSerializeException("Failed to deserialize BuildFormat", e);
        }
    }

}
