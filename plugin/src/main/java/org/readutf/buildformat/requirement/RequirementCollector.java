package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;
import org.readutf.buildformat.settings.BuildSetting;

public interface RequirementCollector<T> {

    void start(Player player);

    void cleanup(Player player);

    BuildSetting<T> awaitBlocking();

}
