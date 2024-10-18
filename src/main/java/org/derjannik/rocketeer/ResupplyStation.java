package org.derjannik.rocketeer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public class RocketeerCommand implements CommandExecutor {

    private final RocketeerPlugin plugin;

    public RocketeerCommand(RocketeerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

    public void resupply(Mob mob) {
        Location closestStation = this.findClosestStation(mob.getLocation(), stations);

        if (closestStation != null) {
            Block block = closestStation.getBlock();
            if (block.getType() == Material.CHEST) {
                ItemStack supplies = new ItemStack(Material.FIREWORK_ROCKET, 64);
                mob.getEquipment().setItemInMainHand(supplies);
                System.out.println("Resupplied " + mob.getName() + " at " + closestStation);
            } else {
                System.out.println("No chest found at " + closestStation);
            }
        } else {
            System.out.println("No closest station found for " + mob.getName());
        }
    }

    private Location findClosestStation(Location startLocation, List<Location> stations) {
        Location closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Location station : stations) {
            double distance = station.distance(startLocation);

            if (closest == null || distance < closestDistance) {
                closest = station;
                closestDistance = distance;
            }
        }

        return closest;
    }
}
