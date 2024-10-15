package org.derjannik.rocketeer;

import de.jxson.enigma.core.logging.Logger;
import de.jxson.enigma.utils.configuration.Config;
import de.jxson.enigma.utils.vectors.LocationUtils;
import org.bukkit.Location;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResupplyStation {

    private static final Logger LOG = new Logger(ResupplyStation.class);

    private static final Config SUPPLY_CONFIG = new Config("resupply-stations.yml");

    private static HashSet<ResupplyStation> SUPPLY_LOCATIONS = new HashSet<>();

    private Location resupplyLocation;

    public ResupplyStation(Location location)
    {
        this.resupplyLocation = location;
        SUPPLY_LOCATIONS.add(this);
    }

    public static void init()
    {
        SUPPLY_LOCATIONS.clear();
        List<String> locations = (List<String>) SUPPLY_CONFIG.getConfiguration().getList("stations");
        if(locations == null)
        {
            LOG.info("No resupply stations found!");
            return;
        }
        for(String serialized : locations)
        {
            SUPPLY_LOCATIONS.add(new ResupplyStation(LocationUtils.deserializeLocation(serialized)));
        }
        LOG.info(SUPPLY_LOCATIONS.size() + " resupply stations found!");
    }

    public static Optional<ResupplyStation> findByLocation(Location location)
    {
        return SUPPLY_LOCATIONS.stream().filter(rs -> rs.getResupplyLocation().distance(location) <= 0).findAny();
    }

    public void save()
    {
        List<String> locationList = SUPPLY_LOCATIONS.stream().map(rs -> LocationUtils.serializeLocation(rs.getResupplyLocation())).collect(Collectors.toList());
        SUPPLY_CONFIG.set("stations", locationList);
    }

    public void remove()
    {
        SUPPLY_LOCATIONS.remove(this);
    }

    public Location getResupplyLocation() {
        return resupplyLocation;
    }

    public static ResupplyStation findClosestStationBaseFromLocation(Location startLocation)
    {
        ResupplyStation closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (ResupplyStation station : SUPPLY_LOCATIONS) {
            double distance = station.getResupplyLocation().distance(startLocation);

            if (closest == null || distance < closestDistance) {
                closest = station;
                closestDistance = distance;
            }
        }

        return closest;
    }
}
