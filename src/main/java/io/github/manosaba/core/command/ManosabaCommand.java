package io.github.manosaba.core.command;

import io.github.manosaba.core.ManosabaCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ManosabaCommand implements TabExecutor {

    private static final List<String> SUBCOMMANDS = List.of("reload", "version", "help");

    private static final String PERMISSION_RELOAD = "manosaba.command.reload";
    private static final String PERMISSION_HUB = "manosaba.alias.hub";
    private static final String PERMISSION_CHECK_PERM = "manosaba.alias.checkperm";
    private static final String PERMISSION_RESET = "manosaba.alias.reset";

    private final ManosabaCore plugin;

    public ManosabaCommand(@NotNull ManosabaCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender,
                             @NotNull Command command,
                             @NotNull String label,
                             @NotNull String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (isDatapackAlias(commandName)) {
            handleDatapackAlias(sender, commandName, args);
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> handleReload(sender);
            case "version" -> handleVersion(sender);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0], NamedTextColor.RED));
                sendHelp(sender);
            }
        }
        return true;
    }

    private static boolean isDatapackAlias(@NotNull String commandName) {
        return commandName.equals("hub") || commandName.equals("checkperm") || commandName.equals("reset");
    }

    private void handleDatapackAlias(@NotNull CommandSender sender,
                                     @NotNull String commandName,
                                     @NotNull String[] args) {
        if (!argsAreEmpty(args)) {
            sender.sendMessage(Component.text("Usage: /" + commandName, NamedTextColor.YELLOW));
            return;
        }

        String permission = switch (commandName) {
            case "hub" -> PERMISSION_HUB;
            case "checkperm" -> PERMISSION_CHECK_PERM;
            case "reset" -> PERMISSION_RESET;
            default -> throw new IllegalArgumentException("Unknown datapack alias: " + commandName);
        };
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(Component.text("You do not have permission to use this command.", NamedTextColor.RED));
            return;
        }

        if ((commandName.equals("hub") || commandName.equals("checkperm")) && !(sender instanceof Player)) {
            sender.sendMessage(Component.text("You must be a player to use this command.", NamedTextColor.RED));
            return;
        }

        String targetCommand;
        CommandSender commandSender;
        if (commandName.equals("hub")) {
            commandSender = sender;
            targetCommand = DatapackAliases.HUB;
        } else if (commandName.equals("checkperm")) {
            Player player = (Player) sender;
            commandSender = Bukkit.getConsoleSender();
            targetCommand = DatapackAliases.checkPermCommand(player.getName());
        } else {
            commandSender = Bukkit.getConsoleSender();
            targetCommand = DatapackAliases.RESET;
        }

        try {
            if (!Bukkit.dispatchCommand(commandSender, targetCommand)) {
                reportAliasFailure(sender, commandName, targetCommand, null);
            }
        } catch (RuntimeException ex) {
            reportAliasFailure(sender, commandName, targetCommand, ex);
        }
    }

    private static boolean argsAreEmpty(@NotNull String[] args) {
        return args.length == 0;
    }

    private void reportAliasFailure(@NotNull CommandSender sender,
                                    @NotNull String alias,
                                    @NotNull String targetCommand,
                                    @Nullable RuntimeException failure) {
        sender.sendMessage(Component.text("Command execution failed. Contact an administrator.", NamedTextColor.RED));
        if (failure == null) {
            plugin.getLogger().warning("Datapack alias /" + alias + " was not dispatched: " + targetCommand);
        } else {
            plugin.getLogger().warning("Datapack alias /" + alias + " failed while dispatching '"
                    + targetCommand + "': " + failure);
        }
    }

    private void handleReload(@NotNull CommandSender sender) {
        if (!sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(Component.text("You do not have permission to reload.", NamedTextColor.RED));
            return;
        }
        try {
            plugin.reloadConfiguration();
            sender.sendMessage(Component.text("ManosabaCore configuration reloaded.", NamedTextColor.GREEN));
        } catch (RuntimeException ex) {
            sender.sendMessage(Component.text("Reload failed: " + ex.getMessage(), NamedTextColor.RED));
            plugin.getLogger().warning("Configuration reload failed: " + ex);
        }
    }

    private void handleVersion(@NotNull CommandSender sender) {
        String version = plugin.getPluginMeta().getVersion();
        sender.sendMessage(Component.text("ManosabaCore v" + version, NamedTextColor.AQUA));
    }

    private void sendHelp(@NotNull CommandSender sender) {
        sender.sendMessage(Component.text("ManosabaCore commands:", NamedTextColor.GOLD));
        if (sender.hasPermission("manosaba.command.reload")) {
            sender.sendMessage(Component.text("  /manosaba reload  ", NamedTextColor.YELLOW)
                    .append(Component.text("- reload configuration", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.text("  /manosaba version ", NamedTextColor.YELLOW)
                .append(Component.text("- show plugin version", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("  /manosaba help    ", NamedTextColor.YELLOW)
                .append(Component.text("- show this help", NamedTextColor.GRAY)));
        if (sender.hasPermission(PERMISSION_HUB)) {
            sender.sendMessage(Component.text("  /hub              ", NamedTextColor.YELLOW)
                    .append(Component.text("- return to the map hub", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission(PERMISSION_CHECK_PERM)) {
            sender.sendMessage(Component.text("  /checkperm        ", NamedTextColor.YELLOW)
                    .append(Component.text("- check the current map permissions", NamedTextColor.GRAY)));
        }
        if (sender.hasPermission(PERMISSION_RESET)) {
            sender.sendMessage(Component.text("  /reset            ", NamedTextColor.YELLOW)
                    .append(Component.text("- reload Manosaba map debug state", NamedTextColor.GRAY)));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                 @NotNull Command command,
                                                 @NotNull String alias,
                                                 @NotNull String[] args) {
        if (isDatapackAlias(command.getName().toLowerCase(Locale.ROOT))) {
            return List.of();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>(SUBCOMMANDS.size());
            for (String s : SUBCOMMANDS) {
                if (s.startsWith(prefix)) {
                    out.add(s);
                }
            }
            return out;
        }
        return List.of();
    }
}
