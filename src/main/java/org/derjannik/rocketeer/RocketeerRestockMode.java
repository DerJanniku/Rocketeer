
package org.derjannik.rocketeer;

import org.bukkit.scheduler.BukkitRunnable;

public class RocketeerRestockMode {
    private final Rocketeer rocketeer;

    public RocketeerRestockMode(Rocketeer rocketeer) {
        this.rocketeer = rocketeer;
    }

    public void restock() {
        if (rocketeer.getResupplyStation() == null) {
            rocketeer.getSearchMode().searchForResupply();
            return;
        }

        // Move to resupply station
        new BukkitRunnable() {
            @Override
            public void run() {
                if (rocketeer.getResupplyStation() != null && rocketeer.getMob().getLocation().distance(rocketeer.getResupplyStation().getLocation()) < 20) {
                    // Restock logic
                    rocketeer.restock();
                    this.cancel();
                } else {
                    rocketeer.getMob().getPathfinder().moveTo(rocketeer.getResupplyStation().getLocation());
                }
            }
        }.runTaskTimer(rocketeer.getPlugin(), 0, 20); // Check every second
    }
}
