
package org.derjannik.rocketeer;

import org.bukkit.scheduler.BukkitRunnable;

public class RocketeerCombatMode {
    private final Rocketeer rocketeer;

    public RocketeerCombatMode(Rocketeer rocketeer) {
        this.rocketeer = rocketeer;
    }

    public void engageCombat() {
        if (rocketeer.getRockets().isEmpty()) {
            rocketeer.getSearchMode().searchForResupply();
            return;
        }

        // Targeting logic
        rocketeer.getMob().setTarget(rocketeer.findNearestPlayer());

        // Shooting logic
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!rocketeer.getRockets().isEmpty()) {
                    rocketeer.shootRocket();
                } else {
                    rocketeer.getSearchMode().searchForResupply();
                    this.cancel();
                }
            }
        }.runTaskTimer(rocketeer.getPlugin(), 0, 20); // Check every second
    }
}
