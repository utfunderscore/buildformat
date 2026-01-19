package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.concurrent.CompletableFuture;

public interface RequirementCollector<T> {

    void start(@NotNull Player player, @NotNull Cuboid bounds, @NotNull Position origin);

    void cleanup(Player player);

    CompletableFuture<BuildSetting<T>> getFuture();

}
