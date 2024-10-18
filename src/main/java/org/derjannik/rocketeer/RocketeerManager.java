package org.derjannik.rocketeer;

import org.bukkit.Location;
import org.bukkit.entity.Piglin;
import org.derjannik.ro cketeer.ResupplyStation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocketeerManager {
    private final RocketeerPlugin plugin;
    private final Map<UUID, Rocketeer> rocketeers = new HashMap<>();
    private final ResupplyStation resupplyStation;

    public RocketeerManager(RocketeerPlugin plugin) {
        this.plugin = plugin;
        this.resupplyStation = new ResupplyStation(); // Initialize resupply station
    }

    public Rocketeer spawnRocketeer(Location location) {
        Piglin piglin = location.getWorld().spawn(location, Piglin.class);
        Rocketeer rocketeer = new Rocketeer(piglin, resupplyStation);
        rocketeer.onSpawn();
        rocketeers.put(piglin.getUniqueId(), rocketeer);
        return rocketeer;
    }

    // Additional methods for managing rocketeers
}
