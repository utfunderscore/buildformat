package org.readutf.buildformat.common.format;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.util.List;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildFormatLoader {
    private static final Logger logger = LoggerFactory.getLogger(BuildFormatLoader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();


}
