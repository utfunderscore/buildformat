package org.readutf.buildformat.common.markers;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a origin inside a build
 * @param name The name of the marker
 * @param origin The origin of the marker
 * @param offset The offset of the marker
 */
public record Marker(String name, Position origin, Position offset) {

    @Contract(pure = true) @JsonIgnore
    public @NotNull Position getTargetPosition() {
        return origin.add(offset);
    }
}