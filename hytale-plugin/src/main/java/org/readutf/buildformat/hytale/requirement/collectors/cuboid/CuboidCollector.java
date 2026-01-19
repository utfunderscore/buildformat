package org.readutf.buildformat.hytale.requirement.collectors.cuboid;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypixel.hytale.builtin.buildertools.BuilderToolsPlugin;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.util.TempAssetIdUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.hytale.utils.ClickableManager;
import org.readutf.buildformat.hytale.utils.PlatformUtils;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class CuboidCollector implements RequirementCollector<Cuboid> {

    private final Message message;
    private final CompletableFuture<Cuboid> future;

    public CuboidCollector(Message message) {
        this.future = new CompletableFuture<>();
        this.message = message;
    }

    @Override
    public void start(@NotNull PlayerRef playerRef, @NotNull Cuboid bounds, @NotNull Position origin) {

        Player player = playerRef.getComponent(Player.getComponentType());

        playerRef.sendMessage(message);

        ItemStack confirmItem = ClickableManager.setClickAction(new ItemStack("Cloth_Block_Wool_Green"), (player1, itemStack) -> {
            Cuboid selection = getSelection(player1);

            if (selection == null) {
                player.sendMessage(Message.raw("Please make a full selection.").color(Color.RED));
                SoundUtil.playSoundEvent2dToPlayer(playerRef, TempAssetIdUtil.getSoundEventIndex("CREATE_ERROR"), SoundCategory.UI);
            } else {
                SoundUtil.playSoundEvent2dToPlayer(playerRef, TempAssetIdUtil.getSoundEventIndex("SFX_Player_Drop_Item"), SoundCategory.UI);
                future.complete(selection.relative(origin));
            }
        });

        PlatformUtils.setItem(player, 8, confirmItem);
        PlatformUtils.setItem(player, 0, new ItemStack("EditorTool_Selection"));

    }

    private static @Nullable Cuboid getSelection(Player player) {
        BuilderToolsPlugin.BuilderState state = BuilderToolsPlugin.get().getBuilderState(player, player.getPlayerRef());

        BlockSelection selection = state.getSelection();

        if (selection.getSelectionMin() == Vector3i.ZERO || selection.getSelectionMax() == Vector3i.ZERO) return null;

        return new Cuboid(
                PlatformUtils.fromVector(selection.getSelectionMin()),
                PlatformUtils.fromVector(selection.getSelectionMax()));
    }

    @Override
    public void cleanup(@NotNull PlayerRef playerRef) {
        Player player = playerRef.getComponent(Player.getComponentType());
        BuilderToolsPlugin.addToQueue(player, playerRef, (r, s, componentAccessor) -> s.deselect(componentAccessor));
    }

    @Override
    public CompletableFuture<BuildSetting<Cuboid>> getFuture() {
        return future.thenApply(cuboid -> new BuildSetting<>(cuboid, new TypeReference<>() {
        }));
    }
}
