package org.readutf.buildformat.requirement.collectors;

import org.bukkit.entity.Player;
import org.readutf.buildformat.requirement.RequirementCollector;
import org.readutf.buildformat.tools.PositionTool;
import org.readutf.buildformat.types.Position;

public class PositionRequirementCollector extends RequirementCollector<Position> {

    private final String name;
    private final int stepNumber;

    public PositionRequirementCollector(String name, int stepNumber) {
        this.name = name;
        this.stepNumber = stepNumber;
    }

    @Override
    protected void start(Player player) {

        PositionTool.giveTool(player, name);

    }

    @Override
    protected void cancel(Player player) {

    }
}
