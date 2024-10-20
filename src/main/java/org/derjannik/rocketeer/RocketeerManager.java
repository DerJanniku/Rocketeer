
package org.derjannik.rocketeer;

import org.bukkit.Location;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.metadata.FixedMetadataValue;

public class RocketeerManager {

    private final RocketeerPlugin plugin;

    public RocketeerManager(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    public void spawnRocketeer(Location location) {
        World world = location.getWorld();
        if (world != null) {
            Piglin piglin = (Piglin) world.spawnEntity(location, EntityType.PIGLIN);
            piglin.setCustomName("Rocketeer");
            piglin.setCustomNameVisible(true);
            piglin.setCollidable(false);
            piglin.setPersistent(true);
            piglin.setHealth(20);
            piglin.setRemoveWhenFarAway(false);
            piglin.setTarget(null);
            piglin.setTarget(null);
            piglin.setAI(false);
            piglin.setCollidable(false);
            piglin.setRemoveWhenFarAway(false);
            piglin.setCollidable(false);
            piglin.setRemoveWhenFarAway(false);
            piglin.setCollidable(false);
            piglin.setRemoveWhenFarAway(false);
            piglin.setCollidable(false);
            piglin.setRemoveWhenFarAway(false);
        }
    }
    public boolean isRocketeer(Piglin piglin) {
        return piglin.hasMetadata("rocketeer");
    }
    public void setRocketeer(Piglin piglin) {
        piglin.setMetadata("rocketeer", new FixedMetadataValue(plugin, true));
    }
    public void unsetRocketeer(Piglin piglin) {
        piglin.removeMetadata("rocketeer", plugin);
    }
}