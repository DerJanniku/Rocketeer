package org.derjannik.rocketeerPlugin;

import org.bukkit.Color;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Rocketeer implements Listener {

    public static final Component ROCKETEER_NAME = Component.text("Rocketeer")
            .color(TextColor.color(0xE02F2F))
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false);

    private static final double BASE_HEALTH = 20.0;
    private static final double MAX_HEALTH = 40.0;
    private static final double MOVEMENT_SPEED = 0.3;
    private static final double FOLLOW_RANGE = 25.0;

    private final Piglin entity;
    private final PersistentDataContainer dataContainer;
    private final List<ArmorStand> rocketStands = new ArrayList<>();
    private final RocketeerPlugin plugin;
    private final RocketeerBehavior behavior;

    public Rocketeer(Location location, RocketeerPlugin plugin) {
        this.plugin = plugin;
        this.entity = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);

        this.entity.customName(ROCKETEER_NAME);
        this.entity.setCustomNameVisible(true);

        if (entity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).setBaseValue(MAX_HEALTH);
        }
        entity.setHealth(BASE_HEALTH);

        if (entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)).setBaseValue(MOVEMENT_SPEED);
        }

        if (entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
            Objects.requireNonNull(entity.getAttribute(Attribute.GENERIC_FOLLOW_RANGE)).setBaseValue(FOLLOW_RANGE);
        }

        this.entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false));
        this.entity.setRemoveWhenFarAway(false);
        this.entity.setImmuneToZombification(true);
        this.entity.setBaby(false);
        this.entity.setCanPickupItems(false);

        this.dataContainer = this.entity.getPersistentDataContainer();
        this.dataContainer.set(plugin.getRocketKey(), PersistentDataType.INTEGER, 5); // Initial rocket count

        equipRocketeer();
        spawnHoveringRockets();

        this.behavior = new RocketeerBehavior(this, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public Piglin getEntity() {
        return entity;
    }

    public RocketeerBehavior getBehavior() {
        return behavior;
    }

    public int getRocketCount() {
        Integer rocketCount = dataContainer.get(plugin.getRocketKey(), PersistentDataType.INTEGER);
        return (rocketCount != null) ? rocketCount : 0;
    }

    public void setRocketCount(int count) {
        dataContainer.set(plugin.getRocketKey(), PersistentDataType.INTEGER, count);
        updateHoveringRockets();
    }

    private void equipRocketeer() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta crossbowMeta = crossbow.getItemMeta();
        if (crossbowMeta != null) {
            crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 2, true);
            crossbowMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            crossbowMeta.setUnbreakable(true);
            crossbow.setItemMeta(crossbowMeta);
        }
        entity.getEquipment().setItemInMainHand(crossbow);

        ItemStack tunic = createColoredArmor(Material.LEATHER_CHESTPLATE, Color.fromRGB(0xBA3030), Enchantment.PROJECTILE_PROTECTION, 5);
        ItemStack pants = createColoredArmor(Material.LEATHER_LEGGINGS, Color.fromRGB(0xBA3030), Enchantment.PROTECTION, 15);
        ItemStack boots = createColoredArmor(Material.LEATHER_BOOTS, Color.fromRGB(0xBA3030), Enchantment.FEATHER_FALLING, 10);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        if (bootsMeta != null) {
            bootsMeta.addEnchant(Enchantment.SOUL_SPEED, 10, true);
            boots.setItemMeta(bootsMeta);
        }

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
            meta.setUnbreakable(true);
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

        new BukkitRunnable() {
            double angle = 0;

            @Override
            public void run() {
                if (entity.isDead()) {
                    this.cancel();
                    removeRockets();
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
        rocketStand.getEquipment().setHelmet(new ItemStack(Material.FIREWORK_ROCKET));
        rocketStand.setHeadPose(new EulerAngle(Math.PI / 2, 0, 0));
        return rocketStand;
    }

    private void removeRockets() {
        for (ArmorStand stand : rocketStands) {
            stand.remove();
        }
        rocketStands.clear();
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(entity) || event.getDamager().equals(entity)) {
            // Ignore creative mode players
            if (event.getDamager() instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
                event.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage("Rocketeer ignored your attack.");
                }, 1L);
                return;
            }
            dataContainer.set(plugin.getCombatKey(), PersistentDataType.LONG, System.currentTimeMillis());
        }
    }

    public void playRestockSound() {
        // Play a sound when the Rocketeer restocks its rockets
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 1f);
    }

    private void updateHoveringRockets() {
        int rocketCount = getRocketCount();
        for (int i = 0; i < rocketStands.size(); i++) {
            ArmorStand stand = rocketStands.get(i);
            if (i < rocketCount) {
                stand.getEquipment().setHelmet(new ItemStack(Material.FIREWORK_ROCKET));
            } else {
                stand.getWorld().spawnParticle(Particle.SMOKE, stand.getLocation(), 5);
                stand.getEquipment().setHelmet(null);
            }
        }
    }
}