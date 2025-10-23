package org.readutf.buildformat.fakes;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display.BillboardConstraints;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay.ItemDisplayTransform;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;

public class Extensions {

    public static com.mojang.math.Transformation getMojangTransformation(Transformation t) {
        return new com.mojang.math.Transformation(
                t.getTranslation(), t.getLeftRotation(), t.getScale(), t.getRightRotation());
    }

    public static Transformation toBukkit(com.mojang.math.Transformation t) {
        return new Transformation(t.getTranslation(), t.getLeftRotation(), t.getScale(), t.getRightRotation());
    }

    public static ClientboundAddEntityPacket getSpawnPacket(Entity entity) {
        return new ClientboundAddEntityPacket(entity, entity.getId(), entity.blockPosition());
    }

    public static ClientboundRemoveEntitiesPacket getDespawnPacket(Entity entity) {
        return new ClientboundRemoveEntitiesPacket(entity.getId());
    }

    public static void sendPacket(Player player, Packet<?>... packets) {
        CraftPlayer craftPlayer = (CraftPlayer) player;
        for (Packet<?> packet : packets) {
            craftPlayer.getHandle().connection.send(packet);
        }
    }

    public static net.minecraft.world.item.ItemStack getVanilla(ItemStack itemStack) {
        return CraftItemStack.asCraftCopy(itemStack).handle;
    }

    public static net.minecraft.world.level.block.state.BlockState getVanilla(BlockState blockState) {
        return ((CraftBlockState) blockState).getHandle();
    }

    public static ItemDisplayContext getVanilla(ItemDisplayTransform transform) {
        return switch (transform) {
            case NONE -> ItemDisplayContext.NONE;
            case THIRDPERSON_LEFTHAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            case THIRDPERSON_RIGHTHAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
            case FIRSTPERSON_LEFTHAND -> ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            case FIRSTPERSON_RIGHTHAND -> ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
            case HEAD -> ItemDisplayContext.HEAD;
            case GUI -> ItemDisplayContext.GUI;
            case GROUND -> ItemDisplayContext.GROUND;
            case FIXED -> ItemDisplayContext.FIXED;
        };
    }

    public static void setLocation(Entity entity, Location location) {
        entity.teleportTo(
                (ServerLevel) getMinecraftLevel(location.getWorld()),
                location.getX(),
                location.getY(),
                location.getZ(),
                java.util.Collections.emptySet(),
                location.getYaw(),
                location.getPitch(),
                true);
        entity.setYHeadRot(location.getYaw());
        entity.setLevel(getMinecraftLevel(location.getWorld()));
    }

    public static Level getMinecraftLevel(World world) {
        return ((CraftWorld) world).getHandle();
    }

    public static net.minecraft.network.chat.Component toVanilla(Component component) {
        String json = GsonComponentSerializer.gson().serialize(component);
        return ComponentSerialization.CODEC
                .parse(JsonOps.INSTANCE, JsonParser.parseString(json))
                .getOrThrow();
    }
}
