
package org.derjannik.rocketeerPlugin;

import org.bukkit.Material;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
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
    public void onRocketeerShoot(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Piglin piglin) {
            Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
            if (rocketeer != null) {
                event.setCancelled(true);
                Player target = rocketeer.getBehavior().findNearestPlayer();
                if (target != null) {
                    rocketeer.getBehavior().fireRocket(target);
                }
            }
        }
    }

    @EventHandler
    public void onRocketeerDamaged(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Piglin piglin) {
            Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
            if (rocketeer != null) {
                rocketeer.getBehavior().handleDamage();
            }
        }
    }

    @EventHandler
    public void onRocketeerDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Piglin piglin) {
            Rocketeer rocketeer = plugin.getRocketeerByEntity(piglin);
            if (rocketeer != null) {
                event.getDrops().clear();
                event.getDrops().add(new ItemStack(Material.CROSSBOW));
                plugin.removeRocketeer(rocketeer);
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (event.getEntity() instanceof Piglin piglin && event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            Player player = event.getEntity().getWorld().getNearbyEntities(piglin.getLocation(), 5, 5, 5)
                    .stream().filter(e -> e instanceof Player).map(e -> (Player) e).findFirst().orElse(null);

            if (player != null) {
                ItemStack mainHandItem = player.getInventory().getItem(EquipmentSlot.HAND);
                ItemStack offHandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

                ItemStack rocketeerEgg = null;
                boolean isInMainHand = false;

                if (isRocketeerEgg(mainHandItem)) {
                    rocketeerEgg = mainHandItem;
                    isInMainHand = true;
                } else if (isRocketeerEgg(offHandItem)) {
                    rocketeerEgg = offHandItem;
                }

                if (rocketeerEgg != null) {
                    Rocketeer rocketeer = new Rocketeer(piglin.getLocation(), plugin);
                    plugin.addRocketeer(rocketeer);
                    piglin.remove();

                    if (isInMainHand) {
                        player.getInventory().getItem(EquipmentSlot.HAND).setAmount(rocketeerEgg.getAmount() - 1);
                    } else {
                        player.getInventory().getItem(EquipmentSlot.OFF_HAND).setAmount(rocketeerEgg.getAmount() - 1);
                    }
                }
            }
        }
    }

    private boolean isRocketeerEgg(ItemStack item) {
        if (item != null && item.getItemMeta() instanceof SpawnEggMeta eggMeta) {
            return eggMeta.getPersistentDataContainer().has(plugin.getRocketKey(), PersistentDataType.STRING) &&
                    "rocketeer_egg".equals(eggMeta.getPersistentDataContainer().get(plugin.getRocketKey(), PersistentDataType.STRING));
        }
        return false;
    }
}
