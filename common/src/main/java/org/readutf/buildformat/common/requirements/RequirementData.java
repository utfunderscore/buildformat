package org.readutf.buildformat.common.requirements;

import org.jetbrains.annotations.NotNull;

public record RequirementData(
        @NotNull String regex,
        int minimum
) {
}
