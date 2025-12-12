package org.readutf.buildformat.builder;

import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;

import java.io.File;
import java.util.List;

public class BuildExportTest {

    @Test public void testExport() throws Exception {

        List<Requirement> requirements = BuildFormatManager.getInstance().generateRequirements(SimpleTestCase.class);
        BuildFormatManager.getInstance().serializeRequirements(new File("test.json"), requirements);

    }

}
