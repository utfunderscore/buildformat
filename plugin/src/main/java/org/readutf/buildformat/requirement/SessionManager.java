package org.readutf.buildformat.requirement;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.readutf.buildformat.requirement.collectors.position.PositionRequirementCollector;
import org.readutf.buildformat.requirement.collectors.RegionRequirementCollector;
import org.readutf.buildformat.requirement.collectors.RequirementCollectorFactory;
import org.readutf.buildformat.requirement.collectors.text.TextInputCollector;
import org.readutf.buildformat.requirement.types.CuboidRequirement;
import org.readutf.buildformat.requirement.types.NumberRequirement;
import org.readutf.buildformat.requirement.types.PositionRequirement;
import org.readutf.buildformat.requirement.types.StringRequirement;
import org.readutf.buildformat.requirement.types.number.IntegerRequirement;
import org.readutf.buildformat.types.Cuboid;
import org.readutf.buildformat.types.Position;

import java.util.*;

public class SessionManager {

    private final Map<UUID, Session> sessions;
    private final Map<Class<? extends Requirement>, RequirementCollectorFactory> collectors;
    private static SessionManager sessionManager = new SessionManager();

    public SessionManager() {
        this.sessions = new HashMap<>();
        this.collectors = Map.of(
                PositionRequirement.class, PositionRequirementCollector::new,
                CuboidRequirement.class, RegionRequirementCollector::new,
                StringRequirement.class, (requirement, step) -> new TextInputCollector<>(requirement.getName(), step, str -> str),
                IntegerRequirement.class, (requirement, step) -> new TextInputCollector<>(requirement.getName(), step, Integer::parseInt)
        );
    }

    public void startInputSession(@NotNull Player player, @NotNull List<Requirement> requirements) throws Exception {
        if (sessions.containsKey(player.getUniqueId())) {
            throw new Exception("Session already active");
        }

        player.getInventory().clear();

        Map<String, Object> results = new HashMap<>();
        List<RequirementCollector<?>> completed = new ArrayList<>();

        for (int i = 0; i < requirements.size(); i++) {

            Requirement requirement = requirements.get(i);

            RequirementCollectorFactory requirementCollector = collectors.get(requirement.getClass());
            if (requirementCollector == null) {
                throw new Exception("No collector available for " + requirement.getClass());
            }
            RequirementCollector<?> collector = requirementCollector.createCollector(requirement, i + 1);

            player.getInventory().clear();
            collector.start(player);
            results.put(requirement.getName(), collector.awaitBlocking());
            completed.add(collector);
        }

        for (RequirementCollector<?> collector : completed) {
            collector.cleanup(player);
        }

        player.getInventory().clear();

        player.sendMessage("Data collected:");
        for (Map.Entry<String, Object> stringObjectEntry : results.entrySet()) {
            player.sendMessage(" - " + stringObjectEntry.getKey() + ": " + stringObjectEntry.getValue());
        }
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
