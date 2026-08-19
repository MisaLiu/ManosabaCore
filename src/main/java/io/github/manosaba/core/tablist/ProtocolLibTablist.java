package io.github.manosaba.core.tablist;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedRemoteChatSessionData;
import io.github.manosaba.core.ManosabaCore;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The sole class that references ProtocolLib APIs. */
public final class ProtocolLibTablist {

    private final Supplier<TablistSnapshot> snapshotSupplier;
    private final Consumer<Throwable> failureHandler;
    private final ProtocolManager manager;
    private final PacketAdapter listener;
    private volatile boolean bypassMasking;

    private ProtocolLibTablist(@NotNull ManosabaCore plugin,
                               @NotNull Supplier<TablistSnapshot> snapshotSupplier,
                               @NotNull Consumer<Throwable> failureHandler) {
        this.snapshotSupplier = snapshotSupplier;
        this.failureHandler = failureHandler;
        this.manager = ProtocolLibrary.getProtocolManager();
        this.listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST, PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(@NotNull PacketEvent event) {
                if (bypassMasking) {
                    return;
                }
                try {
                    maskPacket(event.getPlayer().getUniqueId(), event.getPacket(), snapshotSupplier.get());
                } catch (Throwable failure) {
                    failureHandler.accept(failure);
                }
            }
        };
    }

    public static @NotNull ProtocolLibTablist register(@NotNull ManosabaCore plugin,
                                                         @NotNull Supplier<TablistSnapshot> snapshotSupplier,
                                                         @NotNull Consumer<Throwable> failureHandler) {
        ProtocolLibTablist integration = new ProtocolLibTablist(plugin, snapshotSupplier, failureHandler);
        integration.manager.addPacketListener(integration.listener);
        return integration;
    }

    public void unregister() {
        manager.removePacketListener(listener);
    }

    /** Sends each online viewer one merged game-mode update for the current online player set. */
    public void refreshAll(@NotNull TablistSnapshot snapshot) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            List<PlayerInfoData> entries = new ArrayList<>();
            for (Player target : Bukkit.getOnlinePlayers()) {
                NativeGameMode realMode = NativeGameMode.fromBukkit(target.getGameMode());
                if (realMode == null) {
                    continue;
                }
                NativeGameMode visibleMode = snapshot.shouldMaskFor(viewer.getUniqueId(), target.getUniqueId())
                        ? NativeGameMode.SURVIVAL
                        : realMode;
                entries.add(newPlayerInfo(target, visibleMode));
            }
            sendGameModeUpdate(viewer, entries);
        }
    }

    /** Restores real game modes without the outbound listener reapplying a mask. */
    public void restoreAll() {
        bypassMasking = true;
        try {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                List<PlayerInfoData> entries = new ArrayList<>();
                for (Player target : Bukkit.getOnlinePlayers()) {
                    NativeGameMode mode = NativeGameMode.fromBukkit(target.getGameMode());
                    if (mode != null) {
                        entries.add(newPlayerInfo(target, mode));
                    }
                }
                sendGameModeUpdate(viewer, entries);
            }
        } finally {
            bypassMasking = false;
        }
    }

    private void maskPacket(@NotNull UUID viewerId,
                            @NotNull PacketContainer packet,
                            @NotNull TablistSnapshot snapshot) {
        Set<PlayerInfoAction> actions = packet.getPlayerInfoActions().read(0);
        if (actions == null || (!actions.contains(PlayerInfoAction.UPDATE_GAME_MODE)
                && !actions.contains(PlayerInfoAction.ADD_PLAYER))) {
            return;
        }

        List<PlayerInfoData> original = packet.getPlayerInfoDataLists().read(0);
        if (original == null || original.isEmpty()) {
            return;
        }
        List<PlayerInfoData> replacement = new ArrayList<>(original.size());
        boolean changed = false;
        for (PlayerInfoData entry : original) {
            if (entry == null || !snapshot.shouldMaskFor(viewerId, entry.getProfileId())
                    || entry.getGameMode() != NativeGameMode.SPECTATOR) {
                replacement.add(entry);
                continue;
            }
            try {
                replacement.add(withGameMode(entry, NativeGameMode.SURVIVAL));
                changed = true;
            } catch (RuntimeException ignored) {
                // Preserve a malformed individual entry rather than corrupting the entire packet.
                replacement.add(entry);
            }
        }
        if (changed) {
            packet.getPlayerInfoDataLists().write(0, replacement);
        }
    }

    private static @NotNull PlayerInfoData newPlayerInfo(@NotNull Player player, @NotNull NativeGameMode gameMode) {
        return new PlayerInfoData(
                player.getUniqueId(),
                0,
                true,
                gameMode,
                WrappedGameProfile.fromPlayer(player),
                null,
                false,
                0,
                null
        );
    }

    private static @NotNull PlayerInfoData withGameMode(@NotNull PlayerInfoData entry,
                                                         @NotNull NativeGameMode gameMode) {
        WrappedRemoteChatSessionData session = entry.getRemoteChatSessionData();
        return new PlayerInfoData(
                entry.getProfileId(),
                entry.getLatency(),
                entry.isListed(),
                gameMode,
                entry.getProfile(),
                entry.getDisplayName(),
                entry.isShowHat(),
                entry.getListOrder(),
                session
        );
    }

    private void sendGameModeUpdate(@NotNull Player viewer, @NotNull List<PlayerInfoData> entries) {
        if (entries.isEmpty()) {
            return;
        }
        PacketContainer packet = manager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        packet.getPlayerInfoActions().write(0, EnumSet.of(PlayerInfoAction.UPDATE_GAME_MODE));
        packet.getPlayerInfoDataLists().write(0, entries);
        try {
            manager.sendServerPacket(viewer, packet);
        } catch (RuntimeException failure) {
            failureHandler.accept(failure);
        }
    }
}
