
package org.derjannik.rocketeerPlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Piglin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RocketeerPlugin extends JavaPlugin {

    private NamespacedKey rocketKey;
    private NamespacedKey combatKey;
    private final Map<UUID, Rocketeer> rocketeerMap = new HashMap<>();

    @Override
    public void onEnable() {
        this.rocketKey = new NamespacedKey(this, "rocket_count");
        this.combatKey = new NamespacedKey(this, "combat_key");

        PluginCommand rocketeerCommand = getCommand("rocketeer");
        if (rocketeerCommand != null) {
            rocketeerCommand.setExecutor(new RocketeerCommand(this));
        } else {
            getLogger().severe("Failed to register 'rocketeer' command. Is it properly defined in plugin.yml?");
        }

        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);
        getLogger().info("RocketeerPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RocketeerPlugin has been disabled!");
    }

    public NamespacedKey getRocketKey() {
        return rocketKey;
    }

    public NamespacedKey getCombatKey() {
        return combatKey;
    }

    public void addRocketeer(Rocketeer rocketeer) {
        rocketeerMap.put(rocketeer.getEntity().getUniqueId(), rocketeer);
    }

    public Rocketeer getRocketeerByEntity(Entity entity) {
        if (entity instanceof Piglin) {
            return rocketeerMap.get(entity.getUniqueId());
        }
        return null;
    }

    public void removeRocketeer(Rocketeer rocketeer) {
        rocketeerMap.remove(rocketeer.getEntity().getUniqueId());
    }
}
