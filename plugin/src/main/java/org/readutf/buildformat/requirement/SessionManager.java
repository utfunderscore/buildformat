package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.impl.PositionRequirementCollector;
import org.readutf.buildformat.requirement.impl.RegionRequirementCollector;
import org.readutf.buildformat.requirement.impl.RequirementCollectorFactory;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.*;

public class SessionManager {

    private final Map<UUID, Session> sessions;
    private final Map<Class<?>, RequirementCollectorFactory> collectors;

    private static SessionManager sessionManager = new SessionManager();

    public SessionManager() {
        this.sessions = new HashMap<>();
        this.collectors = Map.of(
                Position.class, PositionRequirementCollector::new,
                Cuboid.class, RegionRequirementCollector::new);
    }

    public void startInputSession(@NotNull Player player, @NotNull List<Requirement> requirements) throws Exception {
        if (sessions.containsKey(player.getUniqueId())) {
            throw new Exception("Session already active");
        }
        Map<String, RequirementCollector<?>> sessionCollectors = new HashMap<>();

        for (int i = 0; i < requirements.size(); i++) {
            Requirement requirement = requirements.get(i);

            RequirementCollectorFactory requirementCollector = collectors.get(requirement.getType());
            if (requirementCollector == null) {
                throw new Exception("No collector available for " + requirement.getType());
            }
            RequirementCollector<?> collector = requirementCollector.createCollector(requirement.getName(), i + 1);
            sessionCollectors.put(requirement.getName(), collector);
        }

        player.getInventory().clear();

        Thread.startVirtualThread(() -> {
            Map<String, Object> results = new HashMap<>();

            sessionCollectors.forEach((s, requirementCollector) -> {
                requirementCollector.start(player);
                results.put(s, requirementCollector.awaitBlocking());
            });


            player.getInventory().clear();
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
