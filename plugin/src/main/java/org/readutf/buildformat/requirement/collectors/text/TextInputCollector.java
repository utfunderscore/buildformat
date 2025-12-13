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
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class TextInputCollector<T> implements RequirementCollector<T> {

    private static Map<UUID, TextInputCollector<?>> activeCollectors = new HashMap<>();

    private final String name;
    private final int step;
    private final Convertor<T> convertor;
    private final CompletableFuture<T> future;
    private final AtomicBoolean completed;

    public TextInputCollector(String name, int step, Convertor<T> convertor) {
        this.name = name;
        this.step = step;
        this.convertor = convertor;
        this.future = new CompletableFuture<>();
        this.completed = new AtomicBoolean(false);
    }

    @Override
    public void start(@NotNull Player player, @NotNull Cuboid bounds, @NotNull Position origin) {
        if(activeCollectors.containsKey(player.getUniqueId())) {
            throw new IllegalStateException("Player " + player.getName() + " is already in a text input collection process.");
        }

        activeCollectors.put(player.getUniqueId(), this);
        player.sendMessage(Lang.getTextInput(name, step));
    }

    @Override
    public void cleanup(Player player) {}

    public void complete(Object object) {
        future.complete((T) object);
        this.completed.set(true);
    }

    @Override
    public BuildSetting<T> awaitBlocking() {
        return new BuildSetting<>(future.join());
    }

    public static class ChatListener implements Listener {

        private static PlainTextComponentSerializer serializer = PlainTextComponentSerializer.plainText();

        @EventHandler
        public void onChat(@NotNull AsyncChatEvent e) {

            Component message = e.message();
            String rawText = serializer.serialize(message);

            TextInputCollector<?> collector = activeCollectors.get(e.getPlayer().getUniqueId());
            if (collector == null) return;
            if (collector.completed.get()) return;
            e.setCancelled(true);

            try {
                Object convert = collector.convertor.convert(rawText);
                collector.complete(convert);
                activeCollectors.remove(e.getPlayer().getUniqueId());
            } catch (Exception ex) {
                ex.printStackTrace();
                e.getPlayer().sendMessage("Invalid input: " + ex.getMessage());
            }
        }
    }
}
