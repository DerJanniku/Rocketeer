
package org.derjannik.rocketeer;

import org.bukkit.plugin.java.JavaPlugin;

public class RocketeerPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("Rocketeer plugin has been enabled!");
        // Register commands and events here
        this.getCommand("rocketeer").setExecutor(new RocketeerCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("Rocketeer plugin has been disabled!");
    }
}
