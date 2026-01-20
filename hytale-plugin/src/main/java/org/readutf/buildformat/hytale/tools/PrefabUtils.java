package org.readutf.buildformat.hytale.tools;

import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.blocktype.component.BlockPhysics;
import com.hypixel.hytale.server.core.prefab.PrefabStore;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.ChunkAccessor;
import com.hypixel.hytale.server.core.universe.world.accessor.LocalCachedChunkAccessor;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonDocument;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class PrefabUtils {

    public static byte @NotNull [] savePrefab(@NotNull String name, World world, Cuboid cuboid, boolean includeEntities) {
        long start = System.nanoTime();
        if (!name.endsWith(".prefab.json")) {
            name = name + ".prefab.json";
        }

        PrefabStore prefabStore = PrefabStore.get();
        Path serverPrefabsPath = prefabStore.getServerPrefabsPath();

        Position minPos = cuboid.min();
        Position maxPos = cuboid.max();

        Vector3i min = new Vector3i((int) minPos.x(), (int) minPos.y(), (int) minPos.z());
        Vector3i max = new Vector3i((int) maxPos.x(), (int) maxPos.y(), (int) maxPos.z());

        int xMin = min.getX();
        int yMin = min.getY();
        int zMin = min.getZ();
        int xMax = max.getX();
        int yMax = max.getY();
        int zMax = max.getZ();
        int width = xMax - xMin;
        int height = yMax - yMin;
        int depth = zMax - zMin;
        int halfWidth = width / 2;
        int halfDepth = depth / 2;
        LocalCachedChunkAccessor accessor = LocalCachedChunkAccessor.atWorldCoords(world, xMin + halfWidth, zMin + halfDepth, Math.max(width, depth));
        BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
        int editorBlock = assetMap.getIndex("Editor_Block");
        int editorBlockPrefabAir = assetMap.getIndex("Editor_Empty");
        int editorBlockPrefabAnchor = assetMap.getIndex("Editor_Anchor");
        BlockSelection tempSelection = new BlockSelection();
        tempSelection.setPosition(xMin + halfWidth, yMin, zMin + halfDepth);
        tempSelection.setSelectionArea(min, max);
        tempSelection.setAnchor(xMax, yMax, zMin);
        int count = 0;
        int top = Math.max(yMin, yMax);
        int bottom = Math.min(yMin, yMax);

        for (int x = xMin; x <= xMax; x++) {
            for (int z = zMin; z <= zMax; z++) {
                WorldChunk chunk = accessor.getChunk(ChunkUtil.indexChunkFromBlock(x, z));
                Store<ChunkStore> store = chunk.getReference().getStore();
                ChunkColumn chunkColumn = store.getComponent(chunk.getReference(), ChunkColumn.getComponentType());
                int lastSection = -1;
                BlockPhysics blockPhysics = null;

                for (int y = top; y >= bottom; y--) {
                    int block = chunk.getBlock(x, y, z);
                    int fluid = chunk.getFluidId(x, y, z);
                    if (lastSection != ChunkUtil.chunkCoordinate(y)) {
                        lastSection = ChunkUtil.chunkCoordinate(y);
                        Ref<ChunkStore> section = chunkColumn.getSection(lastSection);
                        if (section != null) {
                            blockPhysics = store.getComponent(section, BlockPhysics.getComponentType());
                        } else {
                            blockPhysics = null;
                        }
                    }

                    if (block == editorBlockPrefabAnchor) {
                        tempSelection.setAnchorAtWorldPos(x, y, z);
                        int id = getNonEmptyNeighbourBlock(accessor, x, y, z);
                        if (id > 0 && id != editorBlockPrefabAir) {
                            tempSelection.addBlockAtWorldPos(x, y, z, id, 0, 0, 0);
                            count++;
                        } else if (id == editorBlockPrefabAir) {
                            tempSelection.addBlockAtWorldPos(x, y, z, 0, 0, 0, 0);
                            count++;
                        }
                    }
                    if (block == editorBlockPrefabAir) {
                        tempSelection.addBlockAtWorldPos(x, y, z, 0, 0, 0, 0);
                    } else {
                        tempSelection.copyFromAtWorld(x, y, z, chunk, blockPhysics);
                    }

                    count++;
                }
            }
        }

        if (includeEntities) {
            Store<EntityStore> entityStore = world.getEntityStore().getStore();
            BuilderToolsPlugin.forEachCopyableInSelection(world, xMin, yMin, zMin, width, height, depth, e -> {
                Holder<EntityStore> holder = entityStore.copyEntity(e);
                tempSelection.addEntityFromWorld(holder);
            });
        }


        BlockSelection postClone = tempSelection.relativize();
        BsonDocument bsonDocument = SelectionPrefabSerializer.serialize(postClone);
        return bsonDocument.toJson().getBytes(StandardCharsets.UTF_8);


    }

    private static int getNonEmptyNeighbourBlock(@Nonnull ChunkAccessor accessor, int x, int y, int z) {
        int blockId;
        if ((blockId = accessor.getBlock(x, y, z + 1)) > 0) {
            return blockId;
        } else if ((blockId = accessor.getBlock(x, y, z - 1)) > 0) {
            return blockId;
        } else if ((blockId = accessor.getBlock(x, y + 1, z)) > 0) {
            return blockId;
        } else if ((blockId = accessor.getBlock(x, y - 1, z)) > 0) {
            return blockId;
        } else if ((blockId = accessor.getBlock(x - 1, y, z)) > 0) {
            return blockId;
        } else {
            return (blockId = accessor.getBlock(x + 1, y, z)) > 0 ? blockId : 0;
        }
    }

}
