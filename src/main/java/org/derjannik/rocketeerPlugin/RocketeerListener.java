package org.derjannik.rocketeerPlugin;


import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class RocketeerListener implements Listener {
    private final RocketeerPlugin plugin;

    public RocketeerListener(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRocketeerTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Piglin && event.getTarget() instanceof Player) {
            Piglin piglin = (Piglin) event.getEntity();
            if (piglin.getCustomName() != null && piglin.getCustomName().equals(Rocketeer.ROCKETEER_NAME)) {
                Player target = (Player) event.getTarget();
                Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
                if (rocketeer != null) {
                    rocketeer.getBehavior().enterCombatMode(target);
                }
            }
        }
    }

    @EventHandler
    public void onRocketeerDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Piglin) {
            Piglin piglin = (Piglin) event.getEntity();
            if (piglin.getCustomName() != null && piglin.getCustomName().equals(Rocketeer.ROCKETEER_NAME)) {
                Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
                if (rocketeer != null) {
                    if (rocketeer.getBehavior().isRestocking()) {
                        rocketeer.getBehavior().interruptRestock();
                    }
                    if (rocketeer.getRocketCount() == 0) {
                        rocketeer.getBehavior().enterPanicMode();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onRocketeerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Piglin) {
            Piglin piglin = (Piglin) event.getEntity();
            if (piglin.getCustomName() != null && piglin.getCustomName().equals(Rocketeer.ROCKETEER_NAME)) {
                Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
                if (rocketeer != null) {
                    // Drop custom loot
                    event.getDrops().clear(); // Clear default drops
                    event.getDrops().add(new ItemStack(Material.CROSSBOW)); // Add a crossbow drop

                    // Remove the Rocketeer from the plugin's tracking
                    plugin.removeRocketeer(piglin);
                }
            }
        }
    }
}
