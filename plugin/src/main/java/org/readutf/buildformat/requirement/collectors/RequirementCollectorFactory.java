package org.readutf.buildformat.requirement.collectors;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.RequirementCollector;

public interface RequirementCollectorFactory {

    @NotNull
    RequirementCollector<?> createCollector(@NotNull Requirement requirement, int step);
}
