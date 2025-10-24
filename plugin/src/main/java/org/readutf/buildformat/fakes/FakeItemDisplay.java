package org.readutf.buildformat.fakes;

import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PositionMoveRotation;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class FakeItemDisplay implements FakeEntity {

    public Location location;
    public Set<Player> viewerPlayers = new HashSet<>();
    public Display.ItemDisplay entity;

    public FakeItemDisplay(@NotNull Location location) {
        this.location = location;
        this.entity =
                new Display.ItemDisplay(EntityType.ITEM_DISPLAY, Extensions.getMinecraftLevel(location.getWorld()));
        Extensions.setLocation(entity, location);
        entity.setTransformationInterpolationDelay(0);
        entity.setTransformationInterpolationDuration(1);
        ((ItemDisplay) entity.getBukkitEntity()).setInterpolationDuration(1);
        ((ItemDisplay) entity.getBukkitEntity()).setTeleportDuration(1);
        entity.setViewRange(999999f);
    }

    public void setGlowColor(@NotNull Color color) {
        entity.setGlowColorOverride(color.asRGB());
        sendMetadata();
    }

    public void setBrightness(org.bukkit.entity.Display.@NotNull Brightness brightness) {
        entity.setBrightnessOverride(new Brightness(brightness.getBlockLight(), brightness.getSkyLight()));
        sendMetadata();
    }

    public void setTransformation(Transformation transform) {
        entity.setTransformation(Extensions.getMojangTransformation(transform));
        sendMetadata();
    }

    public Transformation getTransformation() {
        return ((ItemDisplay) entity.getBukkitEntity()).getTransformation();
    }

    public void setRotation(float newYaw, float newPitch) {
        Location newLoc = location.clone();
        newLoc.setYaw(newYaw);
        newLoc.setPitch(newPitch);
        teleport(newLoc);
    }

    public void setTransform(ItemDisplay.ItemDisplayTransform transform) {
        entity.setItemTransform(Extensions.getVanilla(transform));
        sendMetadata();
    }

    public void setItem(ItemStack itemStack) {
        entity.setItemStack(Extensions.getVanilla(itemStack));
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
        return this.viewerPlayers;
    }

    @Override
    public Entity getEntity() {
        return entity;
    }

    @Override
    public void addViewer(Player player) {
        if(viewerPlayers.contains(player)) return;
        viewerPlayers.add(player);
        Extensions.sendPacket(player, Extensions.getSpawnPacket(entity));
        sendMetadata(player);
        teleport(location);
    }

    @Override
    public void removeViewer(Player player) {
        if(!viewerPlayers.contains(player)) return;

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
            Extensions.sendPacket(
                    p,
                    new ClientboundTeleportEntityPacket(
                            entity.getId(), PositionMoveRotation.of(entity), Set.of(), entity.onGround));
        }
    }

    @Override
    public void setGlowing(boolean glowing) {
        entity.setGlowingTag(glowing);
        sendMetadata();
    }
}
