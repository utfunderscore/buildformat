package org.readutf.buildformat.builder;

import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Position;

import java.util.List;
import java.util.Map;

public class BuildDataConstructTest {

    @Test
    public void testConstruct() throws Exception {
        BuildFormatManager instance = BuildFormatManager.getInstance();

        Map<String, BuildSetting<?>> settings = Map.of(
                "speedMultiplier", new BuildSetting<>(2.5),
                "entityName", new BuildSetting<>("TestEntity"),
                "startPosition", new BuildSetting<>(new Position(0, 64, 0)),
                "endPosition", new BuildSetting<>(new Position(100, 64, 100)),
                "checkpoints", new BuildSetting<>(List.of(
                        new Position(20, 64, 20),
                        new Position(50, 64, 50),
                        new Position(80, 64, 80)
                ))
        );

        SimpleTestCase testCase = instance.construct(SimpleTestCase.class, settings);
    }

}
