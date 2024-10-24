package org.derjannik.rocketeerPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.persistence.PersistentDataType;

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
                    // Custom loot handling
                    event.getDrops().clear();
                    event.getDrops().add(new ItemStack(Material.CROSSBOW));

                    // Remove the Rocketeer from tracking
                    plugin.removeRocketeer(rocketeer);
                }
            }
        }
    }

    /**
     * Handle creature spawn event to detect if a Piglin was spawned via a custom Rocketeer spawn egg.
     */
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Check if the entity spawned is a Piglin and if the spawn reason was a spawn egg
        if (event.getEntity() instanceof Piglin piglin && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            Player player = event.getEntity().getWorld().getNearbyEntities(piglin.getLocation(), 5, 5, 5)
                    .stream().filter(e -> e instanceof Player).map(e -> (Player) e).findFirst().orElse(null);

            if (player != null) {
                // Check the player's main hand or offhand for the custom spawn egg
                ItemStack itemInHand = player.getInventory().getItem(EquipmentSlot.HAND);
                boolean isInMainHand = true; // Track if it's in the main hand or off-hand

                if (itemInHand == null || !(itemInHand.getItemMeta() instanceof SpawnEggMeta)) {
                    itemInHand = player.getInventory().getItem(EquipmentSlot.OFF_HAND);
                    isInMainHand = false;
                }

                if (itemInHand != null && itemInHand.getItemMeta() instanceof SpawnEggMeta eggMeta) {
                    // Check if the spawn egg contains the custom "rocketeer_egg" tag
                    if (eggMeta.getPersistentDataContainer().has(plugin.getRocketKey(), PersistentDataType.STRING)) {
                        String eggType = eggMeta.getPersistentDataContainer().get(plugin.getRocketKey(), PersistentDataType.STRING);
                        if ("rocketeer_egg".equals(eggType)) {
                            // Convert the Piglin into a Rocketeer
                            Rocketeer rocketeer = new Rocketeer(piglin.getLocation(), plugin);

                            // Register the new Rocketeer
                            plugin.addRocketeer(rocketeer);

                            // Remove the original Piglin (since the Rocketeer is created)
                            piglin.remove();

                            // Remove one spawn egg from the player's hand
                            if (isInMainHand) {
                                player.getInventory().getItem(EquipmentSlot.HAND).setAmount(itemInHand.getAmount() - 1);
                            } else {
                                player.getInventory().getItem(EquipmentSlot.OFF_HAND).setAmount(itemInHand.getAmount() - 1);
                            }
                        }
                    }
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
