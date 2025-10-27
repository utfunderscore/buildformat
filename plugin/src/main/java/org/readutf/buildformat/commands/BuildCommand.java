package org.readutf.buildformat.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import org.bukkit.entity.Player;
import org.readutf.buildformat.requirement.SessionManager;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.NumberRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.List;

@Command(name = "build")
public class BuildCommand {

    @Execute(name = "create")
    @Async
    public void create(@Context Player player, @Arg String format) throws Exception {
        SessionManager.get()
                .startInputSession(
                        player,
                        List.of(
                                new PositionRequirement("spawn1"),
                                new PositionRequirement("spawn2"),
                                new CuboidRequirement("safezone"),
                                new StringRequirement("lobbyName"),
                                new IntegerRequirement("maxPlayers",1, 100)));
    }
}
