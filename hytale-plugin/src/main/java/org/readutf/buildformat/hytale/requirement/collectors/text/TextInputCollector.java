package org.readutf.buildformat.hytale.requirement.collectors.text;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;
import org.readutf.buildformat.hytale.utils.PlatformUtils;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TextInputCollector<T> implements RequirementCollector<T> {

    private static final Logger log = LoggerFactory.getLogger(TextInputCollector.class);
    private static Map<UUID, TextInputCollector<?>> activeCollectors = new HashMap<>();

    private final @NotNull Message message;
    private final @NotNull Convertor<T> convertor;
    private final @NotNull CompletableFuture<T> future;
    private final @NotNull AtomicBoolean completed;

    public TextInputCollector(@NotNull Message message, @NotNull Convertor<T> convertor) {
        this.message = message;
        this.convertor = convertor;
        this.future = new CompletableFuture<>();
        this.completed = new AtomicBoolean(false);
    }

    @Override
    public void start(@NotNull PlayerRef playerRef, @NotNull Cuboid bounds, @NotNull Position origin) {
        if (activeCollectors.containsKey(playerRef.getUuid())) {
            throw new IllegalStateException(
                    "Player " + playerRef.getUsername() + " is already in a text input collection process.");
        }

        activeCollectors.put(playerRef.getUuid(), this);
        playerRef.sendMessage(message);
    }

    @Override
    public void cleanup(PlayerRef player) {
    }

    public void complete(Object object) {
        future.complete((T) object);
        this.completed.set(true);
    }

    @Override
    public CompletableFuture<BuildSetting<T>> getFuture() {
        return future.thenApply(t -> new BuildSetting<>(t, new TypeReference<>() {
        }));
    }

    public static class ChatListener implements Consumer<PlayerChatEvent> {


        @Override
        public void accept(PlayerChatEvent e) {
            PlayerRef sender = e.getSender();
            UUID uuid = sender.getUuid();

            TextInputCollector<?> collector = activeCollectors.get(uuid);
            if (collector == null) return;

            e.setCancelled(true);
            if (collector.completed.get()) return;

            PlatformUtils.getWorld(sender).execute(() -> {
                try {
                    Object convert = collector.convertor.convert(e.getContent());
                    collector.complete(convert);
                    activeCollectors.remove(uuid);
                } catch (Exception ex) {
                    log.error("Failed processing user text input.", ex);
                    sender.sendMessage(
                            Message.raw("Invalid text input: " + ex.getMessage()).color(Color.RED));
                }
            });


        }
    }
}
