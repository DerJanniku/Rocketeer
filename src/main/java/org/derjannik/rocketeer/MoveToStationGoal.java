package org.derjannik.rocketeer;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import de.jxson.enigma.core.monster.rocketeer.Rocketeer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPiglin;
import org.bukkit.entity.Piglin;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class MoveToStationGoal implements Goal<Piglin> {

    private Rocketeer rocketeer;
    private static final GoalKey<Piglin> KEY = GoalKey.of(Piglin.class, new NamespacedKey("enigmaplugin", "move_to_station"));

    public MoveToStationGoal(Rocketeer rocketeer) {
        this.rocketeer = rocketeer;
    }

    @Override
    public boolean shouldActivate() {
        double distanceSquared = rocketeer.mob().getLocation().distance(rocketeer.getDirectStation().getResupplyLocation());
        return distanceSquared > 2.5;
    }

    @Override
    public void start() {
        rocketeer.mob().getPathfinder().moveTo(rocketeer.getDirectStation().getResupplyLocation(), rocketeer.mob().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getValue()+1.3f);
    }

    @Override
    public void stop() {
        rocketeer.mob().getPathfinder().stopPathfinding();
    }

    @Override
    public void tick() {
        rocketeer.mob().setTarget(null);
        Mob nms = ((CraftPiglin) rocketeer.mob()).getHandle();
        nms.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    @Override
    public @NotNull GoalKey<Piglin> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
