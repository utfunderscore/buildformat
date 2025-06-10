package org.readutf.buildformat.common.markers;

/**
 * Represents a origin inside a build
 * @param name The name of the marker
 * @param origin The origin of the marker
 * @param offset The offset of the marker
 */
public record Marker(String name, Position origin, Position offset) {

    public Position getTargetPosition() {
        return origin.add(offset);
    }
}