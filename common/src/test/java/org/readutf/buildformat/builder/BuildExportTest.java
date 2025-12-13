package org.readutf.buildformat.builder;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.BuildFormatManager;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.types.Position;

import java.io.File;
import java.util.List;

public class BuildExportTest {

    record ConvertedTestCase(
            File file
    ) {

    }

    @Test
    public void testExport() throws Exception {

        List<Requirement> requirements = BuildFormatManager.getInstance().generateRequirements(SimpleTestCase.class);
        BuildFormatManager.getInstance().serializeRequirements(new File("test.json"), requirements);

    }

    @Test
    public void adapterExport() throws Exception {

        BuildFormatManager.getInstance().<Position, File>registerAdapter(new TypeReference<>() {}, new TypeReference<>() {}, position -> new File("test.json"));


        List<Requirement> requirements = BuildFormatManager.getInstance().generateRequirements(ConvertedTestCase.class);
        BuildFormatManager.getInstance().serializeRequirements(new File("test.json"), requirements);

    }

}
