package org.readutf.buildformat.common.markers;

import org.jetbrains.annotations.Contract;

public record Position(double x,
                       double y,
                       double z
) {

    public static final Position ZERO = new Position(0, 0, 0);

    @Contract(pure = true)
    public Position add(Position position) {
        return new Position(
                this.x + position.x,
                this.y + position.y,
                this.z + position.z
        );
    }
}
