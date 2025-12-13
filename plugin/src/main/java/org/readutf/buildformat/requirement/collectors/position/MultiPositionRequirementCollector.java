package org.readutf.buildformat.requirement.collectors.position;

import com.fasterxml.jackson.core.type.TypeReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.TaskUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class MultiPositionRequirementCollector implements RequirementCollector<List<Position>> {

    private final String name;
    private final int stepNumber;

    private final List<Position> positions;
    private final CompletableFuture<List<Position>> future;
    private final List<UUID> toolIds = new ArrayList<>();

    public MultiPositionRequirementCollector(@NotNull Requirement requirement, int stepNumber) {
        if (!(requirement instanceof PositionListRequirement positionListRequirement)) {
            throw new IllegalArgumentException("Requirement must be a PositionRequirement");
        }
        this.name = requirement.name();
        this.stepNumber = stepNumber;
        this.future = new CompletableFuture<>();
        this.positions = new ArrayList<>();
    }

    @Override
    public void start(@NotNull Player player, @NotNull Cuboid bounds, @NotNull Position origin) {
        player.sendMessage(Lang.getPositionQuery(name, stepNumber));
        AtomicInteger currentId = new AtomicInteger(1);

        List<Position> positions = new ArrayList<>();

        AtomicReference<Tool> tool = new AtomicReference<>(PositionTool.getTool(name + " #" + currentId));
        toolIds.add(tool.get().id());

        ItemStack confirmButton = ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            Position position = PositionTool.getPosition(tool.get().id());
            if(position != null) {
                positions.add(position.relative(origin));
            }

            if (positions.isEmpty()) {
                player.sendMessage(
                        Component.text("Please make at least 1 selection.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {

                future.complete(positions);
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3, 1);
            }
        });

        ItemStack addPosition = ClickableManager.setClickAction(ItemStack.of(Material.LIME_WOOL), () -> {
            if(PositionTool.getPosition(tool.get().id()) == null) {
                player.sendMessage(
                        Component.text("Please set a position before adding a new one.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
                return;
            }

            currentId.addAndGet(1);
            Position position = PositionTool.getPosition(tool.get().id());
            if (position != null) {
                positions.add(position.relative(origin));
            }

            tool.set(PositionTool.getTool(name + " #" + currentId));
            toolIds.add(tool.get().id());

            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1, 5);
            player.getInventory().setItem(0, tool.get().itemStack());
        });

        TaskUtils.runSync(() -> {
            player.getInventory().setItem(0, tool.get().itemStack());
            player.getInventory().setItem(7, addPosition);
            player.getInventory().setItem(8, confirmButton);
        });
    }

    @Override
    public void cleanup(Player player) {
        for (UUID toolId : toolIds) {
            PositionTool.clearTool(toolId);
        }
    }

    @Override
    public BuildSetting<List<Position>> awaitBlocking() {
        return new BuildSetting<>(future.join(), new TypeReference<>() {
        });
    }
}
