package org.readutf.buildformat.hytale.tools;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.*;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.Pair;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.*;

public class PositionTool {

    private static final Logger log = LoggerFactory.getLogger(PositionTool.class);
    private static final Map<String, MarkerData> entityStore = new HashMap<>();
    private static final Map<String, Vector3d> toolPositions = new HashMap<>();
    private static final Map<String, String> toolNames = new HashMap<>();

    public static @NotNull Pair<ItemStack, String> getTool(String markerName) {
        String id = UUID.randomUUID().toString();

        BsonDocument metadata = new BsonDocument();
        metadata.put("position_tool_id", new BsonString(id));
        ItemStack itemstack = new ItemStack("Furniture_Lumberjack_Sign").withMetadata(metadata);

        toolNames.put(id, markerName);

        return Pair.of(itemstack, id);
    }

    public static void cleanup(String toolId) {
        MarkerData data = entityStore.get(toolId);
        if (data == null) return;
        if (!data.ref.isValid()) return;

        World world = Universe.get().getWorld(data.worldId());
        if (world == null) {
            log.warn("Cleaning up position marker in world that no longer exists.");
            return;
        }
        world.execute(() -> {
            data.ref.getStore().removeEntity(data.ref, RemoveReason.REMOVE);
        });

    }

    public static @Nullable Vector3d getPosition(String id) {
        return toolPositions.get(id);
    }

    public record MarkerData(Ref<EntityStore> ref, String worldId) {

    }


    public static class PlayerInteractionListener extends EntityEventSystem<EntityStore, PlaceBlockEvent> {


        protected PlayerInteractionListener() {
            super(PlaceBlockEvent.class);
        }

        @Override
        public void handle(
                int i,
                @NotNull ArchetypeChunk<EntityStore> archetypeChunk,
                @NotNull Store<EntityStore> store,
                @NotNull CommandBuffer<EntityStore> commandBuffer,
                @NotNull PlaceBlockEvent placeBlockEvent) {

            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
            if (!ref.isValid()) return;

            Player playerComponent = store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null) return;

            ItemStack itemstack = placeBlockEvent.getItemInHand();
            if (itemstack == null) return;

            BsonDocument metadata = itemstack.getMetadata();
            if (metadata == null) return;

            if (metadata.containsKey("position_tool_id")) {
                placeBlockEvent.setCancelled(true);

                String id = metadata.getString("position_tool_id").getValue();
                Vector3d targetBlock = placeBlockEvent.getTargetBlock().toVector3d().add(0.5, 0, 0.5);
                System.out.println(targetBlock);
                toolPositions.put(id, targetBlock);

                HeadRotation rotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
                if (rotationComponent == null) return;
                Vector3f rotation = rotationComponent.getRotation().clone();

                rotation.x = 0;
                rotation.z = 0;
                rotation.y = (rotation.y + (float) Math.PI);

                World world = playerComponent.getWorld();
                if (world == null) return;

                moveMarker(id, playerComponent.getWorld(), store, targetBlock, rotation);
            }
        }

        public void moveMarker(
                String id, @NotNull World world, @NotNull Store<EntityStore> store, Vector3d position, Vector3f rotation) {
            world.execute(() -> {

                MarkerData existing = entityStore.get(id);

                if (existing != null) {
                    Ref<EntityStore> ref = existing.ref;
                    ref.getStore().addComponent(ref, Teleport.getComponentType(), new Teleport(position, rotation));
                } else {

                    String markerName = toolNames.getOrDefault(id, "Unknown");

                    Holder<EntityStore> holder = store.getRegistry().newHolder();
                    holder.addComponent(BlockEntity.getComponentType(), new BlockEntity("Deco_Target"));
                    holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, rotation));
                    holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1 * 2.0F));
                    ItemStack itemStack = new ItemStack("DecoTarget", 1);
                    itemStack.setOverrideDroppedItemAnimation(true);
                    holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(itemStack));
                    holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
                    holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
                    holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(rotation));
                    holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
                    holder.addComponent(Nameplate.getComponentType(), new Nameplate(markerName));
                    holder.ensureComponent(UUIDComponent.getComponentType());
                    Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);

                    entityStore.put(id, new MarkerData(ref, world.getName()));
                }
            });
        }

        @Override
        public @Nullable Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }
}
