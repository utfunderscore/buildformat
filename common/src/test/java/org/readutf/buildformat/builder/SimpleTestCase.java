package org.readutf.buildformat.builder;


import org.readutf.buildformat.types.Position;

import java.util.List;

public record SimpleTestCase(
        double speedMultiplier,
        String entityName,
        Position startPosition,
        Position endPosition,
        List<Position> checkpoints
) {
}
