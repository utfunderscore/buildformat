package org.readutf.buildformat.types;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Cuboid(Position min, Position max) {

    public Cuboid(@NotNull Position min, @NotNull Position max) {
        this.min = new Position(Math.min(min.x(), max.x()), Math.min(min.y(), max.y()), Math.min(min.z(), max.z()));
        this.max = new Position(Math.max(min.x(), max.x()), Math.max(min.y(), max.y()), Math.max(min.z(), max.z()));
    }

    @Contract("_ -> new") @NotNull public Cuboid relative(@NotNull Position origin) {
        return new Cuboid(new Position(min.x() - origin.x(), min.y() - origin.y(), min.z() - origin.z()), new Position(max.x() - origin.x(), max.y() - origin.y(), max.z() - origin.z()));
    }

}
