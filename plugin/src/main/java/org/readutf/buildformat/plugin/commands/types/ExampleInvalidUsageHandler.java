package org.readutf.buildformat.plugin.commands.types;

import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invalidusage.InvalidUsage;
import dev.rollczi.litecommands.invalidusage.InvalidUsageHandler;
import dev.rollczi.litecommands.invocation.Invocation;
import dev.rollczi.litecommands.schematic.Schematic;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ExampleInvalidUsageHandler implements InvalidUsageHandler<CommandSender> {
    @Override
    public void handle(
            Invocation<CommandSender> invocation,
            InvalidUsage<CommandSender> result,
            ResultHandlerChain<CommandSender> chain
    ) {
        CommandSender sender = invocation.sender();
        Schematic schematic = result.getSchematic();

        if (schematic.isOnlyFirst()) {
            sender.sendMessage(Component.text("Invalid usage of command!").color(NamedTextColor.RED)
                    .append(Component.text(" (" + schematic.first() + ")").color(NamedTextColor.GRAY)));
            return;
        }

        sender.sendMessage(Component.text("Invalid usage of command!").color(NamedTextColor.RED));
        for (String scheme : schematic.all()) {
            sender.sendMessage(Component.text(" - ").color(NamedTextColor.DARK_GRAY)
                    .append(Component.text(scheme).color(NamedTextColor.GRAY)));
        }
    }
}