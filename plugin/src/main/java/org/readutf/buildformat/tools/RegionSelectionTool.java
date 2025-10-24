package org.readutf.buildformat.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.fakes.FakeItemDisplay;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.kyori.adventure.text.format.NamedTextColor.*;

public class RegionSelectionTool implements Listener {

    public static final ItemStack tool = ItemStack.of(Material.BLAZE_ROD);
    private static final String toolIdentifier = UUID.randomUUID().toString();

    static {
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
                CustomModelData.customModelData().addString(toolIdentifier).build());
    }

    private static final Map<UUID, Location> leftSelection = new HashMap<>();
    private static final Map<UUID, Location> rightSelection = new HashMap<>();

    private final Map<UUID, FakeItemDisplay> regionDisplays;
    private final Map<UUID, FakeItemDisplay> regionDisplaysInverted;

    public RegionSelectionTool() {
        this.regionDisplays = new HashMap<>();
        this.regionDisplaysInverted = new HashMap<>();
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent e) {

        ItemStack item = e.getItem();
        if (!isTool(item)) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        Location location = clickedBlock.getLocation();
        e.setCancelled(true);

        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        if (e.getAction().isRightClick()) {
            rightSelection.put(playerId, location);
            String text = String.format(
                    "First position as been set to %s, %s, %s",
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());

            player.sendMessage(Component.text(text).color(GRAY));
        } else if (e.getAction().isLeftClick()) {
            leftSelection.put(playerId, location);
            String text = String.format(
                    "Second position as been set to %s, %s, %s",
                    location.getBlockX(), location.getBlockY(), location.getBlockZ());

            player.sendMessage(Component.text(text).color(GRAY));
        }

        Location left = leftSelection.get(playerId);
        Location right = rightSelection.get(playerId);

        FakeItemDisplay fakeItemDisplay = regionDisplays.get(playerId);
        FakeItemDisplay fakeItemDisplayInv = regionDisplaysInverted.get(playerId);

        if (left == null || right == null || left.getWorld() != right.getWorld()) {
            if (fakeItemDisplay != null) {
                fakeItemDisplay.removeViewer(player);
            }
            return;
        }

        if (fakeItemDisplay == null) {
            fakeItemDisplay = spawnOutline(location, player, false);
            fakeItemDisplayInv = spawnOutline(location, player, true);

            regionDisplays.put(playerId, fakeItemDisplay);
            regionDisplaysInverted.put(playerId, fakeItemDisplayInv);
        }

        Location[] minMax = toMinMax(left, right);
        Location min = minMax[0];
        Location max = minMax[1];

        Vector3f scale = new Vector3f(
                (max.getBlockX() - min.getBlockX() + 1),
                (max.getBlockY() - min.getBlockY() + 1),
                (max.getBlockZ() - min.getBlockZ()) + 1);

        fakeItemDisplay.setTransformation(new Transformation(
                new Vector3f(scale).mul(0.5f), new AxisAngle4f(), scale.add(0.15f, 0.15f, 0.15f), new AxisAngle4f()));

        fakeItemDisplay.teleport(min);

        fakeItemDisplayInv.setTransformation(new Transformation(
                new Vector3f(scale).mul(0.5f), new AxisAngle4f(), scale.mul(-1f), new AxisAngle4f()));

        fakeItemDisplayInv.teleport(min);
    }

    @EventHandler
    public void onDrop(@NotNull PlayerDropItemEvent e) {
        Item itemDrop = e.getItemDrop();
        ItemStack itemStack = itemDrop.getItemStack();
        if (isTool(itemStack)) {
            e.setCancelled(true);
            UUID uniqueId = e.getPlayer().getUniqueId();
            leftSelection.remove(uniqueId);
            rightSelection.remove(uniqueId);
            FakeItemDisplay normal = this.regionDisplays.remove(uniqueId);
            FakeItemDisplay inverted = this.regionDisplaysInverted.remove(uniqueId);

            if (normal != null) {
                normal.removeViewer(e.getPlayer());
            }
            if (inverted != null) {
                inverted.removeViewer(e.getPlayer());
            }
        }
    }

    private @NotNull FakeItemDisplay spawnOutline(Location location, Player player, boolean flipped) {
        FakeItemDisplay fakeItemDisplay;
        fakeItemDisplay = new FakeItemDisplay(location);
        fakeItemDisplay.setItem(ItemStack.of(Material.GRAY_STAINED_GLASS));

        fakeItemDisplay.setBrightness(new Display.Brightness(15, 15));

        if (!flipped) {
            fakeItemDisplay.setGlowing(true);
            fakeItemDisplay.setGlowColor(Color.WHITE);
        }

        fakeItemDisplay.addViewer(player);
        return fakeItemDisplay;
    }

    @EventHandler
    public void disconnect(@NotNull PlayerQuitEvent e) {
        UUID uniqueId = e.getPlayer().getUniqueId();
        leftSelection.remove(uniqueId);
        rightSelection.remove(uniqueId);
        this.regionDisplays.remove(uniqueId);
        this.regionDisplaysInverted.remove(uniqueId);
    }

    public static @Nullable Cuboid getSelection(@NotNull UUID playerId) {
        Location left = leftSelection.get(playerId);
        Location right = rightSelection.get(playerId);

        if (left != null && right != null) {
            return new Cuboid(
                    new Position(left.getX(), left.getY(), left.getZ()),
                    new Position(right.getX(), right.getY(), right.getZ()));
        }

        return null;
    }

    private static boolean isTool(@Nullable ItemStack itemStack) {
        if (itemStack == null) return false;

        if (!itemStack.isSimilar(tool)) return false;
        CustomModelData data = itemStack.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null) return false;
        List<String> strings = data.strings();
        if (strings.isEmpty()) return false;
        return strings.getFirst().equalsIgnoreCase(toolIdentifier);
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
