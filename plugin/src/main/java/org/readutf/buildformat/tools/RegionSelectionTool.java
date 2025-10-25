package org.readutf.buildformat.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.fakes.FakeItemDisplay;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.*;

import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;

public class RegionSelectionTool implements Listener {

    private static final Map<UUID, Selection> tools = new HashMap<>();

    public static @NotNull UUID givePlayerTool(@NotNull Player player, String name) {
        ItemStack tool = ItemStack.of(Material.BLAZE_ROD);
        UUID toolIdentifier = UUID.randomUUID();

        tools.put(toolIdentifier, new Selection(name, toolIdentifier));

        tool.editMeta(itemMeta -> {
            itemMeta.itemName(Component.text("Region Selector").color(LIGHT_PURPLE));
            itemMeta.lore(List.of(
                    Component.text("Used to define a cuboid region").color(WHITE),
                    Component.text(""),
                    Component.text("Right-Click ")
                            .color(LIGHT_PURPLE)
                            .append(Component.text("to set the first position").color(GRAY)),
                    Component.text("Left-Click ")
                            .color(LIGHT_PURPLE)
                            .append(Component.text("to set the second position").color(GRAY)),
                    Component.text("Drop ")
                            .color(LIGHT_PURPLE)
                            .append(Component.text("to clear your selection").color(GRAY))));
        });
        tool.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData()
                        .addString(toolIdentifier.toString())
                        .build());


        player.give(tool);
        return toolIdentifier;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent e) {
        Block block = e.getClickedBlock();
        if (block == null) return;

        Location location = block.getLocation();
        ItemStack item = e.getItem();
        UUID toolId = getToolId(item);
        if (toolId == null) return;
        Selection selection = getSelection(toolId);
        if (selection == null) return;
        e.setCancelled(true);

        Player player = e.getPlayer();
        if (!selection.isComplete()) {
            selection.setPosition2(location);
            selection.setPosition1(location);

            if (e.getAction().isRightClick()) {
                player.sendMessage(Lang.getPositionSet("first position", location));
            } else if (e.getAction().isLeftClick()) {
                player.sendMessage(Lang.getPositionSet("second position", location));
            }

            selection.updateDisplays(player);
            return;
        }

        if (e.getAction().isRightClick()) {
            player.sendMessage(Lang.getPositionSet("first position", location));
            selection.setPosition2(location);
        } else if (e.getAction().isLeftClick()) {
            player.sendMessage(Lang.getPositionSet("second position", location));
            selection.setPosition1(location);
        }

        selection.updateDisplays(player);
    }

    private static @Nullable UUID getToolId(@Nullable ItemStack item) {
        if (item == null) return null;
        CustomModelData data = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null) return null;
        List<String> strings = data.strings();
        if (strings.isEmpty()) return null;
        UUID toolId;
        try {
            toolId = UUID.fromString(strings.getFirst());
        } catch (Exception ex) {
            return null;
        }
        return toolId;
    }

    public static @Nullable Selection getSelection(UUID toolId) {
        return tools.get(toolId);
    }

    public static final class Selection {
        private final String name;
        private final UUID serverId;
        private @Nullable Location position1;
        private @Nullable Location position2;
        private @Nullable FakeItemDisplay normalDisplay;
        private @Nullable FakeItemDisplay invertedDisplay;

        public Selection(String name, UUID serverId) {
            this.name = name;
            this.serverId = serverId;
        }

        public boolean isComplete() {
            return position1 != null && position2 != null;
        }

        public String getName() {
            return name;
        }

        public @Nullable Location getPosition1() {
            return position1;
        }

        public void setPosition1(@Nullable Location position1) {
            this.position1 = position1;
        }

        public @Nullable Location getPosition2() {
            return position2;
        }

        public void setPosition2(@Nullable Location position2) {
            this.position2 = position2;
        }

        public @Nullable FakeItemDisplay getNormalDisplay() {
            return normalDisplay;
        }

        public void setNormalDisplay(@Nullable FakeItemDisplay normalDisplay) {
            this.normalDisplay = normalDisplay;
        }

        public @Nullable FakeItemDisplay getInvertedDisplay() {
            return invertedDisplay;
        }

        public void setInvertedDisplay(@Nullable FakeItemDisplay invertedDisplay) {
            this.invertedDisplay = invertedDisplay;
        }

        public void updateDisplays(@NotNull Player player) {
            if (position1 == null || position2 == null) return;

            Location[] minMax = toMinMax(position1, position2);
            Location min = minMax[0];
            Location max = minMax[1];

            if (normalDisplay == null) {
                player.sendMessage("Spawned at " + min);

                FakeItemDisplay normalDisplay1 = new FakeItemDisplay(min);
                normalDisplay1.addViewer(player);
                normalDisplay1.setGlowing(true);
                normalDisplay1.setItem(ItemStack.of(Material.GRAY_STAINED_GLASS));

                setNormalDisplay(normalDisplay1);
            }
            if (invertedDisplay == null) {
                FakeItemDisplay invertedDisplay1 = new FakeItemDisplay(min);
                invertedDisplay1.addViewer(player);
                invertedDisplay1.setItem(ItemStack.of(Material.GRAY_STAINED_GLASS));

                setInvertedDisplay(invertedDisplay1);
            }

            Vector3f scale = new Vector3f(
                    (max.getBlockX() - min.getBlockX() + 1),
                    (max.getBlockY() - min.getBlockY() + 1),
                    (max.getBlockZ() - min.getBlockZ()) + 1);

            normalDisplay.setTransformation(new Transformation(
                    new Vector3f(scale).mul(0.5f),
                    new AxisAngle4f(),
                    scale.add(0.15f, 0.15f, 0.15f),
                    new AxisAngle4f()));

            normalDisplay.teleport(min);

            invertedDisplay.setTransformation(new Transformation(
                    new Vector3f(scale).mul(0.5f), new AxisAngle4f(), scale.mul(-1f), new AxisAngle4f()));

            invertedDisplay.teleport(min);

            //            invertedDisplay.setTransformation(new Transformation(
            //                    new Vector3f(),
            //                    new AxisAngle4f(),
            //                    new Vector3f(-(float) size.getX(), -(float) size.getY(), -(float) size.getZ()),
            //                    new AxisAngle4f()));
        }

        public @Nullable Cuboid getRegion() {
            if (position1 == null || position2 == null) {
                return null;
            }
            return new Cuboid(
                    new Position(
                            Math.min(position1.getX(), position2.getX()),
                            Math.min(position1.getY(), position2.getY()),
                            Math.min(position1.getZ(), position2.getZ())),
                    new Position(
                            Math.max(position1.getX(), position2.getX()),
                            Math.max(position1.getY(), position2.getY()),
                            Math.max(position1.getZ(), position2.getZ()))
            );
        }

        private static Location @NotNull [] toMinMax(@NotNull Location pos1, @NotNull Location pos2) {
            double minX = Math.min(pos1.getX(), pos2.getX());
            double minY = Math.min(pos1.getY(), pos2.getY());
            double minZ = Math.min(pos1.getZ(), pos2.getZ());
            double maxX = Math.max(pos1.getX(), pos2.getX());
            double maxY = Math.max(pos1.getY(), pos2.getY());
            double maxZ = Math.max(pos1.getZ(), pos2.getZ());

            // It's important that both Locations are in the same world!
            Location min = new Location(pos1.getWorld(), minX, minY, minZ);
            Location max = new Location(pos1.getWorld(), maxX, maxY, maxZ);

            return new Location[] {min, max};
        }
    }
}
