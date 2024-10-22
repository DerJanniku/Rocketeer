package org.derjannik.rocketeerPlugin;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RocketeerCommand implements CommandExecutor {

    private final RocketeerPlugin plugin;

    public RocketeerCommand(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        if (args.length == 3) {
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                Location location = new Location(player.getWorld(), x, y, z);

                // Create a new Rocketeer instance
                Rocketeer newRocketeer = new Rocketeer(location, plugin);

                // Register the new Rocketeer with the plugin
                plugin.addRocketeer(newRocketeer);

                player.sendMessage("Rocketeer spawned at the specified location!");
            } catch (NumberFormatException e) {
                player.sendMessage("Invalid coordinates. Usage: /rocketeer [x] [y] [z]");
            }
        } else if (args.length == 1 && args[0].equalsIgnoreCase("egg")) {
            // You can add code here to give the player a spawn egg or handle the egg case
            player.sendMessage("Here is your Rocketeer spawn egg!");
        } else {
            player.sendMessage("Usage: /rocketeer [x] [y] [z] | /rocketeer egg");
        }
        return true;
    }
}
