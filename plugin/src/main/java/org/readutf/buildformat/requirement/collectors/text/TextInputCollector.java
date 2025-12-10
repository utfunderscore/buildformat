package org.readutf.buildformat.requirement.collectors.text;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.requirement.RequirementCollector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextInputCollector<T> implements RequirementCollector<T> {

    private static Map<UUID, List<TextInputCollector<?>>> activeCollectors = new HashMap<>();

    private final String name;
    private final int step;
    private final Convertor<T> convertor;
    private final CompletableFuture<T> future;
    private final AtomicBoolean completed;
    private final List<UUID> waitingPlayers;

    public TextInputCollector(String name, int step, Convertor<T> convertor) {
        this.name = name;
        this.step = step;
        this.convertor = convertor;
        this.future = new CompletableFuture<>();
        this.completed = new AtomicBoolean(false);
        this.waitingPlayers = new ArrayList<>();
    }

    @Override
    public void start(@NotNull Player player) {
        waitingPlayers.add(player.getUniqueId());
        List<TextInputCollector<?>> collectors = activeCollectors.getOrDefault(player.getUniqueId(), new ArrayList<>());
        collectors.add(this);
        activeCollectors.put(player.getUniqueId(), collectors);
        player.sendMessage(Lang.getTextInput(name, step));
    }

    @Override
    public void cleanup(Player player) {}

    public void complete(Object object) {
        future.complete((T) object);
        this.completed.set(true);
    }

    @Override
    public T awaitBlocking() {
        return future.join();
    }

    public static class ChatListener implements Listener {

        private static PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        @EventHandler
        public void onChat(@NotNull AsyncChatEvent e) {

            Component message = e.message();
            String rawText = serializer.serialize(message);

            for (TextInputCollector<?> activeCollector : activeCollectors.getOrDefault(e.getPlayer().getUniqueId(), new ArrayList<>())) {
                if(activeCollector.completed.get()) continue;
                if (!activeCollector.waitingPlayers.contains(e.getPlayer().getUniqueId())) continue;

                e.setCancelled(true);

                try {
                    Object convert = activeCollector.convertor.convert(rawText);
                    activeCollector.complete(convert);
                } catch (Exception ex) {
                    e.getPlayer().sendMessage("Invalid input: " + ex.getMessage());
                }
            }
        }
    }
}
