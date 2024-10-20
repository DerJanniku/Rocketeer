
package org.derjannik.rocketeer;

import org.bukkit.scheduler.BukkitRunnable;

public class RocketeerPanicMode {
    private final Rocketeer rocketeer;

    public RocketeerPanicMode(Rocketeer rocketeer) {
        this.rocketeer = rocketeer;
    }

    public void runAway() {
        // Run away from the player
        new BukkitRunnable() {
            @Override
            public void run() {
                rocketeer.getMob().getPathfinder().moveTo(rocketeer.findSafeLocation());
                this.cancel();
            }
        }.runTaskTimer(rocketeer.getPlugin(), 0, 20); // Check every second
    }
}
