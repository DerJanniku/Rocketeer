package de.jxson.enigma.core.monster.rocketeer;

import com.destroystokyo.paper.entity.ai.MobGoals;
import de.jxson.enigma.EnigmaPlugin;
import de.jxson.enigma.api.monster.EnigmaMonster;
import de.jxson.enigma.core.logging.Logger;
import de.jxson.enigma.core.monster.rocketeer.goal.ForgetTargetGoal;
import de.jxson.enigma.core.monster.rocketeer.goal.MoveToStationGoal;
import de.jxson.enigma.core.score.ScoreHandler;
import de.jxson.enigma.utils.itemstack.ItemCreator;
import de.jxson.enigma.utils.threading.Scheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPiglin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Piglin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * - Implement Visuals/Sounds
 */

public class Rocketeer implements EnigmaMonster<Piglin>, Listener {

    private static final Logger LOG = new Logger(Rocketeer.class);
    private Piglin mob;

    public static final String MOB_METADATA = "ROCKETEER";
    private static final String MOB_SB_TAG = "RT";
    private static final String MOB_AMMUNITION_TAG = "RT_AMMO";
    private static final int MAX_AMMO = 5;

    private int tick;
    private RocketeerPhases currentPhase;
    private RocketeerActions currentAction;

    private List<ArmorStand> currentArmorbelt;

    private static final HashMap<Piglin, Rocketeer> AVAILABLE_ROCKETEERS = new HashMap<>();

    private ResupplyStation directStation;
    boolean temporary = false;

    private BukkitTask restockingTask;
    private BukkitTask panicTask;
    private BukkitTask laydownProjectileTask;

    private boolean restockingTaskInterrupted;
    private boolean panicTaskInterrupted;
    private boolean laydownProjectileInterruped;

    public enum RocketeerPhases
    {
        COMBAT,
        RESTOCK,
        SEARCH,
        PANIC
    }


    public enum RocketeerActions
    {
        FIGHTING(RocketeerPhases.COMBAT),
        SEARCH_STATION(RocketeerPhases.RESTOCK),
        REACHED_STATION(RocketeerPhases.RESTOCK),
        RELOADING(RocketeerPhases.RESTOCK),
        SEARCH_TARGET(RocketeerPhases.SEARCH),
        LAY_DOWN_ROCKET(RocketeerPhases.SEARCH),
        YA_YEET(RocketeerPhases.PANIC);

        private RocketeerPhases belongsToPhase;
        RocketeerActions(RocketeerPhases belongingPhase)
        {
            this.belongsToPhase = belongingPhase;
        }

        public RocketeerPhases belongsToPhase() {
            return belongsToPhase;
        }
    }

