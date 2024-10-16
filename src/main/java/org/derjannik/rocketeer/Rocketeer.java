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
    public Rocketeer(Piglin mob) {
        this.mob = mob;
        this.uuid = UUID.randomUUID();
        this.resupplyStation = null;

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



    public void onSpawn() {
        // Set attributes
        mob.setCustomName(ChatColor.BOLD + "Rocketeer");
        mob.setCustomNameVisible(true);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
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
        mob.getEquipment().getBoots().addEnchantment(Enchantment.FEATHER_FALLING, 4);
        mob.getEquipment().getBoots().addEnchantment(Enchantment.SOUL_SPEED, 10);
        // Initialize rockets
        for (int i = 0; i < maxRockets; i++) {
            rockets.add(new ItemStack(Material.FIREWORK_ROCKET));
        }
        // Add custom goal logic here
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
}
