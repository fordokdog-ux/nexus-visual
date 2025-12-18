package dev.simplevisuals.client.managers;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WaypointManager {

    public static final class Waypoint {
        public final String name;
        public final Vec3d pos;
        public Waypoint(String name, Vec3d pos) {
            this.name = name;
            this.pos = pos;
        }
    }

    private static final List<Waypoint> WAYPOINTS = new ArrayList<>();

    public static synchronized void add(String name, Vec3d pos) {
        WAYPOINTS.add(new Waypoint(name, pos));
    }

    public static synchronized boolean remove(String name) {
        return WAYPOINTS.removeIf(w -> w.name.equalsIgnoreCase(name));
    }

    public static synchronized void clear() {
        WAYPOINTS.clear();
    }

    public static synchronized List<Waypoint> list() {
        return Collections.unmodifiableList(new ArrayList<>(WAYPOINTS));
    }
}
