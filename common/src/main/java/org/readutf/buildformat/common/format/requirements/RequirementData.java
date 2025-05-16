package org.readutf.buildformat.common.format.requirements;

import org.jetbrains.annotations.NotNull;

public record RequirementData(
        @NotNull String regex,
        int minimum
) {
}
