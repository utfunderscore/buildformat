package org.readutf.buildformat.builder;

import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildConstructor;
import org.readutf.buildformat.types.Position;

import java.util.List;
import java.util.Map;

public class BuildConstructorTest {

    private final BuildConstructor buildConstructor = new BuildConstructor();

    @Test
    public void testBuildConstructor() throws Exception {

        buildConstructor.construct(SimpleTestCase.class, Map.of(
                "startPosition", List.of(new Position(0, 0, 0)),
                "endPosition", List.of(new Position(0, 0, 0)),
                "checkpoints", List.of(new Position(0, 0, 0))/*,
                "startPosition", List.of(new Position(0, 0, 0))*/
        ), Map.of("speedMultiplier", "1.5", "entityName", "test entity"));



    }
}
