package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;

public interface RequirementCollector<T> {

    void start(Player player);

    void cleanup(Player player);

    T awaitBlocking();

}
