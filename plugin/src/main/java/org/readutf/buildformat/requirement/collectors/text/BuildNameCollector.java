package org.readutf.buildformat.requirement.collectors.text;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.settings.BuildSetting;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class BuildNameCollector implements RequirementCollector<String> {

    private static Map<UUID, List<BuildNameCollector>> activeCollectors = new HashMap<>();

    private final CompletableFuture<String> future;
    private final AtomicBoolean completed;
    private final List<UUID> waitingPlayers;

    public BuildNameCollector() {
        this.future = new CompletableFuture<>();
        this.completed = new AtomicBoolean(false);
        this.waitingPlayers = new ArrayList<>();
    }

    @Override
    public void start(@NotNull Player player) {
        waitingPlayers.add(player.getUniqueId());
        List<BuildNameCollector> collectors = activeCollectors.getOrDefault(player.getUniqueId(), new ArrayList<>());
        collectors.add(this);
        activeCollectors.put(player.getUniqueId(), collectors);
        player.sendMessage(Component.text("Enter the name for this build").color(NamedTextColor.GREEN));
    }

    @Override
    public void cleanup(Player player) {
        List<BuildNameCollector> collectors = activeCollectors.get(player.getUniqueId());
        collectors.remove(this);
        activeCollectors.put(player.getUniqueId(), collectors);
    }

    public void complete(String text) {
        future.complete(text);
        this.completed.set(true);
    }

    @Override
    public BuildSetting<String> awaitBlocking() {
        return new BuildSetting<>(future.join());
    }

    public static class ChatListener implements Listener {

        private static PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        @EventHandler
        public void onChat(@NotNull AsyncChatEvent e) {

            Component message = e.message();
            String rawText = serializer.serialize(message);

            for (BuildNameCollector buildNameCollector : activeCollectors.getOrDefault(e.getPlayer().getUniqueId(), new ArrayList<>())) {
                if(buildNameCollector.completed.get()) continue;
                if (!buildNameCollector.waitingPlayers.contains(e.getPlayer().getUniqueId())) continue;

                e.setCancelled(true);

                try {
                    buildNameCollector.complete(rawText);
                } catch (Exception ex) {
                    e.getPlayer().sendMessage("Invalid input: " + ex.getMessage());
                }
            }
        }
    }
}
