
package org.derjannik.rocketeer;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
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
                    sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
                }
            } else if (args.length == 1 && args[0].equalsIgnoreCase("egg")) {
                if (sender instanceof Player) {
                    Player player = (Player) sender;
                    ItemStack egg = new ItemStack(Material.PIGLIN_SPAWN_EGG);
                    ItemMeta meta = egg.getItemMeta();
                    meta.setDisplayName(ChatColor.RED + "Rocketeer");
                    egg.setItemMeta(meta);
                    player.getInventory().addItem(egg);
                    player.sendMessage(ChatColor.GREEN + "Rocketeer spawn egg added to your inventory!");
                } else {
                    sender.sendMessage(ChatColor.RED + "This command can only be run by a player.");
                }
            } else if (args.length == 3) {
                try {
                    double x = Double.parseDouble(args[0]);
                    double y = Double.parseDouble(args[1]);
                    double z = Double.parseDouble(args[2]);
                    Location location = new Location(Bukkit.getWorlds().get(0), x, y, z);
                    spawnRocketeer(location);
                    sender.sendMessage(ChatColor.GREEN + "Rocketeer spawned at (" + x + ", " + y + ", " + z + ")!");
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Invalid coordinates.");
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
        rocketeer.setCustomName(ChatColor.RED + "Rocketeer");
        rocketeer.setCustomNameVisible(true);
        // Set attributes
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
        // Rocket Supply (Belt Mechanic)
        for (int i = 0; i < 5; i++) {
            ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET);
            rocketeer.getEquipment().setItemInOffHand(rocket);
            System.out.println("Initialized rocket " + (i + 1) + " for Rocketeer at " + location);
        }
        // Add custom goal logic here
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
        meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 5, true);
        meta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, 15, true);
        meta.addEnchant(Enchantment.PROTECTION_PROJECTILE, 15, true);
        meta.addEnchant(Enchantment.VANISHING_CURSE, 1, true);
        armor.setItemMeta(meta);
        return armor;
    }
}
