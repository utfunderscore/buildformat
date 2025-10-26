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
import org.readutf.buildformat.tools.RegionSelectionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Cuboid;

import java.util.UUID;

public class RegionRequirementCollector extends RequirementCollector<Cuboid> {


    @NotNull
    private final String name;
    private final int stepNumber;
    private UUID toolId;

    public RegionRequirementCollector(@NotNull String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    @Override
    public void start(@NotNull Player player) {

        player.sendMessage(Lang.getRegionQuery(name, stepNumber));
        Tool tool = RegionSelectionTool.givePlayerTool(name);
        player.give(tool.itemStack());

        this.toolId = tool.id();

        player.getInventory().setItem(8, ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            @Nullable Cuboid selection = getSelection(tool.id());

            if (selection == null) {
                player.sendMessage(
                        Component.text("Please make a full selection.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                complete(selection);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 3, 1);
            }
        }));
    }

    private static @Nullable Cuboid getSelection(UUID toolId) {
        RegionSelectionTool.Selection selection = RegionSelectionTool.getSelection(toolId);
        if (selection == null || !selection.isComplete()) {
            return null;
        }
        return selection.getRegion();
    }

    @Override
    public void cleanup(@NotNull Player player) {
        player.getInventory().setItem(0, ItemStack.of(Material.AIR));

        RegionSelectionTool.clearTool(toolId);
    }
}
