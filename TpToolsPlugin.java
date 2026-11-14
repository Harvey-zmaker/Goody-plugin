package dev.tptools;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TpToolsPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Location> backLocations = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private File dataFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        dataFile = new File(getDataFolder(), "back.yml");
        loadBack();

        TpCommands commands = new TpCommands(this);
        for (String name : new String[] {"tp", "tphere", "back"}) {
            PluginCommand cmd = getCommand(name);
            if (cmd != null) {
                cmd.setExecutor(commands);
                cmd.setTabCompleter(commands);
            }
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        saveBack();
    }

    // Records the location a player teleported FROM (any teleport cause).
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("tptools.back")) {
            return;
        }
        backLocations.put(player.getUniqueId(), event.getFrom().clone());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        saveBack();
    }

    public Location getBack(UUID id) {
        Location loc = backLocations.get(id);
        return loc == null ? null : loc.clone();
    }

    public void msg(CommandSender target, String key, String... placeholders) {
        String raw = getConfig().getString("messages." + key, "<red>Missing message: " + key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", mm.escapeTags(placeholders[i + 1]));
        }
        String prefix = getConfig().getString("prefix", "");
        target.sendMessage(mm.deserialize(prefix + raw));
    }

    private void loadBack() {
        if (!dataFile.exists()) {
            return;
        }
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : yml.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                Location loc = yml.getLocation(key);
                if (loc != null && loc.getWorld() != null) {
                    backLocations.put(id, loc);
                }
            } catch (Exception ignored) {
                // Skip entries with invalid UUIDs or missing worlds
            }
        }
    }

    private void saveBack() {
        YamlConfiguration yml = new YamlConfiguration();
        backLocations.forEach((id, loc) -> yml.set(id.toString(), loc));
        try {
            yml.save(dataFile);
        } catch (IOException e) {
            getLogger().warning("Could not save back.yml: " + e.getMessage());
        }
    }
}
