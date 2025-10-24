package org.readutf.buildformat.requirement.impl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.RegionSelectionTool;
import org.readutf.buildformat.types.Cuboid;

public class RegionRequirementCollector extends RequirementCollector<Cuboid> {

    private final String name;
    private final int stepNumber;

    public RegionRequirementCollector(String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    @Override
    public void start(@NotNull Player player) {

        player.getInventory().setItem(0, RegionSelectionTool.tool);

        player.getInventory().setItem(8, ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            Cuboid selection = RegionSelectionTool.getSelection(player.getUniqueId());
            if(selection == null) {
                player.sendMessage(Component.text("Please make a full selection.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                complete(selection);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 3, 1);
            }
        }));
    }

    @Override
    public void cancel(Player player) {
        player.getInventory().setItem(0, ItemStack.of(Material.AIR));
    }
}
