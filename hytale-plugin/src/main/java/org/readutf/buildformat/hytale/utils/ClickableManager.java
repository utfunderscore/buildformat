package org.readutf.buildformat.hytale.utils;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class ClickableManager {

    private static final Map<String, BiConsumer<Player, ItemStack>> clickables = new HashMap<>();

    static {
        PacketAdapters.registerOutbound((PlayerPacketWatcher) (playerRef, packet) -> {
            if (!(packet instanceof SyncInteractionChains syncInteractionChains)) {
                return;
            }

            for (SyncInteractionChain chain : syncInteractionChains.updates) {
                Player player = playerRef.getComponent(Player.getComponentType());
                ItemStack activeHotbarItem = player.getInventory().getActiveHotbarItem();

//                System.out.println("SyncInteractionChain{id=" + chain.chainId +
//                        ", activeHotbarSlot=" + chain.activeHotbarSlot +
//                        ", activeUtilitySlot=" + chain.activeUtilitySlot +
//                        ", activeToolsSlot=" + chain.activeToolsSlot +
//                        ", itemInHandId=" + (chain.itemInHandId != null ? chain.itemInHandId : "<null>") +
//                        ", utilityItemId=" + (chain.utilityItemId != null ? chain.utilityItemId : "<null>") +
//                        ", toolsItemId=" + (chain.toolsItemId != null ? chain.toolsItemId : "<null>") +
//                        ", initial=" + chain.initial +
//                        ", desync=" + chain.desync +
//                        ", overrideRootInteraction=" + chain.overrideRootInteraction +
//                        ", interactionType=" + chain.interactionType +
//                        ", equipSlot=" + chain.equipSlot +
//                        ", chainId=" + chain.chainId +
//                        ", forkedId=" + (chain.forkedId != null ? chain.forkedId : "<null>") +
//                        ", data=" + (chain.data != null ? chain.data : "<null>") +
//                        ", state=" + chain.state +
//                        ", newForks=" + (chain.newForks != null ? chain.newForks.length : "<null>") +
//                        ", operationBaseIndex=" + chain.operationBaseIndex +
//                        ", interactionData=" + (chain.interactionData != null ? chain.interactionData.length : "<null>") +
//                        "}");

                if (activeHotbarItem == null || chain.interactionType != InteractionType.Secondary || !chain.initial) return;

                BsonDocument metadata = activeHotbarItem.getMetadata();

                if (metadata == null) return;
                BsonValue value = metadata.get("click_id");
                if (value != null && value.isString()) {
                    BiConsumer<Player, ItemStack> runnable = clickables.get(value.asString().getValue());
                    if (runnable != null) {
                        runnable.accept(player, activeHotbarItem);
                    }
                }
            }
        });
    }

    public static @NotNull ItemStack setClickAction(@NotNull ItemStack itemStack, BiConsumer<Player, ItemStack> playerRefConsumer) {
        String id = UUID.randomUUID().toString();

        BsonDocument document = new BsonDocument();
        document.put("click_id", new BsonString(id));

        clickables.put(id, playerRefConsumer);

        return itemStack.withMetadata(document);
    }
}
