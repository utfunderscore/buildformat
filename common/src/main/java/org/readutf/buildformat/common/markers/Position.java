package org.readutf.buildformat.common.markers;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a 3D position with x, y, z coordinates.
 */
public record Position(
        double x,
        double y,
        double z
) {
    /**
     * The zero position constant (0, 0, 0).
     */
    public static final Position ZERO = new Position(0, 0, 0);

    /**
     * Adds the given position to this position.
     *
     * @param position the position to add (must not be null)
     * @return the sum of this position and the given position
     */
    @Contract(pure = true)
    public @NotNull Position add(@NotNull Position position) {
        return new Position(
                this.x + position.x,
                this.y + position.y,
                this.z + position.z
        );
    }

    /**
     * Subtracts the given position from this position.
     *
     * @param position the position to subtract (must not be null)
     * @return the difference of this position and the given position
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
     * Multiplies this position by a scalar value.
     *
     * @param scalar the scalar to multiply by
     * @return a new position scaled by the given scalar
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
     * Divides this position by a scalar value.
     *
     * @param scalar the scalar to divide by
     * @return a new position scaled by the reciprocal of the scalar
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
     * Multiplies this position by another position, element-wise.
     *
     * @param position the position to multiply by (must not be null)
     * @return a new position with each component multiplied
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
     * Divides this position by another position, element-wise.
     *
     * @param position the position to divide by (must not be null)
     * @return a new position with each component divided
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