package org.derjannik.rocketeer;

import org.bukkit.entity.EntitySpawnEvent;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RocketeerListener implements Listener {

    private final RocketeerPlugin plugin;

    public RocketeerListener(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPiglinSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Piglin) {
            // Logic to recognize and manage rocketeer spawns
        }
    }
}
