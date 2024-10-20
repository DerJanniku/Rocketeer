package org.derjannik.rocketeer;

import org.bukkit.plugin.java.JavaPlugin;

public class RocketeerPlugin extends JavaPlugin {

    private RocketeerManager rocketeerManager; // private Deklaration

    @Override
    public void onEnable() {
        this.rocketeerManager = new RocketeerManager(this);
        getCommand("rocketeer").setExecutor(new RocketeerCommand(this));
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);
        getLogger().info("Rocketeer plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rocketeer plugin has been disabled!");
    }

    public RocketeerManager getRocketeerManager() {
        return rocketeerManager; // Kontrollierter Zugriff
    }

    public Object getRocketeerListener() {
        return new RocketeerListener(this);
    }

    public ResupplyStation[] getResupplyStations() {
        return new ResupplyStation[0];
    }
}