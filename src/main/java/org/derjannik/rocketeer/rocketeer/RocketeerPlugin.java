package org.derjannik.rocketeer;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class RocketeerPlugin extends JavaPlugin {

    private RocketeerManager rocketeerManager;

    @Override
    public void onEnable() {
        // Initialize RocketeerManager
        this.rocketeerManager = new RocketeerManager(this);

        // Register the rocketeer command
        getLogger().info("Attempting to register 'rocketeer' command.");
        PluginCommand rocketeerCommand = getCommand("rocketeer");
        if (rocketeerCommand == null) {
            getLogger().severe("Command 'rocketeer' is not registered! Check plugin.yml.");
        } else {
            rocketeerCommand.setExecutor(new RocketeerCommand(this));
            getLogger().info("Command 'rocketeer' has been registered successfully.");
        }

        // Register the rocketeer-egg command
        getLogger().info("Attempting to register 'rocketeer-egg' command.");
        PluginCommand eggCommand = getCommand("rocketeer-egg");
        if (eggCommand == null) {
            getLogger().severe("Command 'rocketeer-egg' is not registered! Check plugin.yml.");
        } else {
            eggCommand.setExecutor(new RocketeerEggCommand(this));
            getLogger().info("Command 'rocketeer-egg' has been registered successfully.");
        }

        // Register event listeners
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);

        getLogger().info("Rocketeer plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rocketeer plugin has been disabled!");
    }

    public RocketeerManager getRocketeerManager() {
        return rocketeerManager;
    }
}