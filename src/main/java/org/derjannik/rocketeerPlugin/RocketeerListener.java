package org.derjannik.rocketeerPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

public class RocketeerListener implements Listener {
    private final RocketeerPlugin plugin;

    public RocketeerListener(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRocketeerTarget(EntityTargetLivingEntityEvent event) {
        if (event.getEntity() instanceof Piglin piglin && event.getTarget() instanceof Player target) {
            if (isRocketeer(piglin)) {
                Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
                if (rocketeer != null) {
                    rocketeer.getBehavior().enterCombatMode(target);
                }
            }
        }
    }

    @EventHandler
    public void onRocketeerDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Piglin piglin) {
            if (isRocketeer(piglin)) {
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
        if (event.getEntity() instanceof Piglin piglin) {
            if (isRocketeer(piglin)) {
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

    /**
     * Utility method to check if a Piglin is a Rocketeer by comparing its custom name.
     * Uses Adventure's Component API to handle the name check.
     *
     * @param piglin The Piglin entity to check.
     * @return True if the Piglin is a Rocketeer, false otherwise.
     */
    private boolean isRocketeer(Piglin piglin) {
        // Retrieve the custom name as a Component and compare it to the Rocketeer name
        Component customName = piglin.customName();
        return customName != null && PlainTextComponentSerializer.plainText().serialize(customName)
                .equals(PlainTextComponentSerializer.plainText().serialize(Rocketeer.ROCKETEER_NAME));
    }
}
