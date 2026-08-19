package io.github.manosaba.core.command;

/** Fixed command targets exposed by the companion map datapack. */
public final class DatapackAliases {

    public static final String HUB = "trigger hub";
    public static final String CHECK_PERM_FUNCTION = "lobby:trigger_items/effection/permission_check";
    public static final String RESET = "function manosaba:debug/reload";

    private DatapackAliases() {
    }

    public static String checkPermCommand(String playerName) {
        return "execute as " + playerName + " at @s run function " + CHECK_PERM_FUNCTION;
    }
}
