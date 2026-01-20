package org.readutf.buildformat.hytale.requirement;

import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.concurrent.CompletableFuture;

public interface RequirementCollector<T> {

    void start(@NotNull PlayerRef playerRef, @NotNull Cuboid bounds, @NotNull Position origin);

    void cleanup(PlayerRef player);

    CompletableFuture<BuildSetting<T>> getFuture();

}
