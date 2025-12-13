package org.readutf.buildformat.types;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public record Position(double x, double y, double z, float yaw, float pitch) {

    public Position(int x, int y, int z) {
        this(x, y, z, 0f, 0f);
    }

    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    @Override
    @NotNull
    public String toString() {
        if(yaw == 0 && pitch == 0) {
            return String.format("(x=%.2f, y=%.2f, z=%.2f)", x, y, z);
        } else {
            return String.format("(x=%.2f, y=%.2f, z=%.2f, yaw=%.2f, pitch=%.2f}", x, y, z, yaw, pitch);
        }
    }

    @Contract("_ -> new")
    @NotNull
    public Position relative(@NotNull Position origin) {
        return new Position(x - origin.x(), y - origin.y(), z - origin.z(), yaw, pitch);
    }

}
