package io.github.manosaba.core.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatapackAliasesTest {

    @Test
    void hubUsesThePlayerTriggerCommand() {
        assertEquals("trigger hub", DatapackAliases.HUB);
    }

    @Test
    void checkPermRunsAsAndAtThePlayer() {
        assertEquals(
                "execute as Alice at @s run function lobby:trigger_items/effection/permission_check",
                DatapackAliases.checkPermCommand("Alice")
        );
    }

    @Test
    void resetUsesTheServerFunctionCommand() {
        assertEquals("function manosaba:debug/reload", DatapackAliases.RESET);
    }
}
