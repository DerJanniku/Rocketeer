package org.derjannik.rocketeer;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.List;
import java.util.ArrayList;

public class RocketeerPlugin extends JavaPlugin {

    private RocketeerManager rocketeerManager;

    @Override
    public void onEnable() {
        // Initialize RocketeerManager
        this.rocketeerManager = new RocketeerManager(this);

        PluginCommand command = this.getCommand("rocketeer");
        if (command == null) {
            getLogger().severe("Command 'rocketeer' is not registered! Check plugin.yml.");
        } else {
            command.setExecutor(new RocketeerCommand(this));
            getLogger().info("Command 'rocketeer' has been registered successfully.");
        }
        // Register event listeners
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);

        getLogger().info("Rocketeer plugin has been enabled!");
    }


        @Override
        public void onDisable() {
            // Plugin shutdown logic
            getLogger().info("Rocketeer plugin has been disabled!");
        }

        public List<ResupplyStation> getResupplyStations() {
            // Placeholder implementation
            return new ArrayList<>();
        }

        public RocketeerManager getRocketeerManager() {
            return rocketeerManager;
        }
}