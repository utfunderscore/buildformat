package org.readutf.buildformat.requirement.impl;

import org.bukkit.entity.Player;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.types.Position;

public class PositionRequirementCollector extends RequirementCollector<Position> {

    @Override
    protected void start(Player player) {
        player.sendMessage("");
    }

    @Override
    protected void cancel(Player player) {

    }
}
