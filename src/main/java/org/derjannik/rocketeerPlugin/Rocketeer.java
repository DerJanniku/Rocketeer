package org.derjannik.rocketeerPlugin;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;

public class Rocketeer {
    public static final String ROCKETEER_NAME = ChatColor.RED + "" + ChatColor.BOLD + "Rocketeer";
    private static final double BASE_HEALTH = 20.0;
    private static final double MOVEMENT_SPEED = 0.3;

    private final Piglin entity;
    private final PersistentDataContainer dataContainer;
    private final List<ArmorStand> rocketStands = new ArrayList<>();
    private final RocketeerPlugin plugin;
    private final RocketeerBehavior behavior;

    public Rocketeer(Location location, RocketeerPlugin plugin) {
        this.plugin = plugin;
        this.entity = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);
        this.entity.setCustomName(ROCKETEER_NAME);
        this.entity.setCustomNameVisible(true);
        this.entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(BASE_HEALTH);
        this.entity.setHealth(BASE_HEALTH);
        this.entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(MOVEMENT_SPEED);

        this.dataContainer = this.entity.getPersistentDataContainer();
        this.dataContainer.set(plugin.getRocketKey(), PersistentDataType.INTEGER, 5);

        equipRocketeer();
        spawnHoveringRockets();

        this.behavior = new RocketeerBehavior(this, plugin);
    }

    public Piglin getEntity() {
        return entity;
    }

    public RocketeerBehavior getBehavior() {
        return behavior;
    }

    public int getRocketCount() {
        return dataContainer.get(plugin.getRocketKey(), PersistentDataType.INTEGER);
    }

    public void setRocketCount(int count) {
        dataContainer.set(plugin.getRocketKey(), PersistentDataType.INTEGER, count);
        updateHoveringRockets();
    }

    private void equipRocketeer() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        crossbow.addEnchantment(Enchantment.QUICK_CHARGE, 2);
        crossbow.addEnchantment(Enchantment.VANISHING_CURSE, 1);
        entity.getEquipment().setItemInMainHand(crossbow);

        ItemStack tunic = createColoredArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(0xBA3030), Enchantment.BLAST_PROTECTION, 5);
        ItemStack pants = createColoredArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(0xBA3030), null, 0);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, Color.fromRGB(0xBA3030), Enchantment.FEATHER_FALLING, 10);

        entity.getEquipment().setChestplate(tunic);
        entity.getEquipment().setLeggings(pants);
        entity.getEquipment().setBoots(boots);
    }

    private ItemStack createColoredArmor(Material material, Color color, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(color);
            if (enchantment != null) {
                meta.addEnchant(enchantment, level, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private void spawnHoveringRockets() {
        for (int i = 0; i < 5; i++) {
            Location rocketLoc = entity.getLocation().add(
                    Math.cos(i * 72 * Math.PI / 180) * 1.5,
                    1.5,
                    Math.sin(i * 72 * Math.PI / 180) * 1.5
            );
            ArmorStand rocketStand = createRocketStand(rocketLoc);
            rocketStands.add(rocketStand);
        }

        // Start the task to make rockets hover
        new BukkitRunnable() {
            double angle = 0;
            @Override
            public void run() {
                if (entity.isDead()) {
                    this.cancel();
                    removeRockets();
                    return;
                }

                angle += 0.1;
                for (int i = 0; i < rocketStands.size(); i++) {
                    ArmorStand stand = rocketStands.get(i);
                    Location newLoc = entity.getLocation().add(
                            Math.cos(angle + i * 72 * Math.PI / 180) * 1.5,
                            1.5 + Math.sin(angle) * 0.1,
                            Math.sin(angle + i * 72 * Math.PI / 180) * 1.5
                    );
                    stand.teleport(newLoc);

                    // Add firework particle effect
                    entity.getWorld().spawnParticle(Particle.FIREWORK, newLoc, 1, 0.1, 0.1, 0.1, 0);
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private ArmorStand createRocketStand(Location location) {
        ArmorStand rocketStand = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
        rocketStand.setVisible(false);
        rocketStand.setGravity(false);
        rocketStand.setSmall(true);
        rocketStand.setMarker(true);
        rocketStand.setHelmet(new ItemStack(Material.FIREWORK_ROCKET));
        rocketStand.setHeadPose(new EulerAngle(Math.PI / 2, 0, 0));
        return rocketStand;
    }

    private void removeRockets() {
        for (ArmorStand stand : rocketStands) {
            stand.remove();
        }
        rocketStands.clear();
    }

    public void loadRocket() {
        if (getRocketCount() > 0) {
            setRocketCount(getRocketCount() - 1);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 2f);
            entity.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            entity.getWorld().spawnParticle(Particle.POOF, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
        }
    }

    public void playRestockSound() {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 0f);
    }

    private void updateHoveringRockets() {
        int rocketCount = getRocketCount();
        for (int i = 0; i < rocketStands.size(); i++) {
            ArmorStand stand = rocketStands.get(i);
            if (i < rocketCount) {
                stand.setHelmet(new ItemStack(Material.FIREWORK_ROCKET));
            } else {
                stand.setHelmet(null);
            }
        }
    }
}
