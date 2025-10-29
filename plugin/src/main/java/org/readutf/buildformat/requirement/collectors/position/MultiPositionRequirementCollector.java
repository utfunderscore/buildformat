package org.readutf.buildformat.requirement.collectors.position;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.list.PositionListRequirement;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.TaskUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
    public void start(@NotNull Player player) {
        AtomicReference<Tool> tool = new AtomicReference<>(PositionTool.getTool(name));
        this.toolIds.add(tool.get().id());

        player.sendMessage(Lang.getPositionQuery(name, stepNumber));

        ItemStack itemStack1 = ClickableManager.setClickAction(ItemStack.of(Material.LIME_WOOL), () -> {
            @Nullable Position position = PositionTool.getPosition(tool.get().id());

            if (position == null) {
                player.sendMessage(Component.text("Use the tool provided to set a position.")
                        .color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                positions.add(position);
                player.sendMessage(Component.text("Position " + positions.size() + " recorded.")
                        .color(NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 3, 1);

                tool.set(PositionTool.getTool(name));
                this.toolIds.add(tool.get().id());
            }
        });

        ItemStack itemStack = ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {});

        TaskUtils.runSync(() -> {
            player.getInventory().setItem(7, itemStack1);
            player.getInventory().setItem(8, itemStack);
            player.getInventory().setItem(0, tool.get().itemStack());
        });
    }

    @Override
    public void cleanup(Player player) {
        for (UUID toolId : toolIds) {
            PositionTool.clearTool(toolId);
        }
    }

    @Override
    public List<Position> awaitBlocking() {
        return future.join();
    }
}
