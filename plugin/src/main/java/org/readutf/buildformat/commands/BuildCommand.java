package org.readutf.buildformat.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.readutf.buildformat.requirement.SessionManager;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;

import java.util.List;

@Command(name = "build")
public class BuildCommand {

    @Execute(name = "create")
    @Async
    public void create(@Context Player player, @Arg String format) {
        try {
            SessionManager.get()
                    .startInputSession(
                            player,
                            List.of(
                                    new PositionRequirement("spawn1"),
                                    new PositionRequirement("spawn2"),
                                    new CuboidRequirement("safezone"),
                                    new StringRequirement("lobbyName"),
                                    new IntegerRequirement("maxPlayers", 1, 100)));
        } catch (Exception e) {
            player.sendMessage(Component.text("Error starting build session: " + e.getMessage())
                    .color(NamedTextColor.RED));
        }
    }
}
