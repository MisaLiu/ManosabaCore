package io.github.manosaba.core.tablist;

import io.github.manosaba.core.ManosabaCore;
import io.github.manosaba.core.config.ProximityChatConfig;
import io.github.manosaba.core.game.DeathStatus;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bukkit-facing lifecycle and state cache for the optional ProtocolLib integration. */
public final class TablistBridge implements Listener {

    public static final String REVEAL_PERMISSION = "manosaba.tablist.reveal";
    private static final String PROTOCOL_LIB_PLUGIN_NAME = "ProtocolLib";

    private final ManosabaCore plugin;
    private final AtomicBoolean failureScheduled = new AtomicBoolean();
    private volatile TablistSnapshot snapshot = TablistSnapshot.EMPTY;
    private ProtocolLibTablist protocol;
    private BukkitTask syncTask;

    public TablistBridge(@NotNull ManosabaCore plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        applyConfiguration();
    }

    public void reload() {
        ProximityChatConfig cfg = plugin.chatConfig();
        if (cfg == null || !cfg.tablist().enabled()) {
            disableProtocol(true);
            return;
        }
        if (protocol == null) {
            applyConfiguration();
            return;
        }
        refreshSnapshot();
    }

    public void disable() {
        disableProtocol(true);
    }

    public TablistSnapshot snapshot() {
        return snapshot;
    }

    private void applyConfiguration() {
        ProximityChatConfig cfg = plugin.chatConfig();
        if (cfg == null || !cfg.tablist().enabled()) {
            restoreAndClear();
            plugin.getLogger().info("Tablist spectator masking disabled by configuration.");
            return;
        }
        if (Bukkit.getPluginManager().getPlugin(PROTOCOL_LIB_PLUGIN_NAME) == null) {
            restoreAndClear();
            plugin.getLogger().info("ProtocolLib plugin not present; tablist spectator masking disabled.");
            return;
        }

        try {
            protocol = ProtocolLibTablist.register(plugin, this::snapshot, this::disableForFailure);
            long period = cfg.tablist().syncPeriodTicks();
            syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refreshSnapshot, 1L, period);
            failureScheduled.set(false);
            refreshSnapshot();
            plugin.getLogger().info("Tablist spectator masking enabled (sync period=" + period + " ticks).");
        } catch (Throwable t) {
            disableProtocol(true);
            plugin.getLogger().warning("Failed to enable tablist spectator masking: " + t);
        }
    }

    private void refreshSnapshot() {
        ProximityChatConfig cfg = plugin.chatConfig();
        if (cfg == null || !cfg.tablist().enabled()) {
            return;
        }

        TablistSnapshot next = buildSnapshot(cfg);
        if (!next.equals(snapshot)) {
            snapshot = next;
            ProtocolLibTablist activeProtocol = protocol;
            if (activeProtocol != null) {
                activeProtocol.refreshAll(next);
            }
        }
    }

    private TablistSnapshot buildSnapshot(@NotNull ProximityChatConfig cfg) {
        if (cfg.gameState().onlyDuringGame() && !DeathStatus.isGameRunning(cfg.gameState())) {
            return TablistSnapshot.EMPTY;
        }

        ProximityChatConfig.DeadStateConfig deadCfg = cfg.deadState();
        Objective deadObjective = deadCfg.mode() == ProximityChatConfig.DeadStateConfig.Mode.SCOREBOARD
                ? DeathStatus.lookupObjective(deadCfg.objective())
                : null;
        ProximityChatConfig.PlayingStateConfig playingCfg = cfg.playingState();
        Objective playingObjective = playingCfg.enabled() ? DeathStatus.lookupObjective(playingCfg.objective()) : null;

        Set<UUID> targets = new HashSet<>();
        Set<UUID> activeAlive = new HashSet<>();
        Set<UUID> reveal = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean playing = DeathStatus.isPlaying(player, playingCfg, playingObjective);
            boolean dead = DeathStatus.isDead(player, deadCfg, deadObjective);
            UUID id = player.getUniqueId();
            if (TablistMaskDecision.shouldMask(true, dead, playing, player.getGameMode() == GameMode.SPECTATOR, true, false)) {
                targets.add(id);
            }
            if (playing && !dead && player.getGameMode() != GameMode.SPECTATOR) {
                activeAlive.add(id);
            }
            if (player.hasPermission(REVEAL_PERMISSION)) {
                reveal.add(id);
            }
        }
        return new TablistSnapshot(targets, activeAlive, reveal);
    }

    private void disableForFailure(@NotNull Throwable failure) {
        if (!failureScheduled.compareAndSet(false, true)) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> {
            plugin.getLogger().warning("Disabling tablist spectator masking after ProtocolLib failure: " + failure);
            disableProtocol(true);
        });
    }

    private void disableProtocol(boolean restore) {
        if (syncTask != null) {
            syncTask.cancel();
            syncTask = null;
        }
        ProtocolLibTablist activeProtocol = protocol;
        protocol = null;
        if (restore) {
            snapshot = TablistSnapshot.EMPTY;
        }
        if (activeProtocol != null) {
            if (restore) {
                activeProtocol.restoreAll();
            }
            activeProtocol.unregister();
        }
    }

    private void restoreAndClear() {
        ProtocolLibTablist activeProtocol = protocol;
        snapshot = TablistSnapshot.EMPTY;
        if (activeProtocol != null) {
            activeProtocol.restoreAll();
        }
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::refreshSnapshot, 1L);
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) {
        refreshSnapshot();
    }
}
