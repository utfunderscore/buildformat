package org.readutf.buildformat.common.format;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.common.format.requirements.RequirementData;

import java.util.List;

public record CompiledBuildFormat(@NotNull String name, @NotNull List<RequirementData> requirements) {

}
