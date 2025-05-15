package org.readutf.buildformat.common.markers;

/**
 * Represents a position inside a build
 * @param name The name of the marker
 * @param position The position of the marker
 * @param offset The offset of the marker
 */
public record Marker(String name, Position position, Position offset) {

    public Position getTargetPosition() {
        return position.add(offset);
    }
}