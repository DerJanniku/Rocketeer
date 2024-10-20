package org.derjannik.rocketeer;

import org.bukkit.entity.Piglin;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.enchantments.Enchantment;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.enchantments.Enchantment;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.derjannik.rocketeer.ResupplyStation;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.enchantments.Enchantment;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.derjannik.rocketeer.goal.ForgetTargetGoal;
import org.derjannik.rocketeer.goal.MoveToStationGoal;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.Location;

public class Rocketeer {
    private final Piglin mob;
    private final List<ItemStack> rockets = new ArrayList<>();
    private final int maxRockets = 5;
    private final ResupplyStation resupplyStation;
    private final UUID uuid;

    public Rocketeer(Piglin mob, ResupplyStation resupplyStation) {
        this.mob = mob;
        this.resupplyStation = resupplyStation;
        this.uuid = UUID.randomUUID();
    }

    public void enterPanicMode() {
        // Run away from the player
        new BukkitRunnable() {
            @Override
            public void run() {
                mob.getPathfinder().moveTo(findSafeLocation());
                this.cancel();
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }

    private Location findSafeLocation() {
        Location safeLocation = mob.getLocation();
        double maxDistance = 0;

        for (int i = 0; i < 10; i++) {
            Location randomLocation = mob.getLocation().add(Math.random() * 20 - 10, 0, Math.random() * 20 - 10);
            double distance = randomLocation.distance(mob.getLocation());
            if (distance > maxDistance) {
                maxDistance = distance;
                safeLocation = randomLocation;
            }
        }

        return safeLocation;
    }

    public void enterSearchMode() {
        // Search for resupply station
        new BukkitRunnable() {
            @Override
            public void run() {
                if (resupplyStation != null && mob.getLocation().distance(resupplyStation.getLocation()) < 20) {
                    enterRestockMode();
                    this.cancel();
                } else {
                    mob.getPathfinder().moveTo(findNearestResupplyStation());
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }

    private Location findNearestResupplyStation() {
        Location nearestStation = null;
        double nearestDistance = Double.MAX_VALUE;

        for (ResupplyStation station : plugin.getResupplyStations()) {
            double distance = station.getLocation().distance(mob.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestStation = station.getLocation();
            }
        }

        return nearestStation;
    }

    public void enterRestockMode() {
        if (resupplyStation == null) {
            enterSearchMode();
            return;
        }

        // Move to resupply station
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.getLocation().distance(resupplyStation.getLocation()) < 2) {
                    restockRockets();
                    this.cancel();
                } else {
                    mob.getPathfinder().moveTo(resupplyStation.getLocation());
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }

    private void restockRockets() {
        new BukkitRunnable() {
            int rocketsToRestock = maxRockets - rockets.size();

            @Override
            public void run() {
                if (rocketsToRestock <= 0) {
                    enterCombatMode();
                    this.cancel();
                    return;
                }

                rockets.add(new ItemStack(Material.FIREWORK_ROCKET));
                rocketsToRestock--;
            }
        }.runTaskTimer(plugin, 0, 20 * 2); // Restock every 2 seconds
    }

    public void enterCombatMode() {
        if (rockets.isEmpty()) {
            enterSearchMode();
            return;
        }

        // Targeting logic
        mob.setTarget(findNearestPlayer());

        // Shooting logic
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.getTarget() == null || rockets.isEmpty()) {
                    this.cancel();
                    enterSearchMode();
                    return;
                }

                shootRocket();
            }
        }.runTaskTimer(plugin, 0, 20 * 2); // Shoot every 2 seconds
    }

    private void shootRocket() {
        if (rockets.isEmpty()) {
            return;
        }

        ItemStack rocket = rockets.remove(0);
        Firework firework = (Firework) mob.getWorld().spawnEntity(mob.getLocation(), EntityType.FIREWORK);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        firework.setFireworkMeta(meta);
        firework.setVelocity(mob.getLocation().getDirection().multiply(2));
        firework.setShooter(mob);
        firework.detonate();
    }

    private Player findNearestPlayer() {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : mob.getWorld().getPlayers()) {
            double distance = player.getLocation().distance(mob.getLocation());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
        mob.setCustomName(ChatColor.BOLD + "Rocketeer");
        mob.setCustomNameVisible(true);
        mob.setPersistent(true);
        mob .setRemoveWhenFarAway(false);
        if (mob.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            mob.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
        }
        mob.setHealth(40.0);
        if (mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
        }
        if (mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE) != null) {
            mob.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(25.0);
        }
        mob.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        mob.getEquipment().setItemInMainHand(createCrossbow());
        mob.getEquipment().setHelmet(createLeatherArmor(Material.LEATHER_HELMET, ChatColor.DARK_RED));
        mob.getEquipment().setChestplate(createLeatherArmor(Material.LEATHER_CHESTPLATE, ChatColor.DARK_RED));
        mob.getEquipment().setLeggings(createLeatherArmor(Material.LEATHER_LEGGINGS, ChatColor.DARK_RED));
        mob.getEquipment().setBoots(createLeatherArmor(Material.LEATHER_BOOTS, ChatColor.DARK_RED));
        mob.getEquipment().getBoots().addEnchantment(Enchantment.FEATHER_FALLING, 10);
        mob.getEquipment().getBoots().addEnchantment(Enchantment.SOUL_SPEED, 10);
        // Initialize rockets
        for (int i = 0; i < maxRockets; i++) {
            rockets.add(new ItemStack(Material.FIREWORK_ROCKET));
        }

        // Add custom goal logic here
        mob.addGoal(new ForgetTargetGoal(mob));
        mob.addGoal(new MoveToStationGoal(mob));
    }
    }

    public void onDeath() {
        // Add custom death logic here
    }

    public void launchRocket() {
        Location loc = mob.getLocation();
        Firework firework = (Firework) loc.getWorld().spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .with(Type.BALL)
                .withColor(org.bukkit.Color.ORANGE)
                .withFade(org.bukkit.Color.YELLOW)
                .withFlicker()
                .withTrail()
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
        firework.setVelocity(mob.getLocation().getDirection().multiply(2));
        firework.detonate();
        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        mob.getWorld().spawnParticle(Particle.SMOKE, mob.getLocation(), 10);
    }

    public void resupply() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (rockets.size() < maxRockets) {
                    rockets.add(new ItemStack(Material.FIREWORK_ROCKET));
                    mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_CREEPER_HURT, 1.0f, 1.0f);
                } else {
                    this.cancel();
                }
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin("Rocketeer"), 0, 40);
    }

    public @NotNull String getName() {
        return "Rocketeer";
    }

    private ItemStack createCrossbow() {
        ItemStack crossbow = new ItemStack(Material.CROSSBOW);
        CrossbowMeta meta = (CrossbowMeta) crossbow.getItemMeta();
        meta.addEnchant(Enchantment.QUICK_CHARGE, 2, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        meta.setUnbreakable(true);
        crossbow.setItemMeta(meta);
        return crossbow;
    }

    private ItemStack createLeatherArmor(Material material, ChatColor color) {
        ItemStack armor = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setColor(org.bukkit.Color.fromRGB(186, 48, 48));
        meta.addEnchant(Enchantment.PROTECTION, 4, true);
        meta.addEnchant(Enchantment.FEATHER_FALLING, 10, true);
        meta.addEnchant(Enchantment.BLAST_PROTECTION, 15, true);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 15, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        armor.setItemMeta(meta);
        return armor;
    }
}
