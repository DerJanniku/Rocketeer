public class RocketeerPlugin extends JavaPlugin {

    private ResupplyStation resupplyStation;

    @Override
    public void onEnable() {
        // Initialize ResupplyStation
        this.resupplyStation = new ResupplyStation(/* Add parameters as needed */);

        // Register commands
        this.getCommand("rocketeer").setExecutor(new RocketeerCommand(this));
        this.getCommand("rocketeer_egg").setExecutor(new RocketeerEggCommand());

        getLogger().info("Rocketeer plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rocketeer plugin has been disabled!");
    }

    public ResupplyStation getResupplyStation() {
        return this.resupplyStation;
    }
}
