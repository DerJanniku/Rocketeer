package org.derjannik.rocketeer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Rocketeer extends JavaPlugin {

    private enum State {
        ATTACK, RELOAD, PANIC
    }

    @Override
    public void onEnable() {
        if (getCommand("rocketeer") != null) {
            getCommand("rocketeer").setExecutor((sender, command, label, args) -> {
                if (sender instanceof Player player) {
                    // Check if the subcommand is "egg"
                    if (args.length == 1 && args[0].equalsIgnoreCase("egg")) {
                        player.getInventory().addItem(new ItemStack(Material.PIGLIN_SPAWN_EGG, 1));
                        player.sendMessage("Rocketeer spawn egg added to inventory!");
                        return true;
                    }

                    // Handle the Rocketeer spawn command with coordinates
                    Location location = player.getLocation();
                    if (args.length == 3) {
                        try {
                            double x = Double.parseDouble(args[0]);
                            double y = Double.parseDouble(args[1]);
                            double z = Double.parseDouble(args[2]);
                            location = new Location(player.getWorld(), x, y, z);
                        } catch (NumberFormatException e) {
                            player.sendMessage("Invalid coordinates!");
                            return true;
                        }
                    }

                    spawnRocketeer(location);
                    player.sendMessage("Rocketeer spawned!");
                }
                return true;
            });
        } else {
            getLogger().warning("The 'rocketeer' command is not registered in plugin.yml!");
        }
    }

    private void spawnRocketeer(Location location) {
        Piglin rocketeer = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);
        if (rocketeer != null) {
            rocketeer.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
            rocketeer.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
            rocketeer.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
            rocketeer.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));
            rocketeer.getEquipment().setItemInMainHand(new ItemStack(Material.FIREWORK_ROCKET));

            // Updated Spawn effect with more detailed parameters
            rocketeer.getWorld().spawnParticle(
                    Particle.FIREWORK,           // Updated particle type
                    rocketeer.getLocation(),
                    20,                          // Number of particles
                    0.5, 0.5, 0.5,               // Offset for particle spread
                    0.1                          // Speed of particles
            );

            new BukkitRunnable() {
                private State state = State.ATTACK;
                private int rockets = 5;
                private Player target;

                @Override
                public void run() {
                    if (rocketeer.isDead()) {
                        cancel();
                        return;
                    }

                    switch (state) {
                        case ATTACK:
                            target = getNearestPlayer(rocketeer);
                            if (target != null && rockets > 0) {
                                Firework firework = (Firework) rocketeer.getWorld().spawnEntity(rocketeer.getLocation(), EntityType.FIREWORK_ROCKET);
                                firework.setVelocity(target.getLocation().toVector().subtract(rocketeer.getLocation().toVector()).normalize());
                                rockets--;
                                rocketeer.getWorld().spawnParticle(Particle.LARGE_SMOKE, rocketeer.getLocation(), 10); // Corrected particle type
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
                            rocketeer.setAI(false);
                            rocketeer.setVelocity(rocketeer.getLocation().getDirection().multiply(-1));
                            if (findResupplyStation(rocketeer.getLocation())) {
                                state = State.RELOAD;
                            }
                            break;
                    }
                }
            }.runTaskTimer(this, 0, 20);
        }
    }

    private Player getNearestPlayer(Piglin rocketeer) {
        Player nearest = null ;
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
}