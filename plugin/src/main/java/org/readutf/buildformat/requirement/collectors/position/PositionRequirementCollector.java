package org.readutf.buildformat.requirement.collectors.position;

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
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.tools.Tool;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;
import org.readutf.buildformat.utils.TaskUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PositionRequirementCollector implements RequirementCollector<Position> {

    @NotNull
    private final String name;

    private final int stepNumber;

    private final CompletableFuture<Position> future;

    private @Nullable UUID toolId;

    public PositionRequirementCollector(@NotNull Requirement requirement, int stepNumber) {
        this.name = requirement.name();
        this.stepNumber = stepNumber;
        this.future = new CompletableFuture<>();
    }

    @Override
    public void start(@NotNull Player player, @NotNull Cuboid bounds, @NotNull Position origin) {
        Tool tool = PositionTool.getTool(name);
        this.toolId = tool.id();

        player.sendMessage(Lang.getPositionQuery(name, stepNumber));


        ItemStack itemStack = ClickableManager.setClickAction(ItemStack.of(Material.EMERALD_BLOCK), () -> {
            @Nullable Position position = PositionTool.getPosition(tool.id());

            if (position == null) {
                player.sendMessage(Component.text("Use the tool provided to set a position.").color(NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 5, 1);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 3, 1);
                future.complete(position.relative(origin));
            }
        });

        TaskUtils.runSync(() -> {
            player.getInventory().setItem(0, tool.itemStack());
            player.getInventory().setItem(8, itemStack);
        });
    }

    @Override
    public void cleanup(Player player) {
        PositionTool.clearTool(toolId);
    }

    @Override
    public BuildSetting<Position> awaitBlocking() {
        return new BuildSetting<>(future.join(), new TypeReference<>() {
        });
    }
}
