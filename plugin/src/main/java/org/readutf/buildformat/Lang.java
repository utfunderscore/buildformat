package org.readutf.buildformat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

public class Lang {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static @NotNull Component getRegionQuery(@NotNull String property, int stepNumber) {
        return miniMessage.deserialize(
                "<white><step>.</white> <gray>Make a region selection for property '<light_purple><name></light_purple>'",
                Placeholder.component("name", Component.text(property)), Placeholder.component("step", Component.text(stepNumber)));
    }

    public static @NotNull Component getPositionSet(@NotNull String position, @NotNull Location location) {
        String x = String.format("%.2f", location.getX());
        String y = String.format("%.2f", location.getY());
        String z = String.format("%.2f", location.getZ());
        return miniMessage.deserialize(
                "<gray>Set <position> to (<white><x></white>, <white><y></white>, <white><z></white>)</gray>",
                Placeholder.component("position", Component.text(position)),
                Placeholder.component("x", Component.text(x)),
                Placeholder.component("y", Component.text(y)),
                Placeholder.component("z", Component.text(z)));
    }

}
