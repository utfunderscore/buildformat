package org.readutf.buildformat.builder;

import com.fasterxml.jackson.databind.JsonNode;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.annotations.Range;
import org.readutf.buildformat.requirement.annotations.Regex;
import org.readutf.buildformat.types.Position;

import java.io.File;
import java.util.List;

public record SimpleTestCase(
        @Range(min = 0, max = 5) double speedMultiplier,
        @Regex("^[a-zA-Z0-9]+$") String entityName,
        Position startPosition,
        Position endPosition,
        List<Position> checkpoints
) {

}