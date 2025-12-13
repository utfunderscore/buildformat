package org.readutf.buildformat.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.DynamicTypeParser;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class BuildDataConstructTest {

    @Test public void testConstruct() throws Exception {
        BuildFormatManager instance = BuildFormatManager.getInstance();

        instance.<String, Position>registerAdapter(new TypeReference<>() {}, new TypeReference<>() {}, s -> new Position(0, 0, 0));

        Map<String, BuildSetting<?>> settings = Map.of("speedMultiplier", new BuildSetting<>(2.5, new TypeReference<>() {
        }), "entityName", new BuildSetting<>("TestEntity", new TypeReference<>() {
        }), "startPosition", new BuildSetting<>("test", new TypeReference<>() {
        }), "endPosition", new BuildSetting<>(new Position(100, 64, 100), new TypeReference<>() {
        }), "checkpoints", new BuildSetting<>(List.of(new Position(20, 64, 20), new Position(50, 64, 50), new Position(80, 64, 80)), new TypeReference<>() {
        }));



        SimpleTestCase testCase = instance.construct(SimpleTestCase.class, settings);
    }

}
