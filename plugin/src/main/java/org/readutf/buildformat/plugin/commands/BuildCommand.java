package org.readutf.buildformat.plugin.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.flag.Flag;
import dev.rollczi.litecommands.annotations.join.Join;
import dev.rollczi.litecommands.annotations.varargs.Varargs;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.enginehub.linbus.tree.LinCompoundTag;
import org.enginehub.linbus.tree.LinListTag;
import org.enginehub.linbus.tree.LinStringTag;
import org.enginehub.linbus.tree.LinTagType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.common.exception.BuildFormatException;
import org.readutf.buildformat.common.format.BuildFormatChecksum;
import org.readutf.buildformat.common.format.BuildFormatManager;
import org.readutf.buildformat.common.format.requirements.RequirementData;
import org.readutf.buildformat.common.markers.Marker;
import org.readutf.buildformat.common.meta.BuildMeta;
import org.readutf.buildformat.common.meta.BuildMetaStore;
import org.readutf.buildformat.plugin.formats.BuildFormatCache;
import org.readutf.buildformat.plugin.marker.MarkerScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "build")
public class BuildCommand {

    private @NotNull final BuildMetaStore buildMetaStore;
    private @NotNull final BuildFormatCache buildFormatCache;
    private static final Logger logger = LoggerFactory.getLogger(BuildCommand.class);

    public BuildCommand(@NotNull BuildMetaStore buildMetaStore, @NotNull BuildFormatCache buildFormatCache) {
        this.buildMetaStore = buildMetaStore;
        this.buildFormatCache = buildFormatCache;
    }

    @Execute(name = "create")
    public void create(@Context Player player, @Arg String name, @Varargs String... description) {

        @Nullable BuildMeta meta;
        try {
            meta = buildMetaStore.getByName(name);
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
            buildMetaStore.create(name, descriptionJoined);
            player.sendMessage(Component.text("Build " + name + " created").color(NamedTextColor.GREEN));
        } catch (Exception e) {
            player.sendMessage(Component.text("Failed to create build: " + e.getMessage()).color(NamedTextColor.RED));
        }
    }

    @Execute(name = "save")
    public void save(@Context Player player, @Arg String name, @Flag("-f") boolean force, @Join String formats) {
        @Nullable BuildMeta meta = null;
        try {
            meta = buildMetaStore.getByName(name);
        } catch (BuildFormatException e) {
            player.sendMessage(Component.text("A database exception occurred.").color(NamedTextColor.RED));
            return;
        }
        if (meta == null) {
            player.sendMessage(Component.text("Build with name " + name + " does not exist").color(NamedTextColor.RED));
            return;
        }

        List<String> formatNames = Stream.of(formats).map(String::toLowerCase).toList();
        if (!force) {
            for (BuildFormatChecksum format : meta.formats()) {
                if (!formatNames.contains(format.name())) {
                    player.sendMessage(Component.text("A previously saved format " + format.name() + " is not in the list of formats to save, to override this use -f").color(NamedTextColor.RED));
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

        List<Marker> markers = MarkerScanner.scan(clipboardHolder.getClipboards().getFirst());

        logger.info("Markers: {}", markers);

        List<BuildFormatChecksum> checksums = getBuildFormatChecksums(player, formatNames, markers);
        if (checksums == null) return;

        try {
            buildMetaStore.setFormats(name, checksums);
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
