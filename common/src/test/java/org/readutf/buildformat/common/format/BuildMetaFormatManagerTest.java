package org.readutf.buildformat.common.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.markers.Position;
import org.readutf.buildformat.common.format.requirements.Requirement;

class BuildMetaFormatManagerTest {

    public record TestFormat(
            @Requirement(name = "test-a") String single,
            @Requirement(startsWith = "test-b") List<Marker> startsWith,
            @Requirement(startsWith = "test-c", minimum = 2) List<Marker> minimum,
            @Requirement InnerTest inner
    ) implements BuildFormat {
    }

    public record InnerTest(
            @Requirement(name = "test-d") Marker single
    ) implements BuildFormat {
    }

    private final @NotNull BuildFormatManager buildFormatManager = new BuildFormatManager();

    @Test
    void testCompile() throws BuildFormatException {
        buildFormatManager.compile("test", TestFormat.class);
    }

    @Test
    void successfullyConstructBuildFormat() throws BuildFormatException {

        var single = List.of(new Marker("test-a", new Position(1, 0, 0), new Position(0, 0, 0)));
        var startsWith = List.of(
                new Marker("test-b1", new Position(2, 0, 0), new Position(0, 0, 0)),
                new Marker("test-b2", new Position(2, 1, 0), new Position(0, 0, 0))
        );
        var minimum = List.of(
                new Marker("test-c1", new Position(3, 0, 0), new Position(0, 0, 0)),
                new Marker("test-c2", new Position(3, 1, 0), new Position(0, 0, 0))
        );
        var innerSingle = List.of(new Marker("test-d", new Position(4, 0, 0), new Position(0, 0, 0)));

        var markers = new ArrayList<Marker>();
        markers.addAll(single);
        markers.addAll(startsWith);
        markers.addAll(minimum);
        markers.addAll(innerSingle);

        TestFormat format = buildFormatManager.build(markers, TestFormat.class);

        assertEquals(single.getFirst().toString(), format.single());
        assertEquals(startsWith, format.startsWith());
        assertEquals(minimum, format.minimum());
        assertEquals(innerSingle.getFirst(), format.inner().single());
    }

    @Test
    void testPositionData() throws BuildFormatException {

        record PositionDataFormat(
                @Requirement(name = "test-a") Position position
        ) implements BuildFormat {
        }

        var markers = List.of(
                new Marker("test-a", new Position(1, 0, 0), new Position(0, 0, 0))
        );

        PositionDataFormat format = buildFormatManager.build(markers, PositionDataFormat.class);
    }

    @Test
    void checksumTest() throws BuildFormatException, IOException {

        Path tempFile = Files.createTempFile("buildmeta", "checksumTest");

        byte[] direct = buildFormatManager.checksum("test", TestFormat.class);
        buildFormatManager.save(tempFile, "test", TestFormat.class);
        byte[] fromFile = buildFormatManager.checksum(tempFile);

        assertEquals(direct, fromFile);
    }

    @Test
    void testNotARecord() {

        class InvalidFormat implements BuildFormat {
            @Requirement(name = "test-a")
            Marker single;
        }

        Exception exception = assertThrows(BuildFormatException.class, () -> {
            buildFormatManager.build(List.of(), InvalidFormat.class);
        });

    }

    @Test
    void testInvalidParameterType() {

        record InvalidFormat(
                @Requirement(name = "test-a") int invalid
        ) implements BuildFormat {
        }

        assertThrows(BuildFormatException.class, () -> {
            buildFormatManager.build(List.of(new Marker("test-a", Position.ZERO, Position.ZERO)), InvalidFormat.class);
        });
    }

    @Test
    void unableToConstruct() {
        record InvalidFormat(
                @Requirement(name = "test-a") Marker single
        ) implements BuildFormat {
            public InvalidFormat {
                throw new RuntimeException("Unable to construct");
            }
        }

        assertThrows(BuildFormatException.class, () -> {
            buildFormatManager.build(List.of(new Marker("test-a", new Position(0, 0, 0), new Position(0, 0, 0))), InvalidFormat.class);
        });
    }

    @Test
    void testNotEnoughMarkers() {

        record InvalidFormat(
                @Requirement(name = "test-b", minimum = 2) List<Marker> list
        ) implements BuildFormat {
        }

        var exception = assertThrows(BuildFormatException.class, () -> {
            buildFormatManager.build(List.of(), InvalidFormat.class);
        });
        assertEquals("Not enough markers found for parameter: list", exception.getMessage());
    }

}