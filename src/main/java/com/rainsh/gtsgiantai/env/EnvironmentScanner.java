package com.rainsh.gtsgiantai.env;

import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * 环境扫描器：检测巨人周边地形类型，驱动环境姿态。
 */
public final class EnvironmentScanner {

    public enum EnvType {
        PLAIN,      // 平地：可坐下
        MOUNTAIN,   // 山体：可倚靠
        SEASIDE,    // 海边：可躺卧
        FOREST      // 树林
    }

    private EnvironmentScanner() {
    }

    /**
     * 扫描巨人周边环境。
     * 判断依据（简化）：
     *  - 海边：水平方向 sampleRadius 格内存在水源方块（非冰冻）
     *  - 山体：周边存在高度差 >= 4 格的实心方块墙
     *  - 树林：周边树木（原木）数量 >= 4
     *  - 默认平地
     */
    public static EnvType scan(World world, Vec3 anchor, int sampleRadius) {
        if (world == null || anchor == null) return EnvType.PLAIN;
        int bx = (int) Math.floor(anchor.x);
        int by = (int) Math.floor(anchor.y);
        int bz = (int) Math.floor(anchor.z);
        int r = Math.max(2, sampleRadius);

        // 1) 海边检测：环视一圈找水
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                Block b = world.getBlockAt(bx + dx, by, bz + dz);
                if (isWater(b.getType())) return EnvType.SEASIDE;
            }
        }
        // 2) 山体检测：周边高差
        int maxHeight = world.getHighestBlockYAt(bx, bz);
        int minHeight = maxHeight;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                int h = world.getHighestBlockYAt(bx + dx, bz + dz);
                if (h < minHeight) minHeight = h;
            }
        }
        if (maxHeight - minHeight >= 4) return EnvType.MOUNTAIN;
        // 3) 树林检测
        int trees = 0;
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                Block b = world.getBlockAt(bx + dx, by + 1, bz + dz);
                if (isLog(b.getType())) trees++;
            }
        }
        if (trees >= 4) return EnvType.FOREST;
        return EnvType.PLAIN;
    }

    /** 脚部IK辅助：取某列最高非空气方块Y（用于脚贴合） */
    public static int groundY(World world, int bx, int bz) {
        return world.getHighestBlockYAt(bx, bz);
    }

    private static boolean isWater(Material m) {
        return m == Material.WATER || m == Material.KELP || m == Material.KELP_PLANT
                || m == Material.SEAGRASS || m == Material.TALL_SEAGRASS;
    }

    private static boolean isLog(Material m) {
        String n = m.name();
        return n.endsWith("_LOG") || n.endsWith("_WOOD");
    }
}
