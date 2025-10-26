package org.readutf.buildformat.types;

public record Cuboid(Position min, Position max) {

    public Cuboid(Position min, Position max) {
        this.min = new Position(Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()));
        this.max = new Position(Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
    }



}
