package org.readutf.buildformat.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.fakes.FakeItemDisplay;
import org.readutf.buildformat.fakes.FakeTextDisplay;
import org.readutf.buildformat.types.Position;

import java.util.*;

public class PositionTool implements Listener {

    private static Map<UUID, PositionHighlight> positions = new HashMap<>();
    private static Map<UUID, String> toolNames = new HashMap<>();

    public static @NotNull Tool getTool(@NotNull String name) {
        ItemStack tool = ItemStack.of(Material.PLAYER_HEAD);
        UUID toolIdentifier = UUID.randomUUID();

        tool.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData()
                        .addString(toolIdentifier.toString())
                        .build());

        toolNames.put(toolIdentifier, name);

        return new Tool(toolIdentifier, tool);
    }

    public static void clearTool(@Nullable UUID toolId) {
        PositionHighlight remove = positions.remove(toolId);
        if(remove == null) return;

        for (Player viewerPlayer : remove.getTextDisplay().getViewerPlayers()) {
            remove.getTextDisplay().removeViewer(viewerPlayer);
        }
        for (Player viewerPlayer : remove.getDisplay().getViewerPlayers()) {
            remove.getDisplay().removeViewer(viewerPlayer);
        }

    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent e) {
        ItemStack itemInHand = e.getItemInHand();
        UUID toolId = getToolId(itemInHand);
        if (toolId == null) return;

        e.setCancelled(true);

        Location location = e.getBlockPlaced().getLocation();
        Player player = e.getPlayer();

        Rotatable rotatable = (Rotatable) e.getBlockPlaced().getBlockData();
        float yawDegrees = getYawDegrees(rotatable);

        location.setYaw(yawDegrees);

        String text = toolNames.get(toolId);

        PositionHighlight positionHighlight = positions.get(toolId);
        if (positionHighlight == null) {
            positionHighlight = new PositionHighlight(
                    getPositionDisplay(player, location.add(0.5, 0.5, 0.5)),
                    getPositionText(player, location.add(0, 0.75, 0), text));
        } else {
            positionHighlight.getDisplay().teleport(location.add(0.5, 0.5, 0.5));
            positionHighlight.getTextDisplay().teleport(location.add(0, 0.75, 0));
        }

        positions.put(toolId, positionHighlight);
    }

    public static @Nullable Position getPosition(UUID toolId) {
        PositionHighlight positionHighlight = positions.get(toolId);
        if (positionHighlight == null) return null;
        Location location = positionHighlight.getDisplay().getLocation();

        return new Position(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    private static @NotNull FakeItemDisplay getPositionDisplay(Player player, Location location) {
        FakeItemDisplay fakeItemDisplay = new FakeItemDisplay(location);
        fakeItemDisplay.setItem(ItemStack.of(Material.PLAYER_HEAD));
        fakeItemDisplay.addViewer(player);
        fakeItemDisplay.setGlowing(true);

        fakeItemDisplay.setRotation(location.getYaw(), location.getPitch());

        return fakeItemDisplay;
    }

    private static @NotNull FakeTextDisplay getPositionText(Player player, Location location, String text) {
        FakeTextDisplay fakeTextDisplay = new FakeTextDisplay(location);
        fakeTextDisplay.setText(Component.text(text));
        fakeTextDisplay.addViewer(player);
        fakeTextDisplay.setBillboard(Display.Billboard.CENTER);

        return fakeTextDisplay;
    }

    private static @Nullable UUID getToolId(@NotNull ItemStack item) {
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

    private float getYawDegrees(@NotNull Rotatable rotatable) {
        // Horizontal vector components
        BlockFace rotation = rotatable.getRotation();

        float hx = rotation.getModX();
        float hz = rotation.getModZ();

        // No horizontal direction (e.g., UP or DOWN)
        if (Math.abs(hx) < 1e-9 && Math.abs(hz) < 1e-9) {
            return 0.0f;
        }

        // yaw = degrees(atan2(-x, z))
        float yaw = (float) Math.toDegrees(Math.atan2(-hx, hz));

        // Normalize to (-180, 180]
        //        yaw = normalizeYaw180(yaw);
        yaw = yaw % 360.0f;
        if (yaw <= -180.0) yaw += 360.0;
        else if (yaw > 180.0) yaw -= 360.0;
        return yaw;
    }

    private static final class PositionHighlight {
        private final @NotNull FakeItemDisplay display;
        private final @NotNull FakeTextDisplay textDisplay;

        public PositionHighlight(@NotNull FakeItemDisplay display, @NotNull FakeTextDisplay textDisplay) {
            this.display = display;
            this.textDisplay = textDisplay;
        }

        public @NotNull FakeItemDisplay getDisplay() {
            return display;
        }

        public @NotNull FakeTextDisplay getTextDisplay() {
            return textDisplay;
        }
    }
}
