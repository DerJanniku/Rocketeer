package org.derjannik.rocketeer;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RocketeerCommand implements CommandExecutor {

    private final RocketeerPlugin plugin;

    public RocketeerCommand(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

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
                    giveRocketeerEgg(player);
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
        plugin.getRocketeerManager().spawnRocketeer(location);
    }

    private void giveRocketeerEgg(Player player) {
        ItemStack spawnEgg = new ItemStack(Material.PIGLIN_SPAWN_EGG);
        ItemMeta meta = spawnEgg.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Rocketeer");
        spawnEgg.setItemMeta(meta);
        player.getInventory().addItem(spawnEgg);
        player.sendMessage(ChatColor.GREEN + "Rocketeer spawn egg added to your inventory!");
    }
}
