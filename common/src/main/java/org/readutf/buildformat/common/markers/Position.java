package org.readutf.buildformat.common.markers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a 3D origin with x, y, z coordinates.
 */
public record Position(
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }

    /**
     * The zero origin constant (0, 0, 0).
     */
    public static final Position ZERO = new Position(0, 0, 0, 0, 0);

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    /**
     * Adds the given origin to this origin.
     *
     * @param position the origin to add (must not be null)
     * @return the sum of this origin and the given origin
     */
    @Contract(pure = true)
    public @NotNull Position add(@NotNull Position position) {
        return new Position(
                this.x + position.x,
                this.y + position.y,
                this.z + position.z,
                this.yaw,
                this.pitch
        );
    }

    /**
     * Subtracts the given origin from this origin.
     *
     * @param position the origin to subtract (must not be null)
     * @return the difference of this origin and the given origin
     */
    @Contract(pure = true)
    public @NotNull Position subtract(@NotNull Position position) {
        return new Position(
                this.x - position.x,
                this.y - position.y,
                this.z - position.z
        );
    }

    /**
     * Multiplies this origin by a scalar value.
     *
     * @param scalar the scalar to multiply by
     * @return a new origin scaled by the given scalar
     */
    @Contract(pure = true)
    public @NotNull Position multiply(double scalar) {
        return new Position(
                this.x * scalar,
                this.y * scalar,
                this.z * scalar
        );
    }

    /**
     * Divides this origin by a scalar value.
     *
     * @param scalar the scalar to divide by
     * @return a new origin scaled by the reciprocal of the scalar
     */
    @Contract(pure = true)
    public @NotNull Position divide(double scalar) {
        return new Position(
                this.x / scalar,
                this.y / scalar,
                this.z / scalar
        );
    }

    /**
     * Multiplies this origin by another origin, element-wise.
     *
     * @param position the origin to multiply by (must not be null)
     * @return a new origin with each component multiplied
     */
    @Contract(pure = true)
    public @NotNull Position multiply(@NotNull Position position) {
        return new Position(
                this.x * position.x,
                this.y * position.y,
                this.z * position.z
        );
    }

    /**
     * Divides this origin by another origin, element-wise.
     *
     * @param position the origin to divide by (must not be null)
     * @return a new origin with each component divided
     */
    @Contract(pure = true)
    public @NotNull Position divide(@NotNull Position position) {
        return new Position(
                this.x / position.x,
                this.y / position.y,
                this.z / position.z
        );
    }
}