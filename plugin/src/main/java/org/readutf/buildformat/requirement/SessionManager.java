package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.BuildFormat;
import org.readutf.buildformat.requirement.impl.PositionRequirementCollector;
import org.readutf.buildformat.requirement.impl.RegionRequirementCollector;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.*;

public class SessionManager {

    private final Map<UUID, Session> sessions;
    private final Map<Class<?>, RequirementCollector<?>> collectors;

    private static SessionManager sessionManager = new SessionManager();

    public SessionManager() {
        this.sessions = new HashMap<>();
        this.collectors = Map.of(
                Position.class, new PositionRequirementCollector(),
                Cuboid.class, new RegionRequirementCollector());
    }

    public void startInputSession(@NotNull Player player, @NotNull BuildFormat buildFormat) throws Exception {
        if (sessions.containsKey(player.getUniqueId())) {
            throw new Exception("Session already active");
        }
        Map<String, RequirementCollector<?>> sessionCollectors = new HashMap<>();
        for (Requirement requirement : buildFormat.requirements()) {
            RequirementCollector<?> requirementCollector = collectors.get(requirement.dataType());
            if (requirementCollector == null) {
                throw new Exception("No collector available for " + requirement.dataType());
            }
            sessionCollectors.put(requirement.attributeName(), requirementCollector);
        }

        player.getInventory().clear();

        Thread.startVirtualThread(() -> {
            Map<String, Object> results = new HashMap<>();

            sessionCollectors.forEach((s, requirementCollector) -> {
                requirementCollector.start(player);
                results.put(s, requirementCollector.awaitBlocking());
            });

            System.out.println(results);
        });
    }

    public RequirementCollector<?> getCurrentCollector(UUID playerId) {
        Session session = sessions.get(playerId);
        if (session == null) return null;
        return session.requirements.get(session.index());
    }

    public record Session(List<RequirementCollector<?>> requirements, Integer index) {

        public Session {
            if (requirements().isEmpty()) {
                throw new IllegalStateException("There must be at least 1 requirement");
            }
        }

        public Session(List<RequirementCollector<?>> requirements) {
            this(requirements, 0);
        }
    }

    public static SessionManager get() {
        return sessionManager;
    }
}
