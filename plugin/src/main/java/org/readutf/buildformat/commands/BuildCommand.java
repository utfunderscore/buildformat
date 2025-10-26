package org.readutf.buildformat.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import org.bukkit.entity.Player;
import org.readutf.buildformat.requirement.SessionManager;
import org.readutf.buildformat.requirement.types.SimpleRequirement;
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
                                new SimpleRequirement("spawn1", Position.class),
                                new SimpleRequirement("spawn2", Position.class),
                                new SimpleRequirement("safezone", Cuboid.class)));
    }
}
