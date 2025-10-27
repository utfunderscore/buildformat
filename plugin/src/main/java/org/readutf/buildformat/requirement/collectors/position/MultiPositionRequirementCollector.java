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
import org.readutf.buildformat.requirement.types.ListRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class MultiPositionRequirementCollector implements RequirementCollector<List<Position>> {

    @NotNull
    private final Requirement requirement;

    private final int stepNumber;

    private final int minimum;
    private final int maximum;

    private final List<Position> positions;
    private final CompletableFuture<List<Position>> future;
    private final List<UUID> toolIds = new ArrayList<>();

    public MultiPositionRequirementCollector(@NotNull ListRequirement requirement, int stepNumber) throws Exception {
        this.requirement = requirement;
        this.stepNumber = stepNumber;
        this.future = new CompletableFuture<>();
        this.positions = new ArrayList<>();
        if (!(requirement.innerRequirement() instanceof PositionRequirement)) {
            throw new Exception("MultiPositionRequirementCollector requires a ListRequirement of Position type");
        }
        this.minimum = requirement.min();
        this.maximum = requirement.max();
    }

    @Override
    public void start(@NotNull Player player) {
        AtomicReference<Tool> tool = new AtomicReference<>(PositionTool.getTool(requirement.getName()));
        this.toolIds.add(tool.get().id());

        player.sendMessage(Lang.getPositionQuery(requirement.getName(), stepNumber));

        player.getInventory().setItem(0, tool.get().itemStack());

        player.getInventory().setItem(7, ClickableManager.setClickAction(ItemStack.of(Material.LIME_WOOL), () -> {
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

                tool.set(PositionTool.getTool(requirement.getName()));
                this.toolIds.add(tool.get().id());
            }
        }));

        player.getInventory()
                .setItem(8, ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {}));
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
