package org.derjannik.rocketeerPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
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
    private boolean isRestocking = false;

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

                if (rocketeer.getRocketCount() > 0) {
                    fireRocket(target);
                } else {
                    enterRestockMode();
                    this.cancel();
                }
            }
        };
        combatTask.runTaskTimer(plugin, 0, 60); // Fire every 3 seconds
    }

    private void fireRocket(Player target) {
        Location startLoc = rocketeer.getEntity().getEyeLocation();
        Vector direction = target.getEyeLocation().subtract(startLoc).toVector().normalize();

        rocketeer.getEntity().getWorld().playSound(startLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        rocketeer.setRocketCount(rocketeer.getRocketCount() - 1);

        Firework firework = (Firework) rocketeer.getEntity().getWorld().spawnEntity(startLoc, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.addEffect(org.bukkit.FireworkEffect.builder()
                .withColor(org.bukkit.Color.RED)
                .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .build());

        meta.setPower(2);
        firework.setFireworkMeta(meta);

        firework.setVelocity(direction.multiply(1.5));

        // Schedule the detonation
        new BukkitRunnable() {
            @Override
            public void run() {
                if (firework.isValid()) {
                    firework.detonate();
                }
            }
        }.runTaskLater(plugin, 20); // Detonate after 1 second
    }

    public void enterRestockMode() {
        isRestocking = true;
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

    private Location findNearestResupplyStation() {
        Location rocketeerLoc = rocketeer.getEntity().getLocation();
        org.bukkit.World world = rocketeerLoc.getWorld();
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

    private void restockRockets() {
        if (restockTask != null) {
            restockTask.cancel();
        }

        restockTask = new BukkitRunnable() {
            int restockedRockets = 0;
            @Override
            public void run() {
                if (restockedRockets < 5) {
                    rocketeer.setRocketCount(rocketeer.getRocketCount() + 1);
                    restockedRockets++;
                    rocketeer.getEntity().getWorld().playSound(rocketeer.getEntity().getLocation(), Sound.ENTITY_CREEPER_HURT, 1.0f, 0f);
                } else {
                    isRestocking = false;
                    this.cancel();
                }
            }
        };
        restockTask.runTaskTimer(plugin, 60, 40); // 3-second initial delay, then 2 seconds per rocket
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

    private Player findNearestPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (org.bukkit.entity.Entity entity : rocketeer.getEntity().getNearbyEntities(20, 20, 20)) {
            if (entity instanceof Player) {
                double distance = entity.getLocation().distance(rocketeer.getEntity().getLocation());
                if (distance < nearestDistance) {
                    nearest = (Player) entity;
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
                if (ticks >= 160 || !rocketeer.getEntity().isValid()) { // 8 seconds (160 ticks)
                    this.cancel();
                    return;
                }

                Location fleeLocation = findFleeLocation();
                rocketeer.getEntity().getPathfinder().moveTo(fleeLocation);
                ticks += 20;
            }
        };
        panicTask.runTaskTimer(plugin, 0, 20); // Update path every second
    }

    private Location findFleeLocation() {
        Location currentLoc = rocketeer.getEntity().getLocation();
        Vector randomDir = new Vector(Math.random() - 0.5, 0, Math.random() - 0.5).normalize().multiply(10);
        return currentLoc.add(randomDir);
    }

    public boolean isRestocking() {
        return isRestocking;
    }
}
