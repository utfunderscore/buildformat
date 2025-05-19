package org.readutf.buildformat.plugin.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.extent.clipboard.io.sponge.SpongeSchematicV3Writer;
import com.sk89q.worldedit.session.ClipboardHolder;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.execute.ExecuteDefault;
import dev.rollczi.litecommands.annotations.flag.Flag;
import dev.rollczi.litecommands.annotations.join.Join;
import dev.rollczi.litecommands.annotations.varargs.Varargs;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
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
import org.readutf.buildformat.common.meta.BuildStore;
import org.readutf.buildformat.common.schematic.BuildSchematic;
import org.readutf.buildformat.common.schematic.SchematicStore;
import org.readutf.buildformat.plugin.commands.types.BuildType;
import org.readutf.buildformat.plugin.formats.BuildFormatCache;
import org.readutf.buildformat.plugin.marker.MarkerScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "build")
public class BuildCommand {

    private @NotNull final BuildStore buildStore;
    private @NotNull final SchematicStore schematicStore;
    private @NotNull final BuildFormatCache buildFormatCache;
    private static final Logger logger = LoggerFactory.getLogger(BuildCommand.class);

    public BuildCommand(@NotNull BuildStore buildStore, @NotNull SchematicStore schematicStore, @NotNull BuildFormatCache buildFormatCache) {
        this.buildStore = buildStore;
        this.schematicStore = schematicStore;
        this.buildFormatCache = buildFormatCache;
    }

    @Execute(name = "list")
    public void list(@Context Player player) {
        @Nullable List<String> builds;
        try {
            builds = buildStore.getBuilds();
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
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

        @Nullable BuildMeta meta;
        try {
            meta = buildStore.getByName(name);
        } catch (Exception e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            return;
        }
        if (meta != null) {
            player.sendMessage(Component.text("Build with name " + name + " already exists").color(NamedTextColor.RED));
            return;
        }



        String descriptionJoined = String.join(" ", description);
        try {
            buildStore.create(name, descriptionJoined);
            player.sendMessage(Component.text("Build " + name + " created").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to create build: " + e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Async
    @Execute(name = "save")
    public void save(@Context Player player, @Arg String name, @Flag("-f") boolean force, @Varargs BuildType... buildType) {
        @Nullable BuildMeta meta = null;
        try {
            meta = buildStore.getByName(name);
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
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
                if(!missing.isEmpty()) {
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
        SpongeSchematicV3Writer writer = new SpongeSchematicV3Writer(new DataOutputStream(outputStream));
        try {
            writer.write(clipboard);
        } catch (IOException e) {
            player.sendMessage(Component.text("Failed to save clipboard").color(NamedTextColor.RED));
            return;
        }
        byte[] data = outputStream.toByteArray();


        List<Marker> markers = MarkerScanner.scan(clipboard);

        List<BuildFormatChecksum> checksums = getBuildFormatChecksums(player, formatNames, markers);
        if (checksums == null) return;

        try {
            schematicStore.save(new BuildSchematic(name, data));
            buildStore.update(name, checksums);

            player.sendMessage(Component.text("Build " + name + " saved with formats: " + formatNames).color(NamedTextColor.GREEN));
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
        }
    }

    private @Nullable List<BuildFormatChecksum> getBuildFormatChecksums(Player player, List<String> formatNames, List<Marker> markers) {
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
