package org.derjannik.rocketeerPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocketeerPlugin extends JavaPlugin {

    private NamespacedKey rocketKey;
    private NamespacedKey combatKey;  // Added combatKey

    // Store Rocketeer instances using their entity's UUID as the key
    private final Map<UUID, Rocketeer> rocketeerMap = new HashMap<>();

    @Override
    public void onEnable() {
        // Initialize the NamespacedKey for storing rocket data
        this.rocketKey = new NamespacedKey(this, "rocket_count");
        // Initialize the NamespacedKey for storing combat data
        this.combatKey = new NamespacedKey(this, "combat_key");  // Added this line
        // Register the command "rocketeer" and set its executor
        PluginCommand rocketeerCommand = getCommand("rocketeer");
        if (rocketeerCommand != null) {
            rocketeerCommand.setExecutor(new RocketeerCommand(this));
        } else {
            getLogger().severe("Failed to register 'rocketeer' command. Is it properly defined in plugin.yml?");
        }
        // Register the listener for events
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);
        // Log enabling of the plugin
        getLogger().info("RocketeerPlugin has been enabled!");
        // Ensure Rocketeer instances are correctly managed
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onEntityDeath(EntityDeathEvent event) {
                if (event.getEntity() instanceof Piglin) {
                    Rocketeer rocketeer = getRocketeerByEntity(event.getEntity());
                    if (rocketeer != null) {
                        removeRocketeer(rocketeer);
                    }
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        // Cleanup or saving data if needed
        getLogger().info("RocketeerPlugin has been disabled!");
    }

    /**
     * Get the NamespacedKey used for managing rocket data in PersistentDataContainer.
     *
     * @return NamespacedKey for rocket data.
     */
    public NamespacedKey getRocketKey() {
        return rocketKey;
    }

    /**
     * Get the NamespacedKey used for managing combat data in PersistentDataContainer.
     *
     * @return NamespacedKey for combat data.
     */
    public NamespacedKey getCombatKey() {  // Added this method
        return combatKey;
    }

    /**
     * Add a Rocketeer to the managed collection.
     *
     * @param rocketeer The Rocketeer instance to add.
     */
    public void addRocketeer(Rocketeer rocketeer) {
        rocketeerMap.put(rocketeer.getEntity().getUniqueId(), rocketeer);
    }

    /**
     * Get a Rocketeer by its associated entity.
     *
     * @param entity The entity to find a Rocketeer for.
     * @return The Rocketeer instance if it exists, null otherwise.
     */
    public Rocketeer getRocketeerByEntity(Entity entity) {
        if (entity instanceof Piglin) {
            return rocketeerMap.get(entity.getUniqueId());
        }
        return null;
    }

    /**
     * Remove a Rocketeer from the managed collection.
     *
     * @param rocketeer The Rocketeer instance to remove.
     */
    public void removeRocketeer(Rocketeer rocketeer) {
        rocketeerMap.remove(rocketeer.getEntity().getUniqueId());
    }
}
