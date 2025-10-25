package org.readutf.buildformat.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.fakes.FakeItemDisplay;

import java.util.*;

public class PositionTool implements Listener {

    private static Map<UUID, PositionHighlight> positions = new HashMap<>();

    public static @NotNull UUID giveTool(@NotNull Player player, @NotNull String name) {
        ItemStack tool = ItemStack.of(Material.WHITE_BANNER);
        UUID toolIdentifier = UUID.randomUUID();

        tool.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData()
                        .addString(toolIdentifier.toString())
                        .build());

        player.getInventory().addItem(tool);

        return toolIdentifier;
    }

    @EventHandler
    public void onBlockPlace(@NotNull BlockPlaceEvent e) {
        ItemStack itemInHand = e.getItemInHand();
        UUID toolId = getToolId(itemInHand);
        if (toolId == null) return;

        e.setCancelled(true);

        Location location = e.getBlockPlaced().getLocation();

        PositionHighlight positionHighlight = positions.get(toolId);
        if (positionHighlight == null) {
            positionHighlight = new PositionHighlight(toolId, location, getDisplay(e.getPlayer(), location.add(0.5, 0.5, 0.5)));
        }

        positionHighlight.setPosition(location.add(0.5, 0.5, 0.5));
        positionHighlight.refresh();
        positions.put(toolId, positionHighlight);
    }

    private static @NotNull FakeItemDisplay getDisplay(Player player, Location location) {
        FakeItemDisplay fakeItemDisplay = new FakeItemDisplay(location);
        fakeItemDisplay.setItem(ItemStack.of(Material.DIAMOND_BLOCK));
        fakeItemDisplay.addViewer(player);

        return fakeItemDisplay;
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

    public static final class PositionHighlight {
        private final UUID toolId;
        private final @NotNull FakeItemDisplay display;
        private Location position;

        public PositionHighlight(UUID toolId, Location position, @NotNull FakeItemDisplay display) {
            this.toolId = toolId;
            this.position = position;
            this.display = display;
        }

        public void refresh() {
            display.teleport(position);
        }

        public void setPosition(Location position) {
            this.position = position;
        }
    }
}
