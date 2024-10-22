package org.derjannik.rocketeerPlugin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;

public class RocketeerPlugin extends JavaPlugin implements Listener {



    @EventHandler
    public void onPlayerUseSpawnEgg(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.PIGLIN_SPAWN_EGG) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    if (data.has(rocketeerSpawnEggKey, PersistentDataType.STRING) &&
                        "rocketeer_spawn_egg".equals(data.get(rocketeerSpawnEggKey, PersistentDataType.STRING))) {
                        Location location = event.getClickedBlock().getLocation().add(0, 1, 0);
                        Rocketeer rocketeer = new Rocketeer(location, this);
                        rocketeer.spawnRocketeer(location);
                        event.getItem().setAmount(event.getItem().getAmount() - 1);
                    }
                }
            }
        }
    }

    private final Map<Entity, Rocketeer> rocketeerMap = new HashMap<>();
    private NamespacedKey rocketKey;
    private NamespacedKey rocketeerSpawnEggKey;
    private CommandDispatcher<CommandSender> dispatcher;

    @Override
    public void onEnable() {
        getLogger().info("RocketeerPlugin has been enabled!");

        this.rocketKey = new NamespacedKey(this, "rockets");
        this.rocketeerSpawnEggKey = new NamespacedKey(this, "rocketeer_spawn_egg");

        // Initialize the command dispatcher
        dispatcher = new CommandDispatcher<>();

        // Register commands using the dispatcher
        registerCommands();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new RocketeerListener(this), this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void registerCommands() {
        LiteralArgumentBuilder<CommandSender> rocketeerCommand = LiteralArgumentBuilder
                .<CommandSender>literal("rocketeer")
                .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("x", integer())
                        .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("y", integer())
                                .then(RequiredArgumentBuilder.<CommandSender, Integer>argument("z", integer())
                                        .executes(context -> {
                                            CommandSender sender = context.getSource();
                                            if (sender instanceof Player player) {
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
                            if (sender instanceof Player player) {
                                giveRocketeerSpawnEgg(player);
                                player.sendMessage(Component.text("You have received a Rocketeer spawn egg!").color(NamedTextColor.GREEN));
                            }
                            return 1;
                        }));

        dispatcher.register(rocketeerCommand);
        getLogger().info("Rocketeer command registered successfully!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("rocketeer")) {
            if (sender instanceof Player player) {
                try {
                    dispatcher.execute(String.join(" ", args), player);
                } catch (CommandSyntaxException e) {
                    player.sendMessage(Component.text("Invalid command syntax!").color(NamedTextColor.RED));
                    getLogger().severe("Command syntax error: " + e.getMessage());
                }
            }
            return true;
        }
        return false;
    }

    public Rocketeer spawnRocketeer(Location location) {
    if (location == null || location.getWorld() == null) {
        getLogger().warning("Invalid location provided for spawning Rocketeer.");
        return null;
    }
    
    try {
        Rocketeer rocketeer = new Rocketeer(location, this);
        rocketeerMap.put(rocketeer.getEntity(), rocketeer);
        return rocketeer;
    } catch (Exception e) {
        getLogger().severe("Failed to spawn Rocketeer: " + e.getMessage());
        return null;
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

        // Using Adventure API components
        meta.displayName(Component.text("Rocketeer Spawn Egg").color(NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Right-click to spawn a Rocketeer").color(NamedTextColor.GRAY));
        meta.lore(lore);

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(rocketeerSpawnEggKey, PersistentDataType.BOOLEAN, true);

        spawnEgg.setItemMeta(meta);
        player.getInventory().addItem(spawnEgg);
    }
}
