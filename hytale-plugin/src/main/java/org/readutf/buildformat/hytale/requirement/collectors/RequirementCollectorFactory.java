package org.readutf.buildformat.hytale.requirement.collectors;

import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;

public interface RequirementCollectorFactory<T> {

    @NotNull
    RequirementCollector<T> createCollector(@NotNull Requirement requirement, int step);
}
