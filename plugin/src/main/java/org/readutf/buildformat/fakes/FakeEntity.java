package org.readutf.buildformat.fakes;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import org.bukkit.Location;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public interface FakeEntity {
    Location getLocation();

    void setLocation(Location location);

    Set<Player> getViewerPlayers();

    Entity getEntity();

    default List<Player> getViewers() {
        return List.copyOf(getViewerPlayers());
    }

    void addViewer(Player player);

    void removeViewer(Player player);

    void despawn();

    void teleport(Location location);

    void setGlowing(boolean glowing);

    default void sendMetadata() {
        sendMetadata(null);
    }

    default void sendMetadata(Player player) {
        List<Player> players = (player == null) ? getViewers() : List.of(player);
        ClientboundSetEntityDataPacket entityMetadataPacket = new ClientboundSetEntityDataPacket(
                getEntity().getId(), getEntity().getEntityData().packAll());
        for (Player p : players) {
            ((CraftPlayer) p).getHandle().connection.send(entityMetadataPacket);
        }
    }
}
