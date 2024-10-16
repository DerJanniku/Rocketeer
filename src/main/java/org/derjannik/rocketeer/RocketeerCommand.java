package org.derjannik.rocketeer;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Piglin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class RocketeerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rocketeer")) {
            if (args.length == 0) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    spawnRocketeer(player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Rocketeer spawned!");
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                }
            } else if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);
                    Location location = new Location(((Player) sender).getWorld(), x, y, z);
                    spawnRocketeer(location);
                    sender.sendMessage(ChatColor.GREEN + "Rocketeer spawned at " + x + ", " + y + ", " + z + "!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please provide valid numbers.");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("egg")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack spawnEgg = new ItemStack(Material.PIGLIN_SPAWN_EGG);
                    ItemMeta meta = spawnEgg.getItemMeta();
                    meta.setDisplayName(ChatColor.RED + "Rocketeer");
                    spawnEgg.setItemMeta(meta);
                    player.getInventory().addItem(spawnEgg);
                    player.sendMessage(ChatColor.GREEN + "Rocketeer spawn egg added to your inventory!");
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /rocketeer [x] [y] [z] or /rocketeer egg");
            }
            return true;
        }
        return false;
    }

    private void spawnRocketeer(Location location) {
        Piglin rocketeer = location.getWorld().spawn(location, Piglin.class);
        rocketeer.setCustomName(ChatColor.BOLD + "Rocketeer");
        rocketeer.setCustomNameVisible(true);
        rocketeer.setPersistent(true);
        rocketeer.setRemoveWhenFarAway(false);
        rocketeer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0);
        rocketeer.setHealth(40.0);
        rocketeer.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3);
        rocketeer.getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(25.0);
        rocketeer.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 1, false, false));
        rocketeer.getEquipment().setItemInMainHand(createCrossbow());
        rocketeer.getEquipment().setHelmet(createLeatherArmor(Material.LEATHER_HELMET, ChatColor.DARK_RED));
        rocketeer.getEquipment().setChestplate(createLeatherArmor(Material.LEATHER_CHESTPLATE, ChatColor.DARK_RED));
        rocketeer.getEquipment().setLeggings(createLeatherArmor(Material.LEATHER_LEGGINGS, ChatColor.DARK_RED));
        rocketeer.getEquipment().setBoots(createLeatherArmor(Material.LEATHER_BOOTS, ChatColor.DARK_RED));
        rocketeer.getEquipment().getBoots().addEnchantment(Enchantment.FEATHER_FALLING, 10);
        rocketeer.getEquipment().getBoots().addEnchantment(Enchantment.SOUL_SPEED, 10);

        // Initialize rockets in the off-hand
        for (int i = 0; i < 5; i++) {
            ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET);
            rocketeer.getEquipment(). setItemInOffHand(rocket);
            System.out.println("Initialized rocket " + (i + 1) + " for Rocketeer at " + location);
        }

        // Add custom goal logic here

        // BukkitRunnable for Rocketeer behavior
        new BukkitRunnable() {
            private State state = State.ATTACK;
            private int rockets = 5;
            private Player target;

            @Override
            public void run() {
                if (rocketeer.isDead()) {
                    cancel(); // Stop the task if the Rocketeer is dead
                    return;
                }

                switch (state) {
                    case ATTACK:
                        target = getNearestPlayer(rocketeer);
                        if (target != null && rockets > 0) {
                            Firework firework = (Firework) rocketeer.getWorld().spawnEntity(rocketeer.getLocation(), EntityType.FIREWORK_ROCKET);
                            firework.setVelocity(target.getLocation().toVector().subtract(rocketeer.getLocation().toVector()).normalize());
                            rockets--;
                            rocketeer.getWorld().spawnParticle(Particle.LARGE_SMOKE, rocketeer.getLocation(), 10);
                            rocketeer.getWorld().playSound(rocketeer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        } else if (rockets == 0) {
                            state = State.RELOAD;
                        }
                        break;

                    case RELOAD:
                        if (findResupplyStation(rocketeer.getLocation())) {
                            rockets = 5;
                            state = State.ATTACK;
                            rocketeer.getWorld().playSound(rocketeer.getLocation(), Sound.ENTITY_CREEPER_HURT, 1, 1);
                        } else {
                            state = State.PANIC;
                        }
                        break;

                    case PANIC:
                        rocketeer.setAI(false); // Temporarily disable AI to make the Rocketeer retreat
                        rocketeer.setVelocity(rocketeer.getLocation().getDirection().multiply(-1)); // Move the Rocketeer backwards
                        if (findResupplyStation(rocketeer.getLocation())) {
                            state = State.RELOAD;
                        }
                        break;
                }
            }
        }.runTaskTimer(null, 0L, 20L); // Schedule the task with a delay of 1 second (20 ticks)
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
        meta.setColor(Color.fromRGB(186, 48, 48)); // Dark red color
        meta.addEnchant(Enchantment.PROTECTION, 5, true);
        meta.addEnchant(Enchantment.BLAST_PROTECTION, 15, true);
        meta.addEnchant(Enchantment.PROJECTILE_PROTECTION, 15, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        armor.setItemMeta(meta);
        return armor;
    }

    private Player getNearestPlayer(Piglin rocketeer) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player player : Bukkit.getOnlinePlayers()) {
            double distance = player.getLocation().distance(rocketeer.getLocation());
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean findResupplyStation(Location location) {
        for (int x = -20; x <= 20; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -20; z <= 20; z++) {
                    Location blockLocation = new Location(location.getWorld(), location.getBlockX() + x, location.getBlockY() + y, location.getBlockZ() + z);
                    Block block = blockLocation.getBlock();
                    if (block.getType() == Material.CHEST) {
                        BlockState state = block.getState();
                        if (state instanceof Chest chest) {
                            if (chest.getInventory().contains(Material.ARROW)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private enum State {
        ATTACK, RELOAD, PANIC
    }
}