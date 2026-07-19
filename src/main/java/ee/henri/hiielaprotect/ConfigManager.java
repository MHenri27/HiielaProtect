package ee.henri.hiielaprotect;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ConfigManager {

    private final HiielaProtect plugin;
    private final MiniMessage miniMessage;
    private final boolean papiEnabled;

    public ConfigManager(HiielaProtect plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        this.papiEnabled = papi != null && papi.isEnabled();
    }

    public String getSubcommandName(String key) {
        return plugin.getConfig().getString("command-names." + key, key);
    }

    public Component getMessage(String key, Player player, String regionName, String commandRunner) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) return Component.empty();

        msg = msg.replace("%region_name%", regionName == null ? "" : regionName)
                 .replace("%player_name%", player == null ? "" : player.getName())
                 .replace("%command_runner%", commandRunner == null ? "" : commandRunner);

        if (papiEnabled && player != null) {
            msg = PlaceholderAPI.setPlaceholders(player, msg);
        }

        return miniMessage.deserialize(msg);
    }
    
    public Component getMessage(String key, String playerName, String regionName, String commandRunner) {
        String msg = plugin.getConfig().getString("messages." + key, "");
        if (msg.isEmpty()) return Component.empty();

        msg = msg.replace("%region_name%", regionName == null ? "" : regionName)
                 .replace("%player_name%", playerName == null ? "" : playerName)
                 .replace("%command_runner%", commandRunner == null ? "" : commandRunner);

        if (papiEnabled) {
            msg = PlaceholderAPI.setPlaceholders(null, msg);
        }

        return miniMessage.deserialize(msg);
    }

    public Component getMessage(String key) {
        return getMessage(key, (Player) null, "", "");
    }

    public String getRawMessage(String key) {
        String msg = plugin.getConfig().getString("messages." + key, "");

        if (papiEnabled) {
            msg = PlaceholderAPI.setPlaceholders(null, msg);
        }

        return msg;
    }

    public List<String> getCommandsOnCreate() {
        return plugin.getConfig().getStringList("commands_on_create");
    }

    public List<String> getCommandsBeforeCreate() {
        return plugin.getConfig().getStringList("commands_before_create");
    }

    public String getPermissionNode(String key) {
        return plugin.getConfig().getString("permissions." + key, "");
    }
}
