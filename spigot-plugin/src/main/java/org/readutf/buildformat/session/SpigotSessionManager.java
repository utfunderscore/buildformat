package org.readutf.buildformat.session;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import it.unimi.dsi.fastutil.Pair;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildData;
import org.readutf.buildformat.BuildDataType;
import org.readutf.buildformat.BuildManager;
import org.readutf.buildformat.converter.SchemPolarConverter;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.requirement.collectors.cuboid.BuildCuboidCollector;
import org.readutf.buildformat.requirement.collectors.position.MultiPositionRequirementCollector;
import org.readutf.buildformat.requirement.collectors.position.PositionRequirementCollector;
import org.readutf.buildformat.requirement.collectors.cuboid.CuboidRequirementCollector;
import org.readutf.buildformat.requirement.collectors.RequirementCollectorFactory;
import org.readutf.buildformat.requirement.collectors.text.BuildNameCollector;
import org.readutf.buildformat.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.requirement.types.number.DoubleRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.settings.BuildMetadata;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.RegionSelectionTool;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SpigotSessionManager {

    private static final Logger log = LoggerFactory.getLogger(SpigotSessionManager.class);

    private final @NotNull BuildManager buildManager;
    private final Map<Class<? extends Requirement>, RequirementCollectorFactory> collectors;
    private final Map<UUID, List<RequirementCollector<?>>> sessionMap;

    public SpigotSessionManager(@NotNull BuildManager buildManager) {
        this.buildManager = buildManager;
        this.sessionMap = new HashMap<>();
        this.collectors = Map.of(
                PositionRequirement.class,
                PositionRequirementCollector::new,
                CuboidRequirement.class,
                CuboidRequirementCollector::new,
                StringRequirement.class,
                (requirement, step) -> new TextInputCollector<>(requirement.name(), step, str -> str),
                PositionListRequirement.class,
                MultiPositionRequirementCollector::new,
                IntegerRequirement.class,
                (requirement, step) -> new TextInputCollector<>(requirement.name(), step, Integer::parseInt),
                DoubleRequirement.class,
                (requirement, step) -> new TextInputCollector<>(requirement.name(), step, Double::parseDouble));
    }

    public void startInputSession(
            @NotNull Player player,
            BuildDataType buildDataType,
            @NotNull String format,
            @NotNull List<Requirement> requirements)
            throws Exception {
        if (sessionMap.containsKey(player.getUniqueId())) throw new Exception("Session already active");

        player.getInventory().clear();

        CompletableFuture<Pair<BuildSetting<String>, BuildSetting<Cuboid>>> basicData = getBuildName(player)
                .thenCompose(stringBuildSetting -> getBuildRegion(player)
                        .thenApply(cuboidBuildSetting -> Pair.of(stringBuildSetting, cuboidBuildSetting)));

        basicData
                .thenCompose(buildSettingBuildSettingPair -> {
                    Cuboid cuboid = buildSettingBuildSettingPair.second().data();
                    return collectRequirements(player, requirements, cuboid, cuboid.min());
                })
                .thenAccept(results -> {
                    player.sendMessage("Finished");
                    System.out.println(results);
                });

        //        String buildName = getBuildName(player).data();
        //        Cuboid cuboid = getBuildRegion(player).data();
        //
        //        @NotNull Map<String, BuildSetting<?>> results = collectRequirements(player, requirements, cuboid,
        // cuboid.min());

        //
        //        player.getInventory().clear();
        //
        //        player.sendMessage("Data collected:");
        //        for (Map.Entry<String, BuildSetting<?>> stringObjectEntry : results.entrySet()) {
        //            player.sendMessage(" - " + stringObjectEntry.getKey() + ": "
        //                    + stringObjectEntry.getValue().data());
        //        }
        //
        //        TextComponent upload = Component.text("[Upload]")
        //                .color(NamedTextColor.LIGHT_PURPLE)
        //                .decorate(TextDecoration.BOLD)
        //                .clickEvent(ClickEvent.callback(_ -> {
        //                    CompletableFuture.runAsync(() -> {
        //                        uploadBuild(
        //                                player,
        //                                buildDataType,
        //                                BuildFormatManager.getInstance().checksum(requirements),
        //                                cuboid,
        //                                buildName,
        //                                format,
        //                                results);
        //                    });
        //                }));
        //
        //        player.sendMessage(
        //                Component.text("Upload build? ").color(NamedTextColor.GREEN).append(upload));
        //
        //        endSession(player);
    }

    private CompletableFuture<BuildSetting<String>> getBuildName(@NotNull Player player) {
        BuildNameCollector nameCollector = new BuildNameCollector();
        addCollectorToSession(player, nameCollector);
        nameCollector.start(player, new Cuboid(new Position(0, 0, 0), new Position(0, 0, 0)), new Position(0, 0, 0));
        return nameCollector.getFuture().whenComplete((_, _) -> {
            nameCollector.cleanup(player);
        });
    }

    private CompletableFuture<BuildSetting<Cuboid>> getBuildRegion(@NotNull Player player) {
        BuildCuboidCollector build = new BuildCuboidCollector();
        addCollectorToSession(player, build);
        build.start(player, new Cuboid(new Position(0, 0, 0), new Position(0, 0, 0)), new Position(0, 0, 0));
        return build.getFuture().thenApply(cuboidBuildSetting -> {
            build.cleanup(player);
            return cuboidBuildSetting;
        });
    }

    private void uploadBuild(
            @NotNull Player player,
            BuildDataType buildDataType,
            int checksum,
            Cuboid cuboid,
            String buildName,
            String format,
            Map<String, BuildSetting<?>> metadata) {
        try {
            byte[] data =
                    switch (buildDataType) {
                        case POLAR_WORLD -> createSchematic(player.getWorld(), cuboid);
                        case SPONGE_SCHEMATIC -> createPolarWorld(createSchematic(player.getWorld(), cuboid));
                        default -> throw new Exception("Unsupported schematic type.");
                    };

            buildManager.saveBuild(buildName, format, checksum, new BuildData(data), new BuildMetadata(metadata));

            player.sendMessage(Component.text("Upload successful!").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(
                    Component.text("Upload failed: " + e.getMessage()).color(NamedTextColor.RED));
            log.error("Upload failed for player {}", player.getName(), e);
        }
    }

    public void endSession(@NotNull Player player) {
        List<RequirementCollector<?>> collectors = sessionMap.remove(player.getUniqueId());
        PositionTool.clearTool(player.getUniqueId());
        RegionSelectionTool.clearTool(player.getUniqueId());
        player.getInventory().clear();

        for (RequirementCollector<?> collector : collectors) {
            collector.cleanup(player);
        }
    }

    private byte @NotNull [] createPolarWorld(byte[] schematicData) throws Exception {
        Schematic schematic = SchematicReader.sponge().read(schematicData);
        return SchemPolarConverter.convert(schematic);
    }

    private byte @NotNull [] createSchematic(@NotNull World world, @NotNull Cuboid cuboid) throws IOException {

        Position min = cuboid.min();
        Position max = cuboid.max();
        BlockVector3 minVec3 = BlockVector3.at(min.x(), min.y(), min.z());
        BlockVector3 maxVec3 = BlockVector3.at(max.x(), max.y(), max.z());

        System.out.println(min);
        System.out.println(max);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(world), minVec3, maxVec3);
        try (BlockArrayClipboard clipboard = new BlockArrayClipboard(region)) {

            ForwardExtentCopy forwardExtentCopy =
                    new ForwardExtentCopy(BukkitAdapter.adapt(world), region, clipboard, region.getMinimumPoint());
            Operations.complete(forwardExtentCopy);

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_V3_SCHEMATIC.getWriter(outputStream)) {
                writer.write(clipboard);
            }
        }
        return outputStream.toByteArray();
    }

    public void addCollectorToSession(@NotNull Player player, RequirementCollector<?> collector) {
        List<RequirementCollector<?>> collectors =
                this.sessionMap.getOrDefault(player.getUniqueId(), new ArrayList<>());
        collectors.add(collector);
        this.sessionMap.put(player.getUniqueId(), collectors);
    }

    private @NotNull CompletableFuture<Map<String, BuildSetting<?>>> collectRequirements(
            @NotNull Player player,
            @NotNull List<Requirement> requirements,
            @NotNull Cuboid bounds,
            @NotNull Position origin) {

        // Keep insertion order so results map is deterministic (like original loop order)
        Map<String, CompletableFuture<? extends BuildSetting<?>>> futureMap = new LinkedHashMap<>();
        List<RequirementCollector<?>> createdCollectors = new ArrayList<>();

        for (int i = 0; i < requirements.size(); i++) {
            Requirement requirement = requirements.get(i);

            RequirementCollectorFactory<?> requirementCollector = collectors.get(requirement.getClass());
            if (requirementCollector == null) {
                CompletableFuture<Map<String, BuildSetting<?>>> failed = new CompletableFuture<>();
                failed.completeExceptionally(new Exception("No collector available for " + requirement.getClass()));
                return failed;
            }

            RequirementCollector<?> collector = requirementCollector.createCollector(requirement, i + 1);
            addCollectorToSession(player, collector);

            // keep the original behavior of clearing inventory before starting each collector
            player.getInventory().clear();
            collector.start(player, bounds, origin);

            CompletableFuture<? extends BuildSetting<?>> cf = collector.getFuture();

            futureMap.put(requirement.name(), cf);
            createdCollectors.add(collector);
        }

        CompletableFuture<?>[] cfs = futureMap.values().toArray(new CompletableFuture[0]);
        CompletableFuture<Void> all = CompletableFuture.allOf(cfs);

        // When all complete, assemble results map. Ensure cleanup runs even if some futures complete exceptionally.
        CompletableFuture<Map<String, BuildSetting<?>>> result = all.thenApply(v -> {
                    Map<String, BuildSetting<?>> results = new HashMap<>();
                    for (Map.Entry<String, CompletableFuture<? extends BuildSetting<?>>> entry : futureMap.entrySet()) {
                        // join() will rethrow as CompletionException if any future completed exceptionally
                        results.put(entry.getKey(), entry.getValue().join());
                    }
                    return results;
                })
                .whenComplete((map, ex) -> {
                    // run cleanup for all created collectors (best-effort; swallow/collect exceptions)
                    for (RequirementCollector<?> collector : createdCollectors) {
                        collector.cleanup(player);
                    }
                });

        return result;
    }
}
