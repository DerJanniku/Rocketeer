
package org.derjannik.rocketeer;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RocketeerEggCommand implements CommandExecutor {

    private final RocketeerPlugin plugin;

    public RocketeerEggCommand(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            giveRocketeerEgg(player);
            return true;
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
            return false;
        }
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
