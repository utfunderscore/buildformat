package org.readutf.buildformat.hytale.requirement.collectors.position;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.TempAssetIdUtil;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.hytale.utils.PlatformUtils;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;
import org.readutf.buildformat.hytale.tools.PositionTool;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.hytale.utils.ClickableManager;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class PositionRequirementCollector implements RequirementCollector<Position> {

    private final CompletableFuture<Position> future;

    private final @NotNull Message message;
    private final @NotNull String markerName;
    private String toolId;

    public PositionRequirementCollector(@NotNull Message message, @NotNull String markerName) {
        this.future = new CompletableFuture<>();
        this.message = message;
        this.markerName = markerName;
    }

    @Override
    public void start(@NotNull PlayerRef playerRef, @NotNull Cuboid bounds, @NotNull Position origin) {
        Pair<ItemStack, String> toolData = PositionTool.getTool(markerName);
        Player player = playerRef.getComponent(Player.getComponentType());
        if(player == null) return;

        toolId = toolData.right();

        player.sendMessage(message);

        ItemStack confirmItem = ClickableManager.setClickAction(new ItemStack("Cloth_Block_Wool_Green"), (player1, itemStack) -> {
            Vector3d selection = PositionTool.getPosition(toolData.right());

            if (selection == null) {
                player.sendMessage(Message.raw("Please make a full selection.").color(Color.RED));
                SoundUtil.playSoundEvent2dToPlayer(playerRef, TempAssetIdUtil.getSoundEventIndex("CREATE_ERROR"), SoundCategory.UI);
            } else {
                Position position = new Position(selection.x, selection.y, selection.z);

                SoundUtil.playSoundEvent2dToPlayer(playerRef, TempAssetIdUtil.getSoundEventIndex("SFX_Player_Drop_Item"), SoundCategory.UI);
                future.complete(position.relative(origin));
            }
        });

        PlatformUtils.setItem(player, 8, confirmItem);
        PlatformUtils.setItem(player, 1, toolData.first());
    }

    @Override
    public void cleanup(PlayerRef player) {
        PositionTool.cleanup(toolId);
    }

    @Override
    public CompletableFuture<BuildSetting<Position>> getFuture() {
        return future.thenApply(position -> new BuildSetting<>(position, new TypeReference<>() {
        }));
    }
}