    @Override
    public void spawn(Location location) {
        this.mob = (Piglin) location.getWorld().spawnEntity(location, EntityType.PIGLIN);
        mob().getScoreboardTags().add(MOB_METADATA);
        mob().getScoreboardTags().add(MOB_SB_TAG);
        mob().customName(Component.text("Rocketeer").color(TextColor.fromHexString("#E02F2F")).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        mob().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, PotionEffect.INFINITE_DURATION, 0, false, false));
        mob().setPersistent(true);
        mob().getAttribute(Attribute.GENERIC_FOLLOW_RANGE).setBaseValue(25D);
        mob().getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.3D);
        mob().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40D);
        mob().setHealth(20D);
        mob().setImmuneToZombification(true);
        mob().setIsAbleToHunt(false);
        mob().setCanPickupItems(false);
        mob().setCustomNameVisible(false);
        mob().setAdult();
        mob().getInventory().clear();

        mob().getEquipment().setChestplate(chestplate());
        mob().getEquipment().setLeggings(leggings());
        mob().getEquipment().setBoots(boots());

        this.tick = 0;
        createArmorBelt();
        this.currentPhase = RocketeerPhases.SEARCH;
        this.currentAction = RocketeerActions.SEARCH_TARGET;
        this.directStation = ResupplyStation.findClosestStationBaseFromLocation(this.mob.getLocation());
        AVAILABLE_ROCKETEERS.put(mob(), this);

        mob().getEquipment().setItemInMainHand(weapon());
    }

    private void createArmorBelt()
    {
        this.currentArmorbelt = new ArrayList<>();

        for(int i = 0; i < MAX_AMMO; i++)
        {
            this.currentArmorbelt.add(ammunition());
        }
    }

    private ArmorStand ammunition()
    {
        ArmorStand belt = (ArmorStand) mob().getLocation().getWorld().spawnEntity(mob.getLocation(), EntityType.ARMOR_STAND);
        belt.getEquipment().setHelmet(new ItemStack(Material.FIREWORK_ROCKET, 1));
        belt.setInvisible(true);
        belt.setInvulnerable(true);
        belt.setMarker(true);
        belt.addScoreboardTag(MOB_AMMUNITION_TAG);
        return belt;
    }

    public static void tick()
    {
        List<Piglin> rocketeersListForRemovable = new ArrayList<>();
        Bukkit.getScheduler().runTaskTimerAsynchronously(EnigmaPlugin.getInstance(), () -> {
            AVAILABLE_ROCKETEERS.forEach((piglin, rocketeer) -> {
                if(piglin.isDead()) {
                    if(!piglin.getPassengers().isEmpty())
                    {
                        for(Entity passenger : piglin.getPassengers())
                        {
                            Scheduler.syncLater(passenger::remove, 2L);
                        }
                    }
                    for(ArmorStand ammunition : rocketeer.getCurrentArmorbelt())
                    {
                        Scheduler.syncLater(() -> {
                            ammunition.getWorld().spawnEntity(ammunition.getEyeLocation(), EntityType.FIREWORK_ROCKET);
                            ammunition.remove();
                        }, 2L);
                    }
                    rocketeersListForRemovable.add(piglin);
                }
                rocketeer.tick++;

                double beltRotationSpeed = 0.2;
                double beltRotationRadius = 1;
                double beltRotationOffset = 2 * Math.PI / rocketeer.getCurrentArmorbelt().size();

                double angle = rocketeer.tick / 20.0;
                angle *= beltRotationSpeed;
                angle *= 2 * Math.PI;

                for(ArmorStand ammunition : rocketeer.getCurrentArmorbelt())
                {
                    double x = beltRotationRadius * Math.sin(angle);
                    double y = -1.2;
                    double z = beltRotationRadius * Math.cos(angle);

                    Location newLocation = rocketeer.mob().getLocation().clone().add(x, y, z);
                    newLocation.setYaw(0);
                    newLocation.setPitch(0);
                    Scheduler.sync(() -> ammunition.teleport(newLocation));

                    angle += beltRotationOffset;
                }

                if(rocketeer.currentPhase() == RocketeerPhases.SEARCH) {
                    MobGoals goals = Bukkit.getMobGoals();
                    goals.removeAllGoals(rocketeer.mob);
                    goals.addGoal(rocketeer.mob, 1, new ForgetTargetGoal(piglin));
                    if(rocketeer.laydownProjectileTask != null) return;
                    rocketeer.laydownProjectileTask = Bukkit.getScheduler().runTaskTimerAsynchronously(EnigmaPlugin.getInstance(), () -> {
                        if(rocketeer.currentPhase() != RocketeerPhases.SEARCH)
                        {
                            rocketeer.laydownProjectileTask.cancel();
                            rocketeer.laydownProjectileTask = null;
                            rocketeer.laydownProjectileInterruped = false;
                            return;
                        }
                        if(rocketeer.hasChargedProjectile())
                        {

                            if(rocketeer.temporary) return;
                            rocketeer.temporary = true;
                            Scheduler.syncLater(() -> {
                                rocketeer.mob.getEquipment().setItemInMainHand(rocketeer.weapon());
                                rocketeer.currentArmorbelt.add(rocketeer.ammunition());
                                rocketeer.temporary = false;
                            }, 5*20);
                        }
                    }, 0, 1L);
                }

                if(rocketeer.currentPhase() == RocketeerPhases.COMBAT)
                {
                    if(rocketeer.currentAction == RocketeerActions.FIGHTING)
                    {
                        MobGoals goals = Bukkit.getMobGoals();
                        goals.removeAllGoals(rocketeer.mob);
                        if(rocketeer.hasChargedProjectile())
                        {
                            if(rocketeer.temporary) return;
                            rocketeer.temporary = true;
                            if(!rocketeer.currentArmorbelt.isEmpty())
                            {
                                ArmorStand as = rocketeer.currentArmorbelt.remove(rocketeer.currentArmorbelt.size()-1);
                                Scheduler.sync(as::remove);
                            }
                        }
                    }
                }

                if (rocketeer.currentPhase() == RocketeerPhases.RESTOCK) {
                    if(rocketeer.currentAction == RocketeerActions.SEARCH_STATION)
                    {
                        if(rocketeer.mob.getLocation().distance(rocketeer.directStation.getResupplyLocation()) < 2.5)
                        {
                            rocketeer.currentAction = RocketeerActions.REACHED_STATION;
                            return;
                        }

                        MobGoals goals = Bukkit.getMobGoals();
                        goals.removeAllGoals(rocketeer.mob);
                        MoveToStationGoal mtsg = new MoveToStationGoal(rocketeer);
                        goals.addGoal(rocketeer.mob, 0, mtsg);
                        return;
                    }

                    if(rocketeer.currentAction == RocketeerActions.REACHED_STATION)
                    {
                        if(rocketeer.restockingTask != null) return;
                        rocketeer.restockingTask = Bukkit.getScheduler().runTaskLater(EnigmaPlugin.getInstance(), () -> {
                            if(rocketeer.restockingTaskInterrupted)
                            {
                                rocketeer.restockingTask.cancel();
                                rocketeer.restockingTaskInterrupted = false;
                            }
                            rocketeer.currentAction = RocketeerActions.RELOADING;
                            Scheduler.sync(() -> rocketeer.mob().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 255, false, false)));
                            rocketeer.restockingTask = null;
                        }, 3*20L);
                        return;
                    }

                    if(rocketeer.currentAction == RocketeerActions.RELOADING)
                    {
                        if(rocketeer.restockingTask != null) return;
                        rocketeer.restockingTask = Bukkit.getScheduler().runTaskTimer(EnigmaPlugin.getInstance(), () -> {

                            if(rocketeer.restockingTaskInterrupted) {
                                rocketeer.restockingTask.cancel();
                                rocketeer.restockingTask = null;
                                rocketeer.restockingTaskInterrupted = false;
                            }

                            if (rocketeer.getCurrentArmorbelt().size() >= 5) {
                                rocketeer.currentPhase(RocketeerPhases.SEARCH);
                                rocketeer.currentAction(RocketeerActions.SEARCH_TARGET);
                                Scheduler.sync(() -> rocketeer.mob.removePotionEffect(PotionEffectType.SLOWNESS));
                                rocketeer.restockingTask.cancel();
                                rocketeer.restockingTask = null;
                            }
                            else
                            {
                                Scheduler.sync(() -> {
                                    ArmorStand ammo = rocketeer.ammunition();
                                    rocketeer.getCurrentArmorbelt().add(ammo);
                                    Scheduler.syncLater(() -> {
                                        rocketeer.mob().getWorld().spawnParticle(Particle.EXPLOSION, ammo.getLocation(), 4, 0.4, 0.2, 0.3, 1);
                                        rocketeer.mob().getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, ammo.getLocation(), 1, 0, 0, 0, 1);
                                    }, 2L);
                                });
                            }
                        }, 0, 2*20L);
                        return;
                    }
                }

                if(rocketeer.currentPhase() == RocketeerPhases.PANIC && rocketeer.currentAction == RocketeerActions.YA_YEET)
                {
                    if(rocketeer.panicTask != null) return;
                    rocketeer.mob.getPathfinder().stopPathfinding();
                    Scheduler.sync(() -> rocketeer.mob.removePotionEffect(PotionEffectType.SLOWNESS));

                    PathfinderMob craftPiglin = ((CraftPiglin) rocketeer.mob()).getHandle();
                    Set<WrappedGoal> oldGoals = craftPiglin.goalSelector.getAvailableGoals();

                    Scheduler.sync(() -> {
                        craftPiglin.goalSelector.getAvailableGoals().clear();
                        craftPiglin.goalSelector.addGoal(1, new AvoidEntityGoal<>(
                                craftPiglin,
                                net.minecraft.world.entity.player.Player.class,
                                15, //Max Distance
                                1.3f, //Walk Speed
                                1.5f) //Run Speed
                        );
                        craftPiglin.goalSelector.addGoal(2, new PanicGoal(craftPiglin, 1.5f));
                    });

                    rocketeer.panicTask = Bukkit.getScheduler().runTaskLater(EnigmaPlugin.getInstance(), () -> {
                            rocketeer.panicTask.cancel();
                            rocketeer.panicTask = null;
                            rocketeer.panicTaskInterrupted = false;
                            rocketeer.restockingTaskInterrupted = false;
                            craftPiglin.goalSelector.getAvailableGoals().clear();
                            craftPiglin.goalSelector.getAvailableGoals().addAll(oldGoals);
                            rocketeer.currentPhase = RocketeerPhases.RESTOCK;
                            rocketeer.currentAction = RocketeerActions.SEARCH_STATION;
                    }, 5*20L);
                }

            if(rocketeer.tick == Integer.MAX_VALUE)
                rocketeer.tick = 0;
            });
            rocketeersListForRemovable.forEach(AVAILABLE_ROCKETEERS::remove);
        }, 0, 1L);
    }

    public static void load()
    {
        Bukkit.getWorlds().forEach(world -> {
            List<Entity> entities = world.getEntities();

            entities.stream()
                    .filter(entity -> entity instanceof ArmorStand)
                    .map(entity -> (ArmorStand) entity)
                    .filter(as -> as.getScoreboardTags().contains(MOB_AMMUNITION_TAG))
                    .forEach(Entity::remove);

            entities.stream()
                    .filter(entity -> entity instanceof Piglin)
                    .map(entity -> (Piglin) entity)
                    .filter(piglin -> piglin.getScoreboardTags().contains(MOB_METADATA))
                    .filter(piglin -> !AVAILABLE_ROCKETEERS.containsKey(piglin))
                    .forEach(piglin -> {
                        Rocketeer tmp = fromMob(piglin);
                        AVAILABLE_ROCKETEERS.put(piglin, tmp);
                    });
        });
    }

    public static void save()
    {
        //TODO
        LOG.info(String.format("Saved %d Rocketeer's to file!", AVAILABLE_ROCKETEERS.size()));
    }

    public static Rocketeer fromMob(Piglin piglin)
    {
        if(AVAILABLE_ROCKETEERS.containsKey(piglin))
            return AVAILABLE_ROCKETEERS.get(piglin);
        Rocketeer temporary = new Rocketeer();
        temporary.mob = piglin;
        temporary.createArmorBelt();
        return temporary;
    }

    private final static ItemStack AMMUNITION = new ItemCreator(Material.FIREWORK_ROCKET, 1)
            .customMeta((FireworkMeta meta) -> {
                meta.addEffects(
                        FireworkEffect.builder()
                                .with(FireworkEffect.Type.BALL)
                                .flicker(true)
                                .trail(true)
                                .withColor(
                                        Color.fromRGB((int) Long.parseLong("FFA33B", 16)),
                                        Color.fromRGB((int) Long.parseLong("FF5E19", 16))
                                )
                                .withFade(
                                        Color.fromRGB((int) Long.parseLong("FFE330", 16))
                                ).build()
                );
            })
            .itemStack();

    /* Gear */
    private ItemStack weapon() {
        if(this.currentPhase == RocketeerPhases.COMBAT)
        {
            return new ItemCreator(Material.CROSSBOW, 1)
                    .durability(-1)
                    .enchantment(Enchantment.QUICK_CHARGE, 1)
                    .enchantment(Enchantment.VANISHING_CURSE, 1)
                    .customMeta((CrossbowMeta meta) -> {
                        meta.addChargedProjectile(AMMUNITION);
                    })
                    .itemStack();
        } else return new ItemCreator(Material.CROSSBOW, 1)
                .durability(-1)
                .enchantment(Enchantment.QUICK_CHARGE, 2)
                .enchantment(Enchantment.VANISHING_CURSE, 1)
                .customMeta((CrossbowMeta meta) -> {
                    meta.setChargedProjectiles(null);
                })
                .itemStack();

    }

    private ItemStack chestplate() {
        ItemCreator itemCreator = new ItemCreator(Material.LEATHER_CHESTPLATE, 1)
                .durability(-1)
                .dyeAble("#BA3030")
                .enchantment(Enchantment.VANISHING_CURSE, 1)
                .enchantment(Enchantment.BLAST_PROTECTION, 15)
                .enchantment(Enchantment.PROJECTILE_PROTECTION, 15)
                .enchantment(Enchantment.PROTECTION, 5)
                .armorTrim(TrimMaterial.COPPER, TrimPattern.RAISER);
        return itemCreator.itemStack();
    }

    private ItemStack leggings() {
        ItemCreator itemCreator = new ItemCreator(Material.LEATHER_LEGGINGS, 1)
                .durability(-1)
                .dyeAble("#BA3030")
                .enchantment(Enchantment.VANISHING_CURSE, 1)
                .enchantment(Enchantment.BLAST_PROTECTION, 15)
                .enchantment(Enchantment.PROJECTILE_PROTECTION, 15)
                .enchantment(Enchantment.PROTECTION, 5)
                .armorTrim(TrimMaterial.QUARTZ, TrimPattern.SILENCE);
        return itemCreator.itemStack();
    }

    private ItemStack boots() {
        ItemCreator itemCreator = new ItemCreator(Material.LEATHER_BOOTS, 1)
                .durability(-1)
                .dyeAble("#BA3030")
                .enchantment(Enchantment.BLAST_PROTECTION, 15)
                .enchantment(Enchantment.PROJECTILE_PROTECTION, 15)
                .enchantment(Enchantment.PROTECTION, 5)
                .enchantment(Enchantment.FEATHER_FALLING, 10)
                .enchantment(Enchantment.SOUL_SPEED, 10)
                .enchantment(Enchantment.VANISHING_CURSE, 1)
                .armorTrim(TrimMaterial.QUARTZ, TrimPattern.SILENCE);
        return itemCreator.itemStack();
    }


    /* Events */
    @EventHandler
    public void death(EntityDeathEvent event)
    {
        if(!(event.getEntity() instanceof Piglin)) return;
        Piglin mob = (Piglin) event.getEntity();
        if(!mob.getScoreboardTags().contains(MOB_METADATA)) return;

        if(mob.getKiller() == null) return;
        ScoreHandler.modify(event.getEntity().getKiller()).add(4);
    }

    @EventHandler
    public void shoot(ProjectileLaunchEvent event)
    {
        if(!(event.getEntity().getShooter() instanceof Piglin)) return;
        Piglin shooter = (Piglin) event.getEntity().getShooter();
        if(!shooter.getScoreboardTags().contains(MOB_METADATA)) return;
        Rocketeer potentialRocketeer = AVAILABLE_ROCKETEERS.getOrDefault(shooter, null);
        if(potentialRocketeer == null) return;

        potentialRocketeer.temporary = false;

        if(potentialRocketeer.currentPhase == RocketeerPhases.COMBAT)
            potentialRocketeer.mob.getEquipment().setItemInMainHand(potentialRocketeer.weapon());

        if(potentialRocketeer.currentArmorbelt.isEmpty())
        {
            potentialRocketeer.currentPhase = RocketeerPhases.RESTOCK;
            potentialRocketeer.currentAction = RocketeerActions.SEARCH_STATION;
            EntityTargetLivingEntityEvent targetEvent = new EntityTargetLivingEntityEvent(potentialRocketeer.mob, potentialRocketeer.mob.getTarget(), EntityTargetEvent.TargetReason.CUSTOM);
            Scheduler.sync(targetEvent::callEvent);
            return;
        }
    }

    private long temporaryTimestamp = 0;
    private final long temporaryTimestampRange = 100;

    @EventHandler
    public void target(EntityTargetLivingEntityEvent event)
    {
        if(!(event.getEntity() instanceof Piglin shooter)) return;
        if(!shooter.getScoreboardTags().contains(MOB_METADATA)) return;
        Rocketeer potentialRocketeer = AVAILABLE_ROCKETEERS.getOrDefault(shooter, null);

        if(event.getReason() == EntityTargetEvent.TargetReason.CLOSEST_PLAYER)
        {
            if(potentialRocketeer == null) return;
            if(event.getTarget() == null) return;

            if(     potentialRocketeer.currentPhase == RocketeerPhases.SEARCH &&
                    potentialRocketeer.currentAction == RocketeerActions.SEARCH_TARGET &&
                    event.getTarget().getType() == EntityType.PLAYER
            )
            {
                potentialRocketeer.currentPhase = RocketeerPhases.COMBAT;
                potentialRocketeer.currentAction = RocketeerActions.FIGHTING;
                potentialRocketeer.mob.getEquipment().setItemInMainHand(potentialRocketeer.weapon());
                return;
            }

            if (potentialRocketeer.currentPhase == RocketeerPhases.RESTOCK)
            {
                event.setTarget(null);
                return;
            }
        }

        if(     event.getReason() == EntityTargetEvent.TargetReason.TARGET_INVALID
                || event.getReason() == EntityTargetEvent.TargetReason.FORGOT_TARGET
        )
        {
            if(potentialRocketeer == null) return;

            potentialRocketeer.currentPhase = RocketeerPhases.SEARCH;
            potentialRocketeer.currentAction = RocketeerActions.SEARCH_TARGET;
            potentialRocketeer.temporary = false;

            return;
        }

        if(event.getReason() == EntityTargetEvent.TargetReason.CUSTOM)
        {
            if (potentialRocketeer.currentPhase == RocketeerPhases.RESTOCK) {
                MobGoals goals = Bukkit.getMobGoals();
                goals.removeAllGoals(shooter);
                ForgetTargetGoal ftg = new ForgetTargetGoal(shooter);
                goals.addGoal(shooter, 1, ftg);
            }
            return;
        }

        if(potentialRocketeer.currentPhase == RocketeerPhases.RESTOCK || potentialRocketeer.currentPhase == RocketeerPhases.PANIC)
        {
            event.setTarget(null);
        }
    }

    @EventHandler
    public void damage(EntityDamageByEntityEvent event)
    {
        if(!(event.getEntity() instanceof Piglin entity)) return;
        if(!entity.getScoreboardTags().contains(MOB_METADATA)) return;
        Rocketeer potentialRocketeer = AVAILABLE_ROCKETEERS.getOrDefault(entity, null);
        if(potentialRocketeer == null) return;

        if( potentialRocketeer.currentPhase == RocketeerPhases.RESTOCK && (
                potentialRocketeer.currentAction == RocketeerActions.RELOADING ||
                potentialRocketeer.currentAction == RocketeerActions.REACHED_STATION)
        )
        {
            if(potentialRocketeer.currentArmorbelt.isEmpty())
            {
                potentialRocketeer.currentPhase = RocketeerPhases.PANIC;
                potentialRocketeer.currentAction = RocketeerActions.YA_YEET;
            }
            else
            {
                Scheduler.sync(() -> potentialRocketeer.mob.removePotionEffect(PotionEffectType.SLOWNESS));
                potentialRocketeer.currentPhase = RocketeerPhases.COMBAT;
                potentialRocketeer.currentAction = RocketeerActions.FIGHTING;
            }
            potentialRocketeer.restockingTask.cancel();
            potentialRocketeer.restockingTask = null;
            potentialRocketeer.restockingTaskInterrupted = true;
        }
    }

    /* Getters 'nd Setters */
    public Piglin mob() {
        return mob;
    }

    public ResupplyStation getDirectStation() {
        return directStation;
    }

    public List<ArmorStand> getCurrentArmorbelt() {
        return currentArmorbelt;
    }

    public RocketeerPhases currentPhase() {
        return currentPhase;
    }

    public void currentPhase(RocketeerPhases currentPhase) {
        this.currentPhase = currentPhase;
    }

    public void currentAction(RocketeerActions currentAction) {
        this.currentAction = currentAction;
    }

    public RocketeerActions currentAction() {
        return currentAction;
    }

    public int getTick() {
        return tick;
    }

    private boolean hasChargedProjectile()
    {
        return ((CrossbowMeta) this.mob.getEquipment().getItemInMainHand().getItemMeta()).hasChargedProjectiles();
    }
}
