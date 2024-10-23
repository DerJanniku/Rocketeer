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
    private BukkitRunnable restockTask;
    private BukkitRunnable panicTask;
    public boolean isRestocking = false;

    public RocketeerBehavior(Rocketeer rocketeer, RocketeerPlugin plugin) {
        this.rocketeer = rocketeer;
        this.plugin = plugin;
    }

    /**
     * Enters combat mode and repeatedly fires rockets at the nearest player.
     */
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
     * Fires a rocket directly at the specified player's current location.
     * The rocket will explode on the player's exact coordinates.
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

        // Calculate the target location (the explosion will happen at this location)
        Location targetLocation = target.getLocation().add(0, target.getEyeHeight(), 0);

        // Play sound and animation to simulate crossbow firing
        rocketeer.getEntity().getWorld().playSound(startLoc, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);

        // Decrement rocket count and fire the rocket
        rocketeer.setRocketCount(rocketeer.getRocketCount() - 1); // Decrement rocket count

        // Check if rockets are now zero after decrementing
        if (rocketeer.getRocketCount() <= 0) {
            enterRestockMode(); // Go for restock if rockets are now 0
            return; // Stop further processing
        }

        // Spawn the firework at the target location
        Firework firework = target.getWorld().spawn(targetLocation, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);

        // Set the firework's shooter to avoid self-damage
        firework.setShooter(rocketeer.getEntity());

        // Schedule the firework to explode at the target location
        new BukkitRunnable() {
            @Override
            public void run() {
                if (firework.isValid()) {
                    firework.detonate(); // Explode the firework at the target location
                    firework.getWorld().playSound(targetLocation, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
                }
            }
        }.runTaskLater(plugin, 20); // Delay before explosion (20 ticks = 1 second)
    }

    /**
     * Finds the nearest resupply station for the Rocketeer.
     */
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

    /**
     * Enters restock mode if rockets are depleted. The Rocketeer moves to the nearest resupply station.
     */
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
                enterPanicMode(); // Enter panic if no resupply station is found
            }
        }
    }

    /**
     * Handles the rocket restocking process, refilling the Rocketeer with up to 5 rockets.
     */
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

                    // Add visual or sound cues to indicate restocking
                    rocketeer.getEntity().getWorld().playSound(rocketeer.getEntity().getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
                } else {
                    isRestocking = false;

                    // After restocking, find a player to re-enter combat mode
                    Player nearestPlayer = findNearestPlayer();
                    if (nearestPlayer != null && nearestPlayer.getGameMode() != GameMode.CREATIVE) {
                        enterCombatMode(nearestPlayer);
                    } else {
                        enterPanicMode(); // Panic if no valid target is found
                    }
                    this.cancel();
                }
            }
        };
        restockTask.runTaskTimer(plugin, 60, 40);
    }

    /**
     * Interrupts the restocking process and returns the Rocketeer to combat or panic mode.
     */
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

    /**
     * Checks if the Rocketeer is currently restocking.
     */
    public boolean isRestocking() {
        return isRestocking;
    }

    /**
     * Finds the nearest player within a 20-block range of the Rocketeer.
     */
    public Player findNearestPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location rocketeerLocation = rocketeer.getEntity().getLocation();

        // Get nearby entities within a 20-block range
        for (org.bukkit.entity.Entity entity : rocketeer.getEntity().getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player player) {
                double distance = player.getLocation().distance(rocketeerLocation);

                if (distance < nearestDistance) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    /**
     * Enters panic mode, causing the Rocketeer to flee from the nearest player or move randomly.
     */
    public void enterPanicMode() {
        if (panicTask != null) {
            panicTask.cancel();
        }

        panicTask = new BukkitRunnable() {
            @Override
            public void run() {
                Player nearestPlayer = findNearestPlayer();
                Location fleeLocation;

                if (nearestPlayer != null) {
                    // Calculate the direction away from the player
                    fleeLocation = findFleeLocation(nearestPlayer.getLocation());
                } else {
                    fleeLocation = findFleeLocation(null); // Random movement if no player nearby
                }

                rocketeer.getEntity().getPathfinder().moveTo(fleeLocation);
            }
        };
        panicTask.runTaskTimer(plugin, 0, 20); // Update direction every second
    }

    /**
     * Finds a location to flee to, away from the nearest player or random if no player is nearby.
     */
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
}
