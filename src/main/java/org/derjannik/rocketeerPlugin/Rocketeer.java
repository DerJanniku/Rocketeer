package org.derjannik.rocketeerPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
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
            .color(TextColor.color(0xE02F2F)) // Updated color to #E02F2F
            .decorate(TextDecoration.BOLD)
            .decoration(TextDecoration.ITALIC, false); // Bold and Not Italic

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
        // Set the custom name using Adventure API
        this.entity.customName(ROCKETEER_NAME);
        this.entity.setCustomNameVisible(true);
        // Set attributes safely
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
        // Fire Resistance Effect
        this.entity.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 999999, 0, false, false));
        // Persistence
        this.entity.setRemoveWhenFarAway(false);
        // Zombification immunity
        this.entity.setImmuneToZombification(true);
        // Not a baby (this is deprecated, but it's fine to use for now)
        this.entity.setBaby(false);
        // Cannot pick up loot
        this.entity.setCanPickupItems(false);
        this.dataContainer = this.entity.getPersistentDataContainer();
        this.dataContainer.set(plugin.getRocketKey(), PersistentDataType.INTEGER, 5); // Store rocket count
        equipRocketeer();
        spawnHoveringRockets();
        this.behavior = new RocketeerBehavior(this, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin); // Register the event listener
        // Check for resupply station within 100 blocks
        Location resupplyStation = findNearestResupplyStation(100);
        if (resupplyStation == null) {
            // Increase search radius to 1000 blocks if no station is found within 100 blocks
            resupplyStation = findNearestResupplyStation(1000);
        }
        if (resupplyStation != null) {
            entity.getPathfinder().moveTo(resupplyStation);
        }
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
        // Crossbow with Quick Charge II, Vanishing Curse I, and unbreakable
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        ItemMeta crossbowMeta = crossbow.getItemMeta();
        if (crossbowMeta != null) {
            crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 2, true);
            crossbowMeta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
            crossbowMeta.setUnbreakable(true);
            crossbow.setItemMeta(crossbowMeta);
        }
        entity.getEquipment().setItemInMainHand(crossbow);

        // Armor: Tunic, Pants, Boots with color #BA3030 and various enchantments
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
            meta.setUnbreakable(true); // All armor is unbreakable
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

    // Method to check if the Piglin is in combat
    public boolean isInCombat() {
        Long lastCombatTime = dataContainer.get(plugin.getCombatKey(), PersistentDataType.LONG);
        if (lastCombatTime != null) {
            long currentTime = System.currentTimeMillis();
            // If the entity was in combat in the last 10 seconds (10000 ms), consider it in combat
            return currentTime - lastCombatTime < 10000;
        }
        return false;
    }

    // Event handler to update combat state when attacked or attacking
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().equals(entity) || event.getDamager().equals(entity)) {
            // Update the combat timestamp in the data container
            dataContainer.set(plugin.getCombatKey(), PersistentDataType.LONG, System.currentTimeMillis());
        }
    }

    // Logic for returning rockets to the belt
    public void returnRocketToBelt() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isInCombat()) {
                    setRocketCount(getRocketCount() + 1);
                    updateHoveringRockets();
                }
            }
        }.runTaskLater(plugin, 160); // Returns rocket after 8 seconds (160 ticks)
    }

    @SuppressWarnings("unused")
    public void loadRocket() {
        int rocketCount = getRocketCount();
        if (rocketCount > 0) {
            setRocketCount(rocketCount - 1);
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 2f);
            entity.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, entity.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            entity.getWorld().spawnParticle(Particle.POOF, entity.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0.1);
        } else {
            enterRestockMode();
        }
    }

    @SuppressWarnings("unused")
    public void playRestockSound() {
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_CREEPER_HURT, 1f, 0f);
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

    public void enterRestockMode() {
        Location resupplyStation = findNearestResupplyStation();

        if (resupplyStation != null) {
            entity.getPathfinder().moveTo(resupplyStation);

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (entity.getLocation().distance(resupplyStation) < 2) {
                        restockRockets();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 20);
        } else {
            behavior.enterPanicMode();
        }
    }

    private Location findNearestResupplyStation() {
        Location rocketeerLoc = entity.getLocation();
        World world = rocketeerLoc.getWorld();
        int searchRadius = 20;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location checkLoc = rocketeerLoc.clone().add(x, y, z);
                    if (world.getBlockAt(checkLoc).getType() == Material.DECORATED_POT) {
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    private void restockRockets() {
        new BukkitRunnable() {
            int restockedRockets = 0;

            @Override
            public void run() {
                if (restockedRockets < 5 && entity.isValid()) {
                    setRocketCount(getRocketCount() + 1);
                    restockedRockets++;
                    playRestockSound();
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 40);
    }
}
