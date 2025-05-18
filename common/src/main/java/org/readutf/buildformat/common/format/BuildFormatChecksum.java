package org.readutf.buildformat.common.format;

import java.util.Arrays;
import java.util.Objects;

public record BuildFormatChecksum(String name, byte[] checksum) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BuildFormatChecksum that = (BuildFormatChecksum) o;
        return Objects.equals(name, that.name) && Objects.deepEquals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, Arrays.hashCode(checksum));
    }
}
