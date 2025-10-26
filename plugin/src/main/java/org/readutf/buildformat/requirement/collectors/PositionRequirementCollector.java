package org.readutf.buildformat.requirement.collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.readutf.buildformat.Lang;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Position;

import java.util.UUID;

public class PositionRequirementCollector extends RequirementCollector<Position> {

    @NotNull
    private final String name;

    @NotNull
    private final int stepNumber;

    private @Nullable UUID toolId;

    public PositionRequirementCollector(@NotNull String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    @Override
    protected void start(@NotNull Player player) {
        Tool tool = PositionTool.giveTool(name);
        this.toolId = tool.id();

        player.sendMessage(Lang.getPositionQuery(name, stepNumber));

        player.getInventory().setItem(0, tool.itemStack());

        player.getInventory().setItem(8, ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            @Nullable Position position = PositionTool.getPosition(tool.id());

            if(position == null) {
                player.sendMessage(Component.text("Use the tool provided to set a position.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 3, 1);
                complete(position);
            }
        }));
    }

    @Override
    protected void cleanup(Player player) {

        PositionTool.clearTool(toolId);

    }
}
