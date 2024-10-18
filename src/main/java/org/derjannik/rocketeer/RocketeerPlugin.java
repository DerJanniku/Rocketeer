package org.derjannik.rocketeer;

import org.bukkit.plugin.java.JavaPlugin;

public class RocketeerPlugin extends JavaPlugin {

    private RocketeerManager rocketeerManager;

    @Override
    public void onEnable() {
        // Initialize RocketeerManager
        this.rocketeerManager = new RocketeerManager(this);

        // Register commands
        if (this.getCommand("rocketeer") == null) {
            getLogger().severe("Command 'rocketeer' is not registered!");
        } else {
            this.getCommand("rocketeer").setExecutor(new RocketeerCommand(this));
            getLogger().info("Command 'rocketeer' has been registered successfully.");
        }

        if (this.getCommand("rocketeer_egg") == null) {
            getLogger().severe("Command 'rocketeer_egg' is not registered!");
        } else {
            this.getCommand("rocketeer_egg").setExecutor(new RocketeerEggCommand(this));
            getLogger().info("Command 'rocketeer_egg' has been registered successfully.");
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

    public RocketeerManager getRocketeerManager() {
        return rocketeerManager;
    }
}
