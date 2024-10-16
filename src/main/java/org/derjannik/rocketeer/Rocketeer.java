package org.derjannik.rocketeer;

import org.bukkit.entity.Mob;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class Rocketeer {
    private final Mob mob;

    public Rocketeer(Mob mob) {
        this.mob = mob;
    }

    public void onSpawn() {
        // Add custom goal logic here
    }

    public void onDeath() {
        // Add custom death logic here
    }

    public void onTick() {
        if (this.mob.getTarget() != null) {
            ItemStack rocket = new ItemStack(Material.FIREWORK_ROCKET);
            this.mob.getEquipment().setItemInMainHand(rocket);
        }
    }

    public @NotNull String getName() {
        return "Rocketeer";
    }
}
