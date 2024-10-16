
package org.derjannik.rocketeer;

import org.bukkit.plugin.java.JavaPlugin;

public class RocketeerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Rocketeer plugin has been enabled!");

        // Check if the command is registered
        if (this.getCommand("rocketeer") == null) {
            getLogger().severe("Command 'rocketeer' is not registered!");
        } else {
            this.getCommand("rocketeer").setExecutor(new RocketeerCommand());
            getLogger().info("Command 'rocketeer' has been registered successfully.");
        }

        if (this.getCommand("rocketeer_egg") == null) {
            getLogger().severe("Command 'rocketeer_egg' is not registered!");
        } else {
            getLogger().info("Command 'rocketeer_egg' has been registered successfully.");
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Rocketeer plugin has been disabled!");
    }
}
