package org.derjannik.rocketeer;

import org.bukkit.entity.EntitySpawnEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class RocketeerListener implements Listener {

    private final RocketeerPlugin plugin;

    public RocketeerListener(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.PIGLIN) {
            // Check if it's a Rocketeer and set up accordingly
        }
    }