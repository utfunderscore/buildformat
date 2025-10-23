package org.readutf.buildformat.types;

public record Position(double x, double y, double z, float yaw, float pitch) {

    public Position(int x, int y, int z) {
        this(x, y, z, 0f, 0f);
    }

    public Position(double x, double y, double z) {
        this(x, y, z, 0, 0);
    }
}
