package org.readutf.buildformat.hytale.requirement.collectors.position;

import com.fasterxml.jackson.core.type.TypeReference;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import it.unimi.dsi.fastutil.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.hytale.utils.PlatformUtils;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.hytale.requirement.RequirementCollector;
import org.readutf.buildformat.hytale.tools.PositionTool;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.hytale.utils.ClickableManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MultiPositionRequirementCollector implements RequirementCollector<List<Position>> {

    private final String name;
    private final int stepNumber;

    private final CompletableFuture<List<Position>> future;
    private final List<String> toolIds = new ArrayList<>();

    public MultiPositionRequirementCollector(@NotNull Requirement requirement, int stepNumber) {
        if (!(requirement instanceof PositionListRequirement positionListRequirement)) {
            throw new IllegalArgumentException("Requirement must be a PositionRequirement");
        }
        this.name = requirement.name();
        this.stepNumber = stepNumber;
        this.future = new CompletableFuture<>();
    }

    @Override
    public void start(@NotNull PlayerRef playerRef, @NotNull Cuboid bounds, @NotNull Position origin) {
        Player player = playerRef.getComponent(Player.getComponentType());
        if (player == null) throw new IllegalStateException("Player should not be null");

        playerRef.sendMessage(Message.raw("Multi position"));
        AtomicInteger currentId = new AtomicInteger(1);

        List<Position> positions = new ArrayList<>();

        AtomicReference<Pair<ItemStack, String>> toolData = new AtomicReference<>(PositionTool.getTool(name));
        toolIds.add(toolData.get().right());


        ItemStack confirmItem = ClickableManager.setClickAction(new ItemStack("Cloth_Block_Wool_Green"), (player1, itemStack) -> {
            @Nullable
            Vector3d position = PositionTool.getPosition(toolData.get().right());

            if (position != null) {
                positions.add(new Position(position.x, position.y, position.z).relative(origin));
            }

            if (positions.isEmpty()) {
                playerRef.sendMessage(
                        Message.raw("Please make at least 1 selection.").color(Color.RED));
            } else {

                future.complete(positions);
            }
        });

        ItemStack addPosition = ClickableManager.setClickAction(new ItemStack("Rock_Gem_Emerald"), (_, _) -> {
            if (PositionTool.getPosition(toolData.get().right()) == null) {
                playerRef.sendMessage(Message.raw("Please set a position before adding a new one.")
                        .color(Color.RED));
                return;
            }

            currentId.addAndGet(1);
            Vector3d position = PositionTool.getPosition(toolData.get().right());
            if (position != null) {
                positions.add(new Position(position.x, position.y, position.z).relative(origin));
            }

            toolData.set(PositionTool.getTool(name));
            toolIds.add(toolData.get().right());


            player.getInventory()
                    .getCombinedHotbarFirst()
                    .setItemStackForSlot((short) 0, toolData.get().left());
        });

        try {
            PlatformUtils.setItem(player, 0, toolData.get().first());
            PlatformUtils.setItem(player, 7, addPosition);
            PlatformUtils.setItem(player, 8, confirmItem);

        } catch (Exception e) {
            e.printStackTrace();
        }

        player.sendMessage(Message.raw("Test123"));
    }

    @Override
    public void cleanup(PlayerRef player) {
        for (String toolId : toolIds) {
            PositionTool.cleanup(toolId);
            System.out.println("cleaning tool for " + toolId);
        }
    }

    @Override
    public CompletableFuture<BuildSetting<List<Position>>> getFuture() {
        return future.thenApply(positions -> new BuildSetting<>(positions, new TypeReference<>() {
        }));
    }
}
