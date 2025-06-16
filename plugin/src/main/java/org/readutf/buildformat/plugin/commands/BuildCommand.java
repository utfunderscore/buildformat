package org.readutf.buildformat.plugin.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.flag.Flag;
import dev.rollczi.litecommands.annotations.join.Join;
import dev.rollczi.litecommands.annotations.varargs.Varargs;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.format.BuildFormatManager;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildformat.common.schematic.BuildSchematic;
import org.readutf.buildformat.common.schematic.BuildSchematicStore;
import org.readutf.buildformat.plugin.commands.types.BuildType;
import org.readutf.buildformat.plugin.formats.BuildFormatCache;
import org.readutf.buildformat.plugin.marker.MarkerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "build")
public class BuildCommand {

    private @NotNull final BuildMetaStore buildMetaStore;
    private @NotNull final BuildSchematicStore buildSchematicStore;
    private @NotNull final BuildFormatCache buildFormatCache;
    private static final Logger logger = LoggerFactory.getLogger(BuildCommand.class);
    private static final Executor uploadExecutor = Executors.newSingleThreadExecutor();

    public BuildCommand(@NotNull BuildMetaStore buildMetaStore, @NotNull BuildSchematicStore buildSchematicStore, @NotNull BuildFormatCache buildFormatCache) {
        this.buildMetaStore = buildMetaStore;
        this.buildSchematicStore = buildSchematicStore;
        this.buildFormatCache = buildFormatCache;
    }

    @Execute(name = "list")
    public void list(@Context Player player) {
        @Nullable List<String> builds;
        try {
            builds = buildMetaStore.getBuilds();
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            logger.info("A database exception occurred", e);
            return;
        }
        if (builds.isEmpty()) {
            player.sendMessage(Component.text("No builds found").color(NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Found " + builds.size() + " builds").color(NamedTextColor.GREEN));
        for (String build : builds) {
            player.sendMessage(Component.text(" - " + build).color(NamedTextColor.YELLOW));
        }
    }

    @Async
    @Execute(name = "create")
    public void create(@Context Player player, @Arg String name, @Join String description) {

        name = name.toLowerCase();

        @Nullable BuildMeta meta;
        try {
            meta = buildMetaStore.getByName(name);
        } catch (Exception e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            logger.info("A database exception occurred", e);
            return;
        }
        if (meta != null) {
            player.sendMessage(Component.text("Build with name " + name + " already exists").color(NamedTextColor.RED));
            return;
        }

        String descriptionJoined = String.join(" ", description);
        try {
            buildMetaStore.create(name, descriptionJoined);
            player.sendMessage(Component.text("Build " + name + " created").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to create build: " + e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Async
    @Execute(name = "save")
    public void save(@Context Player player, @Arg("name") String originalName, @Flag("-f") boolean force, @Varargs BuildType... buildType) throws IOException {
        String name = originalName.toLowerCase();
        @Nullable BuildMeta meta;
        try {
            meta = buildMetaStore.getByName(name);
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            logger.info("A database exception occurred", e);
            return;
        }
        if (meta == null) {
            player.sendMessage(Component.text("Build with name " + name + " does not exist").color(NamedTextColor.RED));
            return;
        }

        List<String> formatNames = Stream.of(buildType).map(type -> type.getBuildType().toLowerCase(Locale.ROOT)).toList();
        if (!force) {
            List<String> missing = new ArrayList<>();

            for (BuildFormatChecksum format : meta.formats()) {
                if (!formatNames.contains(format.name())) {
                    missing.add(format.name());
                }
                if (!missing.isEmpty()) {
                    player.sendMessage(Component.text("The following formats are missing: (To override this use -f)").color(NamedTextColor.RED));
                    player.sendMessage(Component.text(String.join(", ", missing)).color(NamedTextColor.RED));
                    return;
                }
            }
        }

        BukkitPlayer adaptPlayer = BukkitAdapter.adapt(player);
        ClipboardHolder clipboardHolder = adaptPlayer.getSession().getExistingClipboard();
        if (clipboardHolder == null) {
            player.sendMessage(Component.text("You need to copy a build to your clipboard.").color(NamedTextColor.RED));
            return;
        }

        Clipboard clipboard = clipboardHolder.getClipboards().getFirst();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipStream = new GZIPOutputStream(outputStream);
        SpongeSchematicV3Writer writer = new SpongeSchematicV3Writer(new DataOutputStream(gzipStream));
        try {
            writer.write(clipboard);
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to save clipboard").color(NamedTextColor.RED));
            return;
        }
        gzipStream.close();
        byte[] data = outputStream.toByteArray();

        player.sendMessage(Component.text("Scanning markers..."));
        List<Marker> markers = MarkerUtils.scan(clipboard);
//        MarkerUtils.removeMarkerBlocks(clipboard, markers);

        List<BuildFormatChecksum> checksums = testBuildRequirements(player, formatNames, markers);
        if (checksums == null) return;

        player.sendMessage(Component.text("Uploading build file..."));

        try {
            buildSchematicStore.save(new BuildSchematic(name, data));
            player.sendMessage(Component.text("Updating database...").color(NamedTextColor.GREEN));
            buildMetaStore.update(name, checksums);
            player.sendMessage(Component.text("Build " + name + " saved with formats: " + formatNames).color(NamedTextColor.GREEN));
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            logger.info("A database exception occurred", e);
        }

    }

    private @Nullable List<BuildFormatChecksum> testBuildRequirements(Player player, List<String> formatNames, List<Marker> markers) {
        List<BuildFormatChecksum> checksums = new ArrayList<>();

        for (String formatName : formatNames) {
            try {
                List<RequirementData> requirements = buildFormatCache.getRequirements(formatName);
                BuildFormatManager.testRequirements(markers, requirements);
                BuildFormatChecksum checksum = new BuildFormatChecksum(formatName, BuildFormatManager.generateChecksum(requirements));
                checksums.add(checksum);
            } catch (BuildFormatException e) {
                player.sendMessage(Component.text(e.getMessage()).color(NamedTextColor.RED));
                return null;
            }
        }
        return checksums;
    }

}
