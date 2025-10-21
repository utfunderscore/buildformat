package org.readutf.buildformat.requirement;

public record PositionRequirement(
        String name,
        int minimumRequired,
        int maximumRequired
) {
}
