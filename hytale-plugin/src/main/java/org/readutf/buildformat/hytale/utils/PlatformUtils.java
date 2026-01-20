package org.readutf.buildformat.hytale.utils;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.types.Position;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class PlatformUtils {

    public static @NotNull UUID getPlayerId(Player player) {
        return player.getPlayerRef().getUuid();
    }

    public static Position fromVector(Vector3d vector3d) {
        return new Position(vector3d.x, vector3d.y, vector3d.z);
    }

    public static Position fromVector(Vector3i vector3d) {
        return new Position(vector3d.x, vector3d.y, vector3d.z);
    }

    public static World getWorld(PlayerRef playerRef) {
        return Universe.get().getWorld(playerRef.getWorldUuid());
    }

    public static CompletableFuture<Void> runInWorld(World world, Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        world.execute(() -> {
            runnable.run();
            future.complete(null);
        });

        return future;
    }

    public static void clearInventory(@NotNull Player player) {
        player.getInventory().clear();
    }

    public static void clearInventory(@NotNull PlayerRef playerRef) {
        Player player = playerRef.getComponent(Player.getComponentType());
        if(player != null) {
            clearInventory(player);
        }
    }

    public static void setItem(@NotNull PlayerRef playerRef, int slot, @NotNull ItemStack itemStack) {
        Player player = playerRef.getComponent(Player.getComponentType());
        if(player != null) setItem(player, slot, itemStack);
    }

    public static void setItem(@NotNull Player player, int slot, @NotNull ItemStack itemStack) {
        player.getInventory().getCombinedHotbarFirst().setItemStackForSlot((short) slot, itemStack);
    }

    public static <T> @NotNull CompletableFuture<T> runInWorld(@NotNull World world, Supplier<T> tSupplier) {
        CompletableFuture<T> future = new CompletableFuture<>();

        world.execute(() -> {
            T result = tSupplier.get();
            future.complete(result);
        });

        return future;
    }

}

