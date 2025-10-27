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
import org.readutf.buildformat.converter.SchemPolarConverter;
import org.readutf.buildformat.requirement.collectors.cuboid.BuildRequirementCollector;
import org.readutf.buildformat.requirement.collectors.position.PositionRequirementCollector;
import org.readutf.buildformat.requirement.collectors.cuboid.CuboidRequirementCollector;
import org.readutf.buildformat.requirement.collectors.RequirementCollectorFactory;
import org.readutf.buildformat.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private final HashSet<UUID> sessions;
    private final Map<Class<? extends Requirement>, RequirementCollectorFactory> collectors;
    private static SessionManager sessionManager = new SessionManager();

    public SessionManager() {
        this.sessions = new HashSet<>();
        this.collectors = Map.of(
                PositionRequirement.class,
                PositionRequirementCollector::new,
                CuboidRequirement.class,
                CuboidRequirementCollector::new,
                StringRequirement.class,
                (requirement, step) -> new TextInputCollector<>(requirement.getName(), step, str -> str),
                IntegerRequirement.class,
                (requirement, step) -> new TextInputCollector<>(requirement.getName(), step, Integer::parseInt));
    }

    public void startInputSession(@NotNull Player player, @NotNull List<Requirement> requirements) throws Exception {
        if (sessions.contains(player.getUniqueId())) {
            throw new Exception("Session already active");
        }
        sessions.add(player.getUniqueId());

        player.getInventory().clear();

        BuildRequirementCollector build = new BuildRequirementCollector();
        build.start(player);
        Cuboid cuboid = build.awaitBlocking();
        build.cleanup(player);

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
                .clickEvent(ClickEvent.callback(_ -> uploadBuild(player, cuboid)));

        player.sendMessage(
                Component.text("Upload build? ").color(NamedTextColor.GREEN).append(upload));
    }

    private void uploadBuild(@NotNull Player player, Cuboid cuboid) {
        try {
            byte[] schematicData = createSchematic(player.getWorld(), cuboid);
            byte[] polarWorld = createPolarWorld(schematicData);

            new BuildData(schematicData, polarWorld);
        } catch (Exception e) {
            player.sendMessage(Component.text("Upload failed: " + e.getMessage()).color(NamedTextColor.RED));
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

            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                    BukkitAdapter.adapt(world), region, clipboard, region.getMinimumPoint()
            );
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
            results.put(requirement.getName(), collector.awaitBlocking());
            completed.add(collector);
        }

        for (RequirementCollector<?> collector : completed) {
            collector.cleanup(player);
        }
        return results;
    }

    public static SessionManager get() {
        return sessionManager;
    }
}
