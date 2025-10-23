package org.readutf.buildformat.commands;


import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import org.bukkit.entity.Player;
import org.readutf.buildformat.BuildFormat;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.requirement.SessionManager;
import org.readutf.buildformat.types.Cuboid;

import java.util.List;

@Command(name = "build")
public class BuildCommand {

    @Execute(name = "create")
    public void create(@Context Player player, @Arg String format) throws Exception {

        SessionManager.get().startInputSession(player, new BuildFormat("test", List.of(new Requirement(
                Cuboid.class, "first"
        ))));
    }

}
