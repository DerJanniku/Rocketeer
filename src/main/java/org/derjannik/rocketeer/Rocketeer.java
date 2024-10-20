package org.derjannik.rocketeer;

import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
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
    private final RocketeerPlugin plugin;

    public Rocketeer(Piglin mob, ResupplyStation resupplyStation, RocketeerPlugin plugin) {
        this.mob = mob;
        this.resupplyStation = resupplyStation;
        this.plugin = plugin;
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

    public void enterRestockMode() {
        if (resupplyStation == null) {
            enterSearchMode();
            return;
        }

        // Move to resupply station
        new BukkitRunnable() {
            @Override
            public void run() {
                if (resupplyStation != null && mob.getLocation().distance(resupplyStation.getLocation()) < 20) {
                    // Restock logic
                    restock();
                    this.cancel();
                } else {
                    mob.getPathfinder().moveTo(resupplyStation.getLocation());
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }

    private void restock() {
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
                if (!rockets.isEmpty()) {
                    shootRocket();
                } else {
                    enterSearchMode();
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Check every second
    }

    private void shootRocket() {
        if (rockets.isEmpty()) {
            return;
        }

        ItemStack rocket = rockets.remove(0);
        Firework firework = (Firework) mob.getWorld().spawnEntity(mob.getLocation(), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = (FireworkMeta) rocket.getItemMeta();
        firework.setFireworkMeta(meta);
        firework.setVelocity(mob.getLocation().getDirection().multiply(2));
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
        }.runTaskTimer(plugin, 0, 40);
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
