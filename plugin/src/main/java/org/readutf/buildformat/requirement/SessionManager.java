package org.readutf.buildformat.requirement;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import net.hollowcube.schem.Schematic;
import net.hollowcube.schem.reader.SchematicReader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildData;
import org.readutf.buildformat.BuildManager;
import org.readutf.buildformat.converter.SchemPolarConverter;
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
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final BuildManager buildManager;
    private final HashSet<UUID> sessions;
    private final Map<Class<? extends Requirement>, RequirementCollectorFactory> collectors;

    public SessionManager(BuildManager buildManager) {
        this.buildManager = buildManager;
        this.sessions = new HashSet<>();
        this.collectors = Map.of(
                PositionRequirement.class, PositionRequirementCollector::new,
                CuboidRequirement.class, CuboidRequirementCollector::new,
                StringRequirement.class, (requirement, step) -> new TextInputCollector<>(requirement.name(), step, str -> str),
                PositionListRequirement.class, MultiPositionRequirementCollector::new,
                IntegerRequirement.class, (requirement, step) -> new TextInputCollector<>(requirement.name(), step, Integer::parseInt));
    }

    public void startInputSession(
            @NotNull Player player, @NotNull String format, @NotNull List<Requirement> requirements) throws Exception {
        if (sessions.contains(player.getUniqueId())) throw new Exception("Session already active");

        sessions.add(player.getUniqueId());
        player.getInventory().clear();

        String buildName = getBuildName(player);
        Cuboid cuboid = getBuildRegion(player);

        Map<String, Object> results = collectRequirements(player, requirements);

        player.getInventory().clear();

        player.sendMessage("Data collected:");
        for (Map.Entry<String, Object> stringObjectEntry : results.entrySet()) {
            player.sendMessage(" - " + stringObjectEntry.getKey() + ": " + stringObjectEntry.getValue());
        }
        sessions.remove(player.getUniqueId());

        TextComponent upload = Component.text("[Upload]")
                .color(NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.callback(_ -> {
                    CompletableFuture.runAsync(() -> {
                        uploadBuild(player, cuboid, buildName, format);
                    });
                }));

        player.sendMessage(
                Component.text("Upload build? ").color(NamedTextColor.GREEN).append(upload));
    }

    private static String getBuildName(@NotNull Player player) {
        BuildNameCollector nameCollector = new BuildNameCollector();
        nameCollector.start(player);
        String buildName = nameCollector.awaitBlocking();
        nameCollector.cleanup(player);
        return buildName;
    }

    private static Cuboid getBuildRegion(@NotNull Player player) {
        BuildCuboidCollector build = new BuildCuboidCollector();
        build.start(player);
        Cuboid cuboid = build.awaitBlocking();
        build.cleanup(player);
        return cuboid;
    }

    private void uploadBuild(@NotNull Player player, Cuboid cuboid, String buildName, String format) {
        try {
            byte[] schematicData = createSchematic(player.getWorld(), cuboid);
            byte[] polarWorld = createPolarWorld(schematicData);
            buildManager.saveBuild(buildName, format, new BuildData(schematicData, polarWorld));
            player.sendMessage(Component.text("Upload successful!").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(
                    Component.text("Upload failed: " + e.getMessage()).color(NamedTextColor.RED));
            log.error("Upload failed for player {}", player.getName(), e);
        }
    }

    public byte[] createPolarWorld(byte[] schematicData) throws Exception {
        Schematic schematic = SchematicReader.sponge().read(schematicData);
        return SchemPolarConverter.convert(schematic);
    }

    public byte[] createSchematic(@NotNull World world, @NotNull Cuboid cuboid) throws IOException {

        Position min = cuboid.min();
        Position max = cuboid.max();
        BlockVector3 minVec3 = BlockVector3.at(min.x(), min.y(), min.z());
        BlockVector3 maxVec3 = BlockVector3.at(max.x(), max.y(), max.z());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        CuboidRegion region = new CuboidRegion(BukkitAdapter.adapt(world), minVec3, maxVec3);
        try (BlockArrayClipboard clipboard = new BlockArrayClipboard(region)) {

            ForwardExtentCopy forwardExtentCopy =
                    new ForwardExtentCopy(BukkitAdapter.adapt(world), region, clipboard, region.getMinimumPoint());
            Operations.complete(forwardExtentCopy);

            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(outputStream)) {
                writer.write(clipboard);
            }
        }
        return outputStream.toByteArray();
    }

    private @NotNull Map<String, Object> collectRequirements(
            @NotNull Player player, @NotNull List<Requirement> requirements) throws Exception {
        Map<String, Object> results = new HashMap<>();
        List<RequirementCollector<?>> completed = new ArrayList<>();

        for (int i = 0; i < requirements.size(); i++) {

            Requirement requirement = requirements.get(i);

            RequirementCollectorFactory requirementCollector = collectors.get(requirement.getClass());
            if (requirementCollector == null) {
                throw new Exception("No collector available for " + requirement.getClass());
            }
            RequirementCollector<?> collector = requirementCollector.createCollector(requirement, i + 1);

            player.getInventory().clear();
            collector.start(player);
            results.put(requirement.name(), collector.awaitBlocking());
            completed.add(collector);
        }

        for (RequirementCollector<?> collector : completed) {
            collector.cleanup(player);
        }
        return results;
    }
}
