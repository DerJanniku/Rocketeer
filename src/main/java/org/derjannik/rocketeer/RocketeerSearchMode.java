
package org.derjannik.rocketeer;

import org.bukkit.scheduler.BukkitRunnable;

public class RocketeerSearchMode {
    private final Rocketeer rocketeer;

    public RocketeerSearchMode(Rocketeer rocketeer) {
        this.rocketeer = rocketeer;
    }

    public void searchForResupply() {
        // Search for resupply station
        new BukkitRunnable() {
            @Override
            public void run() {
                if (rocketeer.getResupplyStation() != null && rocketeer.getMob().getLocation().distance(rocketeer.getResupplyStation().getLocation()) < 20) {
                    rocketeer.getRestockMode().restock();
                    this.cancel();
                } else {
                    rocketeer.getMob().getPathfinder().moveTo(rocketeer.findNearestResupplyStation());
                }
            }
        }.runTaskTimer(rocketeer.getPlugin(), 0, 20); // Check every second
    }
}
