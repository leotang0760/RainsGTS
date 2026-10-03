package com.rainsh.gtsgiantai.util;

import org.bukkit.Location;

/**
 * Vec3 与 Bukkit Location 互转工具
 */
public final class LocUtil {

    private LocUtil() {
    }

    public static Vec3 of(Location loc) {
        return new Vec3(loc.getX(), loc.getY(), loc.getZ());
    }

    public static Location toLocation(Vec3 v, Location template) {
        return new Location(template.getWorld(), v.x, v.y, v.z, template.getYaw(), template.getPitch());
    }
}
