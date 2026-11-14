package dev.tptools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TpCommands implements CommandExecutor, TabCompleter {

    private final TpToolsPlugin plugin;

    public TpCommands(TpToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "tp" -> tp(sender, args);
            case "tphere" -> tpHere(sender, args);
            case "back" -> back(sender);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void tp(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!(sender instanceof Player self)) {
                plugin.msg(sender, "player-only");
                return;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.msg(sender, "player-not-found", "input", args[0]);
                return;
            }
            if (target.equals(self)) {
                plugin.msg(sender, "same-player");
                return;
            }
            teleport(self, target.getLocation());
            plugin.msg(sender, "teleported-to", "target", target.getName());
        } else if (args.length == 2) {
            Player from = Bukkit.getPlayer(args[0]);
            if (from == null) {
                plugin.msg(sender, "player-not-found", "input", args[0]);
                return;
            }
            Player to = Bukkit.getPlayer(args[1]);
            if (to == null) {
                plugin.msg(sender, "player-not-found", "input", args[1]);
                return;
            }
            if (from.equals(to)) {
                plugin.msg(sender, "same-player");
                return;
            }
            teleport(from, to.getLocation());
            plugin.msg(sender, "teleported-other", "player", from.getName(), "target", to.getName());
            if (!from.equals(sender)) {
                plugin.msg(from, "you-were-teleported", "target", to.getName());
            }
        } else {
            plugin.msg(sender, "usage-tp");
        }
    }

    private void tpHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player self)) {
            plugin.msg(sender, "player-only");
            return;
        }
        if (args.length != 1) {
            plugin.msg(sender, "usage-tphere");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.msg(sender, "player-not-found", "input", args[0]);
            return;
        }
        if (target.equals(self)) {
            plugin.msg(sender, "same-player");
            return;
        }
        teleport(target, self.getLocation());
        plugin.msg(sender, "brought", "player", target.getName());
        plugin.msg(target, "you-were-teleported", "target", self.getName());
    }

    private void back(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.msg(sender, "player-only");
            return;
        }
        Location loc = plugin.getBack(player.getUniqueId());
        if (loc == null || loc.getWorld() == null) {
            plugin.msg(sender, "no-back");
            return;
        }
        teleport(player, loc);
        plugin.msg(sender, "back-success");
    }

    private void teleport(Player player, Location destination) {
        player.teleportAsync(destination, PlayerTeleportEvent.TeleportCause.COMMAND);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        boolean completes = (name.equals("tp") && args.length <= 2)
                || (name.equals("tphere") && args.length == 1);
        if (!completes) {
            return List.of();
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                result.add(p.getName());
            }
        }
        return result;
    }
}
