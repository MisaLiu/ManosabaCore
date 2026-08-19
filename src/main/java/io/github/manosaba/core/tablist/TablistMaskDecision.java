package io.github.manosaba.core.tablist;

/** Pure decision logic for viewer-specific spectator-mode masking. */
public final class TablistMaskDecision {

    private TablistMaskDecision() {
    }

    public static boolean shouldMask(boolean gameRunning,
                                     boolean targetDead,
                                     boolean targetPlaying,
                                     boolean targetSpectator,
                                     boolean viewerActiveAlive,
                                     boolean viewerCanReveal) {
        return gameRunning
                && targetDead
                && targetPlaying
                && targetSpectator
                && viewerActiveAlive
                && !viewerCanReveal;
    }
}
