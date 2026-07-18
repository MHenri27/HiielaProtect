package ee.henri.hiielaprotect;

import ee.henri.hiielaprotect.commands.KaitseCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class HiielaProtect extends JavaPlugin {
    
    private ConfigManager configManager;
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        databaseManager = new DatabaseManager(this);
        
        databaseManager.setup();

        getCommand("kaitse").setExecutor(new KaitseCommand(this));
        getCommand("kaitse").setTabCompleter(new KaitseCommand(this));
        
        getLogger().info("HiielaProtect enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("HiielaProtect disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
