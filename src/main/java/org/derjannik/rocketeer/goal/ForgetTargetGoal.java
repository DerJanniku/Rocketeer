package org.derjannik.rocketeer.goal;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import org.bukkit.entity.Mob;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ForgetTargetGoal implements Goal<Mob> {
    private static final GoalKey<Mob> KEY = GoalKey.of(Mob.class, new NamespacedKey("rocketeer", "forget_target"));
    private final Mob mob;

    public ForgetTargetGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean shouldActivate() {
        return this.mob.getTarget() != null;
    }

    @Override
    public void start() {
        this.mob.setTarget(null);
    }

    @Override
    public @NotNull GoalKey<Mob> getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
