package org.readutf.buildformat.fakes;

import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static net.minecraft.world.entity.Display.TextDisplay.DATA_BACKGROUND_COLOR_ID;

public class FakeTextDisplay implements FakeEntity {

    public Location location;
    public Set<Player> viewerPlayers = new HashSet<>();
    public TextDisplay entity;

    public FakeTextDisplay(@NotNull Location location) {
        this.location = location;
        this.entity = new TextDisplay(EntityType.TEXT_DISPLAY, Extensions.getMinecraftLevel(location.getWorld()));
        Extensions.setLocation(entity, location);
        entity.setViewRange(99999f);
    }

    public void setText(Component component) {
        entity.setText(Extensions.toVanilla(component));
        sendMetadata();
    }

    public void setBillboard(Billboard billboard) {
        entity.setBillboardConstraints(Extensions.toVanilla(billboard));
        sendMetadata();
    }

    public void setBackground(int background) {
        entity.getEntityData().set(DATA_BACKGROUND_COLOR_ID, background);
        sendMetadata();
    }

    public void setShadow(boolean shadow) {
        TagValueOutput tag = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        entity.save(tag);
        tag.putBoolean("shadow", shadow);
        entity.load(TagValueInput.createGlobal(ProblemReporter.DISCARDING, tag.buildResult()));
        sendMetadata();
    }

    public void setSeeThrough(boolean seeThrough) {
        TagValueOutput tag = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        entity.save(tag);
        tag.putBoolean("see_through", seeThrough);
        entity.load(TagValueInput.createGlobal(ProblemReporter.DISCARDING, tag.buildResult()));
        sendMetadata();
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public Set<Player> getViewerPlayers() {
        return viewerPlayers;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void addViewer(Player player) {
        viewerPlayers.add(player);
        Extensions.sendPacket(player, Extensions.getSpawnPacket(entity));
        sendMetadata(player);
        teleport(location);
    }

    @Override
    public void removeViewer(Player player) {
        Extensions.sendPacket(player, Extensions.getDespawnPacket(entity));
        viewerPlayers.remove(player);
    }

    @Override
    public void despawn() {
        for (Player p : new HashSet<>(viewerPlayers)) {
            removeViewer(p);
        }
        viewerPlayers.clear();
    }

    @Override
    public void teleport(Location location) {
        this.location = location;
        Extensions.setLocation(entity, location);
        for (Player p : viewerPlayers) {
            Extensions.sendPacket(p, new ClientboundTeleportEntityPacket(entity.getId(), PositionMoveRotation.of(entity), Set.of(), entity.onGround));
        }
    }

    @Override
    public void setGlowing(boolean glowing) {
        entity.setGlowingTag(glowing);
        sendMetadata();
    }

    public void setGlowColor(@NotNull Color color) {
        entity.setGlowColorOverride(color.asRGB());
        sendMetadata();
    }

    @Contract(pure = true)
    private @Nullable Component toMiniMessage(String text) {
        // TODO: Implement this based on your Kotlin extension.
        return null;
    }
}