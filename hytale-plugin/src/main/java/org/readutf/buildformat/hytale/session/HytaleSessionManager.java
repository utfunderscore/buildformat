package org.readutf.buildformat.hytale.session;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildDataType;
import org.readutf.buildformat.BuildManager;
import org.readutf.buildformat.hytale.utils.PlatformUtils;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;
import org.readutf.buildformat.hytale.requirement.collectors.cuboid.CuboidCollector;
import org.readutf.buildformat.hytale.requirement.collectors.position.MultiPositionRequirementCollector;
import org.readutf.buildformat.hytale.requirement.collectors.position.PositionRequirementCollector;
import org.readutf.buildformat.hytale.requirement.collectors.RequirementCollectorFactory;
import org.readutf.buildformat.hytale.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.requirement.types.number.DoubleRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class HytaleSessionManager {

    private static final Logger log = LoggerFactory.getLogger(HytaleSessionManager.class);

    private final @NotNull BuildManager buildManager;
    private final @NotNull Map<Class<? extends Requirement>, RequirementCollectorFactory> collectors;
    private final @NotNull Map<UUID, List<RequirementCollector<?>>> sessionMap;

    public HytaleSessionManager(@NotNull BuildManager buildManager) {
        this.buildManager = buildManager;
        this.sessionMap = new HashMap<>();
        this.collectors = Map.of(
                PositionRequirement.class,
                (requirement, step) -> new PositionRequirementCollector(Message.raw("%s. Select a position for %s".formatted(step, requirement.name())), requirement.name()),
                CuboidRequirement.class,
                (requirement, step) -> new CuboidCollector(Message.raw("%s. Select a region for %s".formatted(step, requirement.name()))),
                StringRequirement.class,
                (requirement, step) -> new TextInputCollector<>(Message.raw("%s. Enter a value for the %s".formatted(step, requirement.name())), str -> str),
                PositionListRequirement.class,
                MultiPositionRequirementCollector::new,
                IntegerRequirement.class,
                (requirement, step) -> new TextInputCollector<>(Message.raw("%s. Enter a integer for %s".formatted(step, requirement.name())), Integer::parseInt),
                DoubleRequirement.class,
                (requirement, step) -> new TextInputCollector<>(Message.raw("%s. Enter a number for %s".formatted(step, requirement.name())), Double::parseDouble));
    }

    public void startInputSession(
            @NotNull PlayerRef player,
            BuildDataType buildDataType,
            @NotNull String format,
            @NotNull List<Requirement> requirements)
            throws Exception {
        if (sessionMap.containsKey(player.getUuid())) throw new Exception("Session already active");

        PlatformUtils.clearInventory(player);

        CompletableFuture<Pair<BuildSetting<String>, BuildSetting<Cuboid>>> basicData = getBuildName(player)
                .thenCompose(stringBuildSetting -> getBuildRegion(player)
                        .thenApply(cuboidBuildSetting -> Pair.of(stringBuildSetting, cuboidBuildSetting)));

        basicData
                .thenCompose(buildSettingBuildSettingPair -> {
                    Cuboid cuboid = buildSettingBuildSettingPair.second().data();
                    return collectRequirementsAsync(player, requirements, cuboid, cuboid.min());
                })
                .thenAccept(results -> {
                    player.sendMessage(Message.raw("Finished"));
                    System.out.println(results);
                });

    }

    private @NotNull CompletableFuture<BuildSetting<String>> getBuildName(@NotNull PlayerRef player) {
        TextInputCollector<String> nameCollector = new TextInputCollector<>(Message.raw("Enter a name for the build").color(Color.GREEN), str -> str);
        addCollectorToSession(player, nameCollector);
        nameCollector.start(player, new Cuboid(new Position(0, 0, 0), new Position(0, 0, 0)), new Position(0, 0, 0));
        return nameCollector.getFuture().whenComplete((_, _) -> {
            nameCollector.cleanup(player);
        });
    }

    private @NotNull CompletableFuture<BuildSetting<Cuboid>> getBuildRegion(@NotNull PlayerRef player) {
        CuboidCollector build = new CuboidCollector(Message.raw("Select the region containing the build").color(Color.GREEN));
        addCollectorToSession(player, build);
        build.start(player, new Cuboid(new Position(0, 0, 0), new Position(0, 0, 0)), new Position(0, 0, 0));
        return build.getFuture().thenApply(cuboidBuildSetting -> {
            build.cleanup(player);
            return cuboidBuildSetting;
        });
    }

    private void uploadBuild(
            @NotNull PlayerRef player,
            BuildDataType buildDataType,
            int checksum,
            Cuboid cuboid,
            String buildName,
            String format,
            Map<String, BuildSetting<?>> metadata) {

    }

    public void endSession(@NotNull PlayerRef player) {
        List<RequirementCollector<?>> collectors = sessionMap.remove(player.getUuid());

        for (RequirementCollector<?> collector : collectors) {
            collector.cleanup(player);
        }
    }

    public void addCollectorToSession(@NotNull PlayerRef player, RequirementCollector<?> collector) {
        List<RequirementCollector<?>> collectors =
                this.sessionMap.getOrDefault(player.getUuid(), new ArrayList<>());
        collectors.add(collector);
        this.sessionMap.put(player.getUuid(), collectors);
    }


    private @NotNull CompletableFuture<Map<String, BuildSetting<?>>> collectRequirementsAsync(
            @NotNull PlayerRef player,
            @NotNull List<Requirement> requirements,
            @NotNull Cuboid bounds,
            @NotNull Position origin) {

        Map<String, BuildSetting<?>> results = new LinkedHashMap<>(); // keep order
        List<RequirementCollector<?>> finishedCollectors = new ArrayList<>();

        // start the chain with a completed future
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (int i = 0; i < requirements.size(); i++) {
            final int index = i;
            final Requirement requirement = requirements.get(i);

            chain = chain.thenCompose(ignored -> {
                RequirementCollectorFactory<?> requirementCollector = collectors.get(requirement.getClass());
                if (requirementCollector == null) {
                    CompletableFuture<Void> failed = new CompletableFuture<>();
                    failed.completeExceptionally(new Exception("No collector available for " + requirement.getClass()));
                    return failed;
                }

                RequirementCollector<?> collector = requirementCollector.createCollector(requirement, index + 1);
                addCollectorToSession(player, collector);

                System.out.println("Starting collector for " + requirement.name());
                PlatformUtils.clearInventory(player);

                // clear inventory and start only now (previous collector has finished)
                collector.start(player, bounds, origin);


                // convert the collector's Future to CompletableFuture and wait for it
                CompletableFuture<? extends BuildSetting<?>> settingCf = collector.getFuture();

                return settingCf.thenAccept(setting -> {
                    // record result and mark collector as finished (for later cleanup)
                    results.put(requirement.name(), setting);
                    finishedCollectors.add(collector);
                });
            });
        }


        // after chain completes (or fails), run cleanup and produce the results map or propagate exception
        return chain.thenApply(e -> {
            System.out.println("FINISHED? " + finishedCollectors.size());

            // best-effort cleanup of finished collectors
            for (RequirementCollector<?> c : finishedCollectors) {
                try {
                    System.out.println("Cleaning up for " + c.getClass());
                    c.cleanup(player);
                } catch (Exception cleanupEx) {
                    log.error("Failed cleaning up collector {}", c.getClass(), cleanupEx);
                }
            }

            return results;
        });
    }
}
