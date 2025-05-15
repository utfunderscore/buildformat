package org.readutf.buildformat.common.markers;

import org.jetbrains.annotations.Contract;

public record Position(double x,
                       double y,
                       double z
) {

    @Contract(pure = true)
    public Position add(Position position) {
        return new Position(
                this.x + position.x,
                this.y + position.y,
                this.z + position.z
        );
    }
}
