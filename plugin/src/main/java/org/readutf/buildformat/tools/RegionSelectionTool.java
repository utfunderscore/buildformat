package org.readutf.buildformat.tools;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.readutf.buildformat.fakes.FakeItemDisplay;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RegionSelectionTool implements Listener {

    private final Map<UUID, Location> leftSelection;
    private final Map<UUID, Location> rightSelection;

    private final Map<UUID, FakeItemDisplay> regionDisplays;
    private final Map<UUID, FakeItemDisplay> regionDisplaysInverted;

    public RegionSelectionTool() {
        this.leftSelection = new HashMap<>();
        this.rightSelection = new HashMap<>();
        this.regionDisplays = new HashMap<>();
        this.regionDisplaysInverted = new HashMap<>();
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent e) {

        ItemStack item = e.getItem();
        if(item == null || item.getType() != Material.BLAZE_ROD) return;

        Block clickedBlock = e.getClickedBlock();
        if (clickedBlock == null) return;

        Location location = clickedBlock.getLocation();
        e.setCancelled(true);

        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();

        if (e.getAction().isRightClick()) {
            rightSelection.put(playerId, location);
        } else if (e.getAction().isLeftClick()) {
            leftSelection.put(playerId, location);
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
            fakeItemDisplay = spawnOutline(location, playerId, player, false);
            fakeItemDisplayInv = spawnOutline(location, playerId, player, true);

            regionDisplays.put(playerId, fakeItemDisplay);
            regionDisplaysInverted.put(playerId, fakeItemDisplayInv);
        }

        Location[] minMax = toMinMax(left, right);
        Location min = minMax[0];
        Location max = minMax[1];

        Vector3f translation = new Vector3f(
                (min.getBlockX() + max.getBlockX()) / 2f,
                (min.getBlockY() + max.getBlockY()) / 2f,
                (min.getBlockZ() + max.getBlockZ()) / 2f);

        Vector3f scale = new Vector3f(
                (max.getBlockX() - min.getBlockX() + 1),
                (max.getBlockY() - min.getBlockY() + 1),
                (max.getBlockZ() - min.getBlockZ()) + 1);

        fakeItemDisplay.setTransformation(
                new Transformation(new Vector3f(scale).mul(0.5f), new AxisAngle4f(), scale.mul(1.00002f), new AxisAngle4f()));

        fakeItemDisplay.teleport(min);

        fakeItemDisplayInv.setTransformation(
                new Transformation(new Vector3f(scale).mul(0.5f), new AxisAngle4f(), scale.mul(-1.00001f), new AxisAngle4f()));

        fakeItemDisplayInv.teleport(min);

        //        FakeItemDisplay test = new FakeItemDisplay(min);
        //        test.addViewer(player);
        //        test.setItem(new ItemStack(Material.DIAMOND_BLOCK));
    }

    private @NotNull FakeItemDisplay spawnOutline(Location location, UUID playerId, Player player, boolean flipped) {
        FakeItemDisplay fakeItemDisplay;
        fakeItemDisplay = new FakeItemDisplay(location);
        fakeItemDisplay.setItem(ItemStack.of(Material.GRAY_STAINED_GLASS));

        fakeItemDisplay.setBrightness(new Display.Brightness(15, 15));

        if(!flipped) {
            fakeItemDisplay.setGlowing(true);
            fakeItemDisplay.setGlowColor(Color.WHITE);
        }

        fakeItemDisplay.addViewer(player);
        return fakeItemDisplay;
    }

    @EventHandler
    public void disconnect(@NotNull PlayerQuitEvent e) {
        UUID uniqueId = e.getPlayer().getUniqueId();
        this.leftSelection.remove(uniqueId);
        this.rightSelection.remove(uniqueId);
        this.regionDisplays.remove(uniqueId);
        this.regionDisplaysInverted.remove(uniqueId);
    }

    public static Location @NotNull [] toMinMax(@NotNull Location pos1, @NotNull Location pos2) {
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
