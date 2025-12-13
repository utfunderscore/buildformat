package org.readutf.buildformat.commands;

import dev.rollczi.litecommands.annotations.argument.Arg;
import dev.rollczi.litecommands.annotations.async.Async;
import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.execute.Execute;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.readutf.buildformat.format.FormatRegistry;
import org.readutf.buildformat.requirement.Requirement;
import org.readutf.buildformat.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Command(name = "build")
public class BuildCommand {

    private static final Logger log = LoggerFactory.getLogger(BuildCommand.class);
    private final SessionManager sessionManager;
    private final FormatRegistry formatRegistry;

    public BuildCommand(SessionManager sessionManager, FormatRegistry formatRegistry) {
        this.sessionManager = sessionManager;
        this.formatRegistry = formatRegistry;
    }

    @Execute(name = "cancel")
    public void cancel(@Context Player player) {
        sessionManager.endSession(player);
    }

    @Execute(name = "create")
    @Async
    public void create(@Context Player player, @Arg String format) {
        try {
            Map<String, List<Requirement>> formats = this.formatRegistry.readFormats();
            List<Requirement> requirements = formats.get(format);
            if (requirements == null) {
                player.sendMessage(Component.text("Format not found").color(NamedTextColor.RED));
                return;
            }

            sessionManager.startInputSession(player, format, requirements);
        } catch (Exception e) {
            log.error("Failed to start build session", e);
            player.sendMessage(Component.text("Error starting build session: " + e.getMessage())
                    .color(NamedTextColor.RED));
        }
    }
}
