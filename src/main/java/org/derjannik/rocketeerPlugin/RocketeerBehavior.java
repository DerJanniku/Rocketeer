package org.derjannik.rocketeerPlugin;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RocketeerBehavior {
    private final Rocketeer rocketeer;
    private final RocketeerPlugin plugin;
    private BukkitRunnable combatTask;
    private BukkitRunnable panicTask;
    private BukkitRunnable restockTask;
    public boolean isRestocking = false;

    public RocketeerBehavior(Rocketeer rocketeer, RocketeerPlugin plugin) {
        this.rocketeer = rocketeer;
        this.plugin = plugin;
    }

    public void enterCombatMode(Player target) {
        if (combatTask != null) {
            combatTask.cancel();
        }

        combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!rocketeer.getEntity().isValid() || target == null || !target.isOnline()) {
                    this.cancel();
                    return;
                }

                // Check if rockets are available
                if (rocketeer.getRocketCount() > 0) {
                    fireRocket(target);
                } else {
                    // Cancel combat and enter restock mode if no rockets
                    this.cancel();
                    enterRestockMode();
                }
            }
        };
        combatTask.runTaskTimer(plugin, 0, 60); // Fire every 3 seconds
    }

    /**
     * Fires a rocket at the specified target player.
     * The method handles Creative mode checks, prevents self-damage, and ensures rockets are fired correctly.
     */
    public void fireRocket(Player target) {
        // Check if there are rockets before proceeding
        if (rocketeer.getRocketCount() <= 0) {
            enterRestockMode(); // Go for restock if no rockets
            return;
        }

        // Check if the player is in Creative mode
        if (target.getGameMode() == GameMode.CREATIVE) {
            return; // Do not attack players in Creative mode
        }

        // Define the start location
        Location startLoc = rocketeer.getEntity().getEyeLocation();

        // Calculate the target location
        Location targetLocation = target.getLocation().add(0, target.getEyeHeight(), 0);

        // Calculate the distance and height difference
        double distance = startLoc.distance(targetLocation);
        double heightDifference = targetLocation.getY() - startLoc.getY();

        // Calculate the horizontal angle (azimuth)
        double azimuth = Math.atan2(targetLocation.getX() - startLoc.getX(), targetLocation.getZ() - startLoc.getZ());

        // Calculate the vertical angle (elevation)
        double elevation = Math.atan2(heightDifference, distance);

        // Adjust for gravity (this value may need tuning)
        double g = 0.08; // Gravity in Minecraft (adjust if necessary)
        double gravityAdjustment = -0.05 * distance; // Adjust based on distance
        elevation += gravityAdjustment;

        // Calculate launch velocity (adjust this value as needed)
        double launchVelocity = 1.5; // Adjust this value to change the speed of the rocket

        // Calculate the velocity vector for the firework
        Vector velocity = new Vector(Math.cos(azimuth) * Math.cos(elevation), Math.sin(elevation), Math.sin(azimuth) * Math.cos(elevation)).multiply(launchVelocity);

        // Play sound and animation to simulate crossbow firing
        rocketeer.getEntity().getWorld().playSound(startLoc, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);

        // Decrement rocket count and fire the rocket
        rocketeer.setRocketCount(rocketeer.getRocketCount() - 1); // Decrement rocket count

        // Check if rockets are now zero after decrementing
        if (rocketeer.getRocketCount() <= 0) {
            enterRestockMode(); // Go for restock if rockets are now 0
            return; // Stop further processing
        }

        // Spawn the firework at the Rocketeer's location to mimic shooting
        Firework firework = rocketeer.getEntity().getWorld().spawn(startLoc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);

        // Set the firework's shooter to avoid self-damage
        firework.setShooter(rocketeer.getEntity());

        // Set the velocity to simulate being shot from the crossbow
        firework.setVelocity(velocity);

        // Optionally, you can add a delay before the firework explodes
        firework.setFireworkMeta(meta);
    }

    /**
     * Checks if the path between the start and target locations is clear.
     */
    private boolean isPathClear(Location start, Location target) {
        // Create a ray from the start location to the target location
        Vector direction = target.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(target);

        for (double i = 0; i < distance; i += 0.5) { // Adjust step size as needed
            Location point = start.clone().add(direction.clone().multiply(i));
            if (!point.getBlock().isPassable()) { // Check if the block is not passable
                return false; // Path is blocked
            }
        }
        return true; // Path is clear
    }

    public Location findNearestResupplyStation() {
        Location rocketeerLoc = rocketeer.getEntity().getLocation();
        World world = rocketeerLoc.getWorld();
        int searchRadius = 20;

        for (int x = -searchRadius; x <= searchRadius; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -searchRadius; z <= searchRadius; z++) {
                    Location checkLoc = rocketeerLoc.clone().add(x, y, z);
                    if (world.getBlockAt(checkLoc).getType() == Material.DECORATED_POT) {
                        return checkLoc;
                    }
                }
            }
        }
        return null;
    }

    public void enterRestockMode() {
        if (rocketeer.getRocketCount() <= 0) { // Ensure we only enter restock mode if rockets are 0
            isRestocking = true;
            // Clear any target and stop combat
            if (combatTask != null) {
                combatTask.cancel();
                combatTask = null;
            }

            Location resupplyStation = findNearestResupplyStation();
            if (resupplyStation != null) {
                rocketeer.getEntity().getPathfinder().moveTo(resupplyStation);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (rocketeer.getEntity().getLocation().distance(resupplyStation) < 2) {
                            restockRockets();
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0, 20); // Check every second
            } else {
                enterPanicMode();
            }
        }
    }

    private void restockRockets() {
        if (restockTask != null) {
            restockTask.cancel();
        }

        restockTask = new BukkitRunnable() {
            int restockedRockets = 0;

            @Override
            public void run() {
                if (restockedRockets < 5 && rocketeer.getEntity().isValid()) {
                    rocketeer.setRocketCount(rocketeer.getRocketCount() + 1);
                    restockedRockets++;
                    rocketeer.playRestockSound();
                } else {
                    isRestocking = false;
                    // Ensure the correct transition out of restock mode
                    if (rocketeer.getRocketCount() > 0) {
                        Player nearestPlayer = findNearestPlayer();
                        if (nearestPlayer != null && nearestPlayer.getGameMode() != GameMode.CREATIVE) {
                            enterCombatMode(nearestPlayer);
                        } else {
                            enterPanicMode(); // Panic if no valid target is found
                        }
                    } else {
                        enterPanicMode();
                    }
                    this.cancel();
                }
            }
        };
        restockTask.runTaskTimer(plugin, 60, 40);
    }

    public void interruptRestock() {
        if (restockTask != null) {
            restockTask.cancel();
        }
        isRestocking = false;
        if (rocketeer.getRocketCount() > 0) {
            enterCombatMode(findNearestPlayer());
        } else {
            enterPanicMode();
        }
    }

    public Player findNearestPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location rocketeerLocation = rocketeer.getEntity().getLocation();

        // Get nearby entities within a 20-block range
        for (org.bukkit.entity.Entity entity : rocketeer.getEntity().getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player player) {
                double distance = player.getLocation().distance(rocketeerLocation);

                // Prioritize players that are on the same Y-level or very close to it
                if (Math.abs(rocketeerLocation.getY() - player.getLocation().getY()) <= 1 && distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
        }

        return nearest;
    }

    public void enterPanicMode() {
        if (panicTask != null) {
            panicTask.cancel();
        }

        panicTask = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks >= 160 || !rocketeer.getEntity().isValid()) { // Panic mode for 8 seconds
                    this.cancel();
                    return;
                }

                // Find the nearest player and flee
                Player nearestPlayer = findNearestPlayer();
                Location fleeLocation;

                if (nearestPlayer != null) {
                    // Calculate the direction away from the player
                    fleeLocation = findFleeLocation(nearestPlayer.getLocation());
                } else {
                    fleeLocation = findFleeLocation(null); // Random movement if no player nearby
                }

                rocketeer.getEntity().getPathfinder().moveTo(fleeLocation);
                ticks += 20; // Increment the tick count
            }
        };
        panicTask.runTaskTimer(plugin, 0, 20); // Update direction every second
    }

    private Location findFleeLocation(Location playerLocation) {
        Location currentLoc = rocketeer.getEntity().getLocation();

        if (playerLocation != null) {
            // Find direction away from the player
            Vector fleeDirection = currentLoc.toVector().subtract(playerLocation.toVector()).normalize().multiply(10);
            return currentLoc.add(fleeDirection);
        } else {
            // Random movement if no player is found
            Vector randomDir = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize().multiply(10);
            return currentLoc.add(randomDir);
        }
    }

    public boolean isRestocking() {
        return isRestocking;
    }
}