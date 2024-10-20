package org.derjannik.rocketeer;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public class RocketeerListener implements Listener {

    private final RocketeerPlugin plugin;

    public RocketeerListener(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntityType() == EntityType.PIGLIN) {
            Piglin piglin = (Piglin) event.getEntity();
            if (isRocketeer(piglin)) {
                piglin.setCustomName("Rocketeer " + piglin.getName());
                piglin.setCustomNameVisible(true);
            }
        }
    }

    private boolean isRocketeer(Piglin piglin) {
        if (piglin.hasMetadata("rocketeer")) {
            System.out.println("Piglin has rocketeer metadata.");
            return true;
        }
        boolean result = plugin.getRocketeerManager().isRocketeer(piglin);
        System.out.println("Is Rocketeer: " + result);
        return result;
    }
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Piglin piglin) {
            if (isRocketeer(piglin)) {
                event.getPlayer().sendMessage("Du interagierst mit einem Rocketeer!");
            }
        }
    } // Stelle sicher, dass hier die Klammer geschlossen ist
}