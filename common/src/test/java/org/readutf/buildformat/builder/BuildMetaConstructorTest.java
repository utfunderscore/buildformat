package org.readutf.buildformat.builder;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;

import java.lang.reflect.*;
import java.util.List;

public class BuildMetaConstructorTest {

    @Test
    public void testBuildConstructor() throws Exception {

        BuildFormatManager buildFormatManager = new BuildFormatManager();

        List<Requirement> requirements = buildFormatManager.generateRequirements(SimpleTestCase.class);
        JsonNode node = buildFormatManager.serializeRequirements(requirements);

        System.out.println(node);
        System.out.println(buildFormatManager.deserializeRequirements(node));

    }
}
