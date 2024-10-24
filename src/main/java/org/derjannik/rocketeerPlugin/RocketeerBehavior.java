
package org.derjannik.rocketeerPlugin;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class RocketeerBehavior {
    private final Rocketeer rocketeer;
    private final RocketeerPlugin plugin;
    private BukkitRunnable combatTask;
    private BukkitRunnable panicTask;
    private BukkitRunnable restockTask;
    private BukkitRunnable rocketCheckTask;
    private boolean isRestocking = false;
    private Location knownResupplyStation;
    private static final int NORMAL_SEARCH_RADIUS = 100;
    private static final int EXTENDED_SEARCH_RADIUS = 1000;
    private static final int ROCKET_CAPACITY = 5;
    private static final int ROCKET_COOLDOWN = 60; // 3 seconds cooldown
    private long lastRocketFiredTime = 0;

    public RocketeerBehavior(Rocketeer rocketeer, RocketeerPlugin plugin) {
        this.rocketeer = rocketeer;
        this.plugin = plugin;
        this.knownResupplyStation = findNearestResupplyStation(NORMAL_SEARCH_RADIUS);
        startRocketCheck();
    }

    private void startRocketCheck() {
        rocketCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (rocketeer.getRocketCount() < 1 && !isRestocking) {
                    if (combatTask != null) {
                        combatTask.cancel();
                    }
                    enterRestockMode();
                }
            }
        };
        rocketCheckTask.runTaskTimer(plugin, 0, 20); // Check every second
    }

    public void enterCombatMode(Player target) {
        if (combatTask != null) {
            combatTask.cancel();
        }

        combatTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!rocketeer.getEntity().isValid() || target == null || !target.isOnline() || target.getGameMode() == GameMode.CREATIVE) {
                    this.cancel();
                    enterIdleMode();
                    return;
                }

                if (rocketeer.getRocketCount() < 1) {
                    this.cancel();
                    enterRestockMode();
                    return;
                }

                if (rocketeer.getEntity().getLocation().distance(target.getLocation()) > 20) {
                    this.cancel();
                    enterIdleMode();
                    return;
                }

                fireRocket(target);
            }
        };
        combatTask.runTaskTimer(plugin, 0, 20); // Check every second
    }

    public void fireRocket(Player target) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRocketFiredTime < ROCKET_COOLDOWN * 50) {
            return; // Still on cooldown
        }

        if (rocketeer.getRocketCount() < 1) {
            enterRestockMode();
            return;
        }

        Location startLoc = rocketeer.getEntity().getEyeLocation();
        Location targetLocation = target.getLocation().add(0, target.getEyeHeight(), 0);

        Vector velocity = calculateFireworkVelocity(startLoc, targetLocation, 1.8);

        rocketeer.getEntity().getWorld().playSound(startLoc, Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.0f);
        rocketeer.setRocketCount(rocketeer.getRocketCount() - 1);

        Firework firework = rocketeer.getEntity().getWorld().spawn(startLoc, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.RED)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);
        firework.setShooter(rocketeer.getEntity());
        firework.setVelocity(velocity);

        lastRocketFiredTime = currentTime;
    }

    public void enterRestockMode() {
        isRestocking = true;
        if (combatTask != null) combatTask.cancel();

        if (knownResupplyStation != null) {
            rocketeer.getEntity().getPathfinder().moveTo(knownResupplyStation);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (rocketeer.getEntity().getLocation().distance(knownResupplyStation) < 2) {
                        restockRockets();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 20);
        } else {
            searchForStation(NORMAL_SEARCH_RADIUS);
        }
    }

    private void restockRockets() {
        if (restockTask != null) restockTask.cancel();

        restockTask = new BukkitRunnable() {
            int restockedRockets = 0;

            @Override
            public void run() {
                if (restockedRockets < ROCKET_CAPACITY && rocketeer.getEntity().isValid()) {
                    if (rocketeer.getEntity().getLocation().distance(knownResupplyStation) > 2) {
                        this.cancel();
                        enterIdleMode();
                        return;
                    }

                    rocketeer.setRocketCount(rocketeer.getRocketCount() + 1);
                    restockedRockets++;
                    playRestockEffects();
                } else {
                    isRestocking = false;
                    this.cancel();
                    enterIdleMode();
                }
            }
        };
        restockTask.runTaskTimer(plugin, 60, 40); // Rockets are restocked every 2 seconds
    }

    private void enterIdleMode() {
        wanderAround();
    }

    private void wanderAround() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!rocketeer.getEntity().isValid()) {
                    this.cancel();
                    return;
                }

                Location currentLoc = rocketeer.getEntity().getLocation();
                Random random = new Random();
                double x = random.nextDouble() * 20 - 10;
                double z = random.nextDouble() * 20 - 10;
                Location targetLoc = currentLoc.clone().add(x, 0, z);

                rocketeer.getEntity().getPathfinder().moveTo(targetLoc);
            }
        }.runTaskTimer(plugin, 0, 100); // Wander every 5 seconds
    }

    private void searchForStation(int radius) {
        new BukkitRunnable() {
            @Override
            public void run() {
                knownResupplyStation = findNearestResupplyStation(radius);
                if (knownResupplyStation != null) {
                    this.cancel();
                    enterRestockMode();
                } else if (radius == NORMAL_SEARCH_RADIUS) {
                    searchForStation(EXTENDED_SEARCH_RADIUS);
                } else {
                    enterIdleMode();
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Search every second
    }

    private void playRestockEffects() {
        Location loc = rocketeer.getEntity().getLocation();
        rocketeer.getEntity().getWorld().playSound(loc, Sound.ENTITY_CREEPER_HURT, 1.0f, 0.5f);
        rocketeer.getEntity().getWorld().spawnParticle(Particle.SMOKE, loc, 10, 0.5, 0.5, 0.5, 0.1);
    }

    private Vector calculateFireworkVelocity(Location startLoc, Location targetLocation, double speed) {
        Vector direction = targetLocation.toVector().subtract(startLoc.toVector());
        double distance = startLoc.distance(targetLocation);
        double gravityCompensation = 0.04 * distance;
        direction.setY(direction.getY() + gravityCompensation);
        return direction.normalize().multiply(speed);
    }


    public Player findNearestPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        Location rocketeerLocation = rocketeer.getEntity().getLocation();

        for (org.bukkit.entity.Entity entity : rocketeer.getEntity().getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player player) {
                double distance = player.getLocation().distance(rocketeerLocation);
                if (distance < nearestDistance && player.getGameMode() != GameMode.CREATIVE) {
                    nearest = player;
                    nearestDistance = distance;
                }
            }
        }
        return nearest;
    }

    public void handleDamage() {
        if (isRestocking) {
            interruptRestock();
        }
        if (rocketeer.getRocketCount() == 0) {
            enterPanicMode();
        } else {
            Player nearestPlayer = findNearestPlayer();
            if (nearestPlayer != null) {
                enterCombatMode(nearestPlayer);
            }
        }
    }
}
