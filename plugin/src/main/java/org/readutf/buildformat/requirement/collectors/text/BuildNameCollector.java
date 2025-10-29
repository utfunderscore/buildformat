package org.readutf.buildformat.requirement.collectors.text;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.requirement.RequirementCollector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class BuildNameCollector implements RequirementCollector<String> {

    private static List<BuildNameCollector> activeCollectors = new ArrayList<>();

    private final CompletableFuture<String> future;
    private final AtomicBoolean completed;
    private final List<UUID> waitingPlayers;

    public BuildNameCollector() {
        this.future = new CompletableFuture<>();
        this.completed = new AtomicBoolean(false);
        this.waitingPlayers = new ArrayList<>();
        activeCollectors.add(this);
    }

    @Override
    public void start(@NotNull Player player) {
        waitingPlayers.add(player.getUniqueId());
        player.sendMessage(Component.text("Enter the name for this build").color(NamedTextColor.GREEN));
    }

    @Override
    public void cleanup(Player player) {}

    public void complete(String text) {
        future.complete(text);
        this.completed.set(true);
    }

    @Override
    public String awaitBlocking() {
        return future.join();
    }

    public static class ChatListener implements Listener {

        private static PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        @EventHandler
        public void onChat(@NotNull AsyncChatEvent e) {

            Component message = e.message();
            String rawText = serializer.serialize(message);

            for (BuildNameCollector buildNameCollector : activeCollectors) {
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
