package org.readutf.buildformat.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClickableManager implements Listener {
    private static final Map<UUID, Runnable> tasks = new HashMap<>();

    @Contract("_, _, _ -> param2")
    public static @NotNull ItemStack setClickAction(Player player, @NotNull ItemStack itemStack, Runnable runnable) {

        UUID taskId = UUID.randomUUID();

        itemStack.setData(
                DataComponentTypes.CUSTOM_MODEL_DATA,
                CustomModelData.customModelData().addString(taskId.toString()).build());

        tasks.put(taskId, runnable);

        return itemStack;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent e) {
        if (!e.getAction().isRightClick()) return;

        ItemStack item = e.getItem();
        if (item == null) return;

        CustomModelData data = item.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        if (data == null) return;

        List<String> strings = data.strings();
        if (!strings.isEmpty()) {
            String first = strings.getFirst();
            try {
                UUID uuid = UUID.fromString(first);

                Runnable runnable = tasks.get(uuid);
                if (runnable != null) runnable.run();

            } catch (Exception ignore) {
            }
        }
    }
}
