package org.readutf.buildformat.requirement.collectors.cuboid;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.readutf.buildformat.settings.BuildSetting;
import org.readutf.buildformat.tools.ClickableManager;
import org.readutf.buildformat.tools.RegionSelectionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.TaskUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CuboidRequirementCollector implements RequirementCollector<Cuboid> {

    @NotNull
    private final String name;

    private final int stepNumber;
    private final CompletableFuture<Cuboid> future;

    private UUID toolId;

    public CuboidRequirementCollector(@NotNull Requirement requirement, int stepNumber) {
        this.name = requirement.name();
        this.stepNumber = stepNumber;
        this.future = new CompletableFuture<>();
    }

    @Override
    public void start(@NotNull Player player, @NotNull Cuboid bounds, @NotNull Position origin) {

        player.sendMessage(Lang.getRegionQuery(name, stepNumber));
        Tool tool = RegionSelectionTool.getTool(name);
        this.toolId = tool.id();

        ItemStack itemStack = ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            @Nullable Cuboid selection = getSelection(tool.id());

            if (selection == null) {
                player.sendMessage(
                        Component.text("Please make a full selection.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                future.complete(selection.relative(origin));
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3, 1);
            }
        });
        TaskUtils.runSync(() -> {
            player.getInventory().setItem(0, tool.itemStack());
            player.getInventory().setItem(8, itemStack);
        });
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
        TaskUtils.runSync(() -> player.getInventory().setItem(0, ItemStack.of(Material.AIR)));

        RegionSelectionTool.clearTool(toolId);
    }

    @Override
    public BuildSetting<Cuboid> awaitBlocking() {
        return new BuildSetting<>(future.join(), new TypeReference<>() {
        });
    }
}
