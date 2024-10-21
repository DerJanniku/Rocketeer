package org.derjannik.rocketeerPlugin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class RocketeerPlugin extends JavaPlugin implements Listener {
    private Map<Entity, Rocketeer> rocketeerMap = new HashMap<>();
    private NamespacedKey rocketKey;
    private NamespacedKey rocketeerSpawnEggKey;
    private CommandDispatcher<CommandSender> dispatcher;

    @Override
    public void onEnable() {
        getLogger().info("RocketeerPlugin has been enabled!");
        this.rocketKey = new NamespacedKey(this, "rockets");
        this.rocketeerSpawnEggKey = new NamespacedKey(this, "rocketeer_spawn_egg");
        dispatcher = new CommandDispatcher<>();
        registerCommands();
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("RocketeerPlugin has been disabled!");
    }

    private void registerCommands() {
        LiteralArgumentBuilder<CommandSender> rocketeerCommand = LiteralArgumentBuilder.<CommandSender>literal("rocketeer")
                .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("x", integer())
                        .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("y", integer())
                                .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("z", integer())
                                        .executes(context -> {
                                            CommandSender sender = context.getSource();
                                            if (sender instanceof Player) {
                                                Player player = (Player) sender;
                                                int x = context.getArgument("x", Integer.class);
                                                int y = context.getArgument("y", Integer.class);
                                                int z = context.getArgument("z", Integer.class);
                                                Location spawnLocation = new Location(player.getWorld(), x, y, z);
                                                spawnRocketeer(spawnLocation);
                                                player.sendMessage("Rocketeer spawned at " + x + ", " + y + ", " + z);
                                            }
                                            return 1;
                                        }))))
                .then(LiteralArgumentBuilder.<CommandSender>literal("egg")
                        .executes(context -> {
                            CommandSender sender = context.getSource();
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                giveRocketeerSpawnEgg(player);
                                player.sendMessage("You have received a Rocketeer spawn egg!");
                            }
                            return 1;
                        }));

        dispatcher.register(rocketeerCommand);

        // Register command using Bukkit's command map
        PluginCommand rocketeerPluginCommand = this.getCommand("rocketeer");
        if (rocketeerPluginCommand != null) {
            rocketeerPluginCommand.setExecutor(this);
        } else {
            getLogger().severe("Failed to register the rocketeer command!");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("rocketeer")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                try {
                    dispatcher.execute(String.join(" ", args), player);
                } catch (CommandSyntaxException e) {
                    player.sendMessage(ChatColor.RED + "Invalid command syntax!");
                    e.printStackTrace(); // Log the exception for debugging
                }
            }
            return true;
        }
        return false;
    }

    private void spawnRocketeer(Location location) {
        Rocketeer rocketeer = new Rocketeer(location, this);
        rocketeerMap.put(rocketeer.getEntity(), rocketeer);
    }

    public Rocketeer getRocketeerByEntity(Entity entity) {
        return rocketeerMap.get(entity);
    }

    public NamespacedKey getRocketKey() {
        return rocketKey;
    }

    public void removeRocketeer(Entity entity) {
        rocketeerMap.remove(entity);
    }

    private void giveRocketeerSpawnEgg(Player player) {
        ItemStack spawnEgg = new ItemStack(Material.PIGLIN_SPAWN_EGG);
        ItemMeta meta = spawnEgg.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Rocketeer Spawn Egg");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Right-click to spawn a Rocketeer");
        meta.setLore(lore);

        // Add custom NBT data to identify this as a Rocketeer spawn egg
        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(rocketeerSpawnEggKey, PersistentDataType.BOOLEAN, true);

        spawnEgg.setItemMeta(meta);
        player.getInventory().addItem(spawnEgg);
    }

    @EventHandler
    public void onSpawnEggUse(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getItem() != null) {
            ItemStack item = event.getItem();
            if (item.getType() == Material.PIGLIN_SPAWN_EGG) {
                ItemMeta meta = item.getItemMeta();
                PersistentDataContainer container = meta.getPersistentDataContainer();
                if (container.has(rocketeerSpawnEggKey, PersistentDataType.BOOLEAN)) {
                    event.setCancelled(true);
                    Location spawnLocation = event.getClickedBlock().getLocation().add(0, 1, 0);
                    spawnRocketeer(spawnLocation);
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Rocketeer spawned!");
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        event.getPlayer().getInventory().removeItem(item);
                    }
                }
            }
        }
    }
}
