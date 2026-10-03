package com.rainsh.gtsgiantai.village;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.util.LocUtil;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 村庄互动系统：
 *  - 村庄检测（周边村民聚集判定）
 *  - 村庄行为（观察/戏弄村民、坐房子、碾压建筑）
 *  - 村庄集群生成（中小型3-5只 / 大体型强制单只）
 */
public final class VillageManager {

    private final GTSGiantAI plugin;

    public VillageManager(GTSGiantAI plugin) {
        this.plugin = plugin;
    }

    /** 检测巨人周边是否处于村庄（村民数量 >= 阈值） */
    public boolean isInVillage(GiantEntity giant) {
        if (!plugin.getCfg().raw().getBoolean("village.enabled", true)) return false;
        int radius = plugin.getCfg().raw().getInt("village.scan_radius", 16);
        int threshold = plugin.getCfg().raw().getInt("village.villager_count_threshold", 3);
        int count = 0;
        Vec3 a = giant.getAnchor();
        double r2 = radius * radius;
        for (Entity e : giant.getWorld().getEntities()) {
            if (!(e instanceof Villager v)) continue;
            if (v.isDead()) continue;
            if (LocUtil.of(v.getLocation()).distanceSq(a) <= r2) {
                count++;
                if (count >= threshold) return true;
            }
        }
        return false;
    }

    /** 获取巨人周边村民 */
    public List<Villager> nearbyVillagers(GiantEntity giant, double radius) {
        List<Villager> out = new ArrayList<>();
        Vec3 a = giant.getAnchor();
        double r2 = radius * radius;
        for (Entity e : giant.getWorld().getEntities()) {
            if (!(e instanceof Villager v)) continue;
            if (v.isDead()) continue;
            if (LocUtil.of(v.getLocation()).distanceSq(a) <= r2) out.add(v);
        }
        return out;
    }

    /** 戏弄村民：把村民抛向空中/旁边（不致死，仿GTS"玩弄"） */
    public void teaseVillager(GiantEntity giant, Villager v) {
        if (v == null || v.isDead()) return;
        Vector dir = v.getLocation().toVector().subtract(
                new Vector(giant.getAnchor().x, giant.getAnchor().y, giant.getAnchor().z)).normalize();
        v.setVelocity(dir.multiply(0.8).setY(1.2));
        v.setInvulnerable(false);
        giant.getWorld().playSound(v.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_HURT, 1.0f, 1.2f);
    }

    /** 坐房子：找村庄内最高实心方块，移动锚点到其上方并坐下，分级破坏下方建筑 */
    public boolean sitOnHouse(GiantEntity giant) {
        if (!plugin.getCfg().raw().getBoolean("village.enabled", true)) return false;
        if (!plugin.getCfg().raw().getBoolean("village.huge_can_destroy_buildings", true)) return false;

        World world = giant.getWorld();
        Vec3 a = giant.getAnchor();
        int r = plugin.getCfg().raw().getInt("village.scan_radius", 16);
        int bx = (int) Math.floor(a.x);
        int bz = (int) Math.floor(a.z);

        // 找半径内最高非自然方块（建筑）：从高往低找
        Block target = null;
        int bestY = -1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                int top = world.getHighestBlockYAt(bx + dx, bz + dz);
                for (int y = top; y >= top - 8 && y > 0; y--) {
                    Block b = world.getBlockAt(bx + dx, y, bz + dz);
                    if (isBuildingBlock(b.getType()) && y > bestY) {
                        bestY = y;
                        target = b;
                        break;
                    }
                }
            }
        }
        if (target == null) return false;

        // 移动到建筑上方
        Location sitLoc = target.getLocation().add(0.5, 1, 0.5);
        giant.moveAnchor(sitLoc.getX(), sitLoc.getY(), sitLoc.getZ());
        giant.getAnim().play(com.rainsh.gtsgiantai.entity.PoseLibrary.sit(), 30, 300, "village_sit");

        // 分级破坏下方建筑（大体型才破坏）
        if (giant.isHuge()) {
            int radius = plugin.getCfg().raw().getInt("village.sit_house_break_radius", 3);
            destroyArea(world, target.getX(), target.getY(), target.getZ(), radius);
        }
        return true;
    }

    /** 破坏建筑区域（记录到恢复队列，由TraceManager统一恢复） */
    private void destroyArea(World world, double x, double y, double z, int radius) {
        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -Math.min(2, radius); dy <= 0; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (isBuildingBlock(b.getType())) {
                        plugin.getTraceManager().scheduleRestore(b);
                    }
                }
            }
        }
    }

    /** 村庄内集群生成：按体型规则（3-5只小 / 大体型单只），返回是否生成 */
    public boolean tryGroupSpawn(World world, Location center) {
        double chance = plugin.getCfg().raw().getDouble("village.group_spawn_chance", 0.15);
        if (Math.random() > chance) return false;
        boolean hugeForcedSingle = plugin.getCfg().raw().getBoolean("village.huge_forced_single", true);
        // 大体型强制单只
        if (Math.random() < 0.1) {
            plugin.getGiantManager().spawnGiant("femalegiant", 12 + Math.random() * 8, center);
            return true;
        }
        // 中小型 3-5 只
        int min = plugin.getCfg().raw().getInt("village.small_group_min", 3);
        int max = plugin.getCfg().raw().getInt("village.small_group_max", 5);
        int n = min + (int) (Math.random() * (max - min + 1));
        for (int i = 0; i < n; i++) {
            Location loc = center.clone().add(Math.random() * 12 - 6, 0, Math.random() * 12 - 6);
            loc.setY(world.getHighestBlockYAt(loc) + 1);
            plugin.getGiantManager().spawnGiant("femalegiant", 2.5 + Math.random() * 2.5, loc);
        }
        return true;
    }

    /** 玩家建筑保护：是否允许破坏该方块（领地/黑名单/玩家建筑开关） */
    public boolean canBreak(Block b) {
        if (!plugin.getCfg().raw().getBoolean("village.player_buildings_targetable", false)) {
            // 领地保护检测（WorldGuard/GriefPrevention 可选，有则尊重）
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                return false; // 简化：有WorldGuard一律交由领地规则，此处不破坏
            }
            // 默认不破坏玩家放置方块：仅破坏原版生成的建筑方块（由破坏调用方保证）
        }
        return true;
    }

    private boolean isBuildingBlock(Material m) {
        String n = m.name();
        if (n.contains("AIR") || n.contains("BEDROCK") || n.contains("COMMAND")) return false;
        if (isNatural(m)) return false;
        return true;
    }

    private boolean isNatural(Material m) {
        switch (m) {
            case GRASS_BLOCK:
            case DIRT:
            case STONE:
            case DEEPSLATE:
            case ANDESITE:
            case DIORITE:
            case GRANITE:
            case SAND:
            case GRAVEL:
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case WATER:
            case LAVA:
            case TALL_GRASS:
            case GRASS:
            case FERN:
            case LARGE_FERN:
                return true;
            default:
                return false;
        }
    }

    /** 村民掉落物（戏弄/击杀奖励） */
    public static ItemStack villagerDrop() {
        return new ItemStack(Material.EMERALD, 1 + (int) (Math.random() * 3));
    }
}
