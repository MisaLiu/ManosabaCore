package io.github.manosaba.core.tablist;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TablistMaskDecisionTest {

    @Test
    void masksDeadPlayingSpectatorFromActiveViewer() {
        assertTrue(TablistMaskDecision.shouldMask(true, true, true, true, true, false));
    }

    @Test
    void doesNotMaskWhenGameIsNotRunning() {
        assertFalse(TablistMaskDecision.shouldMask(false, true, true, true, true, false));
    }

    @Test
    void doesNotMaskObserver() {
        assertFalse(TablistMaskDecision.shouldMask(true, false, false, true, true, false));
    }

    @Test
    void doesNotMaskNonPlayingDeadPlayer() {
        assertFalse(TablistMaskDecision.shouldMask(true, true, false, true, true, false));
    }

    @Test
    void doesNotMaskNonSpectatorGameMode() {
        assertFalse(TablistMaskDecision.shouldMask(true, true, true, false, true, false));
    }

    @Test
    void doesNotMaskObserverViewer() {
        assertFalse(TablistMaskDecision.shouldMask(true, true, true, true, false, false));
    }

    @Test
    void revealPermissionWins() {
        assertFalse(TablistMaskDecision.shouldMask(true, true, true, true, true, true));
    }
}
