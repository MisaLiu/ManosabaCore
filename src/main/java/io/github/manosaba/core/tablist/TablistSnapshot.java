package io.github.manosaba.core.tablist;

import java.util.Set;
import java.util.UUID;

/** Immutable main-thread state consumed by the ProtocolLib packet listener. */
public record TablistSnapshot(
        Set<UUID> maskedTargets,
        Set<UUID> activeAliveViewers,
        Set<UUID> revealViewers
) {
    public static final TablistSnapshot EMPTY = new TablistSnapshot(Set.of(), Set.of(), Set.of());

    public TablistSnapshot {
        maskedTargets = Set.copyOf(maskedTargets);
        activeAliveViewers = Set.copyOf(activeAliveViewers);
        revealViewers = Set.copyOf(revealViewers);
    }

    public boolean shouldMaskFor(UUID viewerId, UUID targetId) {
        return maskedTargets.contains(targetId)
                && activeAliveViewers.contains(viewerId)
                && !revealViewers.contains(viewerId);
    }
}
