package org.derjannik.rocketeer;

import com.destroystokyo.paper.entity.ai.Goal;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.destroystokyo.paper.entity.ai.GoalType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPiglin;
import org.bukkit.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class ForgetTargetGoal implements Goal<Piglin> {

    private Piglin piglin;
    private static final GoalKey<Piglin> KEY = GoalKey.of(Piglin.class, new NamespacedKey("enigmaplugin", "remove_target"));

    public ForgetTargetGoal(Piglin piglin) {
        this.piglin = piglin;
    }

    @Override
    public boolean shouldActivate() {
        return true;
    }

    @Override
    public void start() {
        Mob nms = ((CraftPiglin) piglin).getHandle();
        nms.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        nms.getBrain().eraseMemory(MemoryModuleType.NEAREST_PLAYERS);
        nms.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        nms.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        nms.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        nms.getBrain().eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
        nms.getBrain().eraseMemory(MemoryModuleType.HUNTED_RECENTLY);

        nms.getBrain().eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        nms.getBrain().eraseMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
        nms.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    public void stop() {
    }

    @Override
    public @NotNull GoalKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull EnumSet<GoalType> getTypes() {
        return EnumSet.of(GoalType.MOVE, GoalType.LOOK);
    }
}
