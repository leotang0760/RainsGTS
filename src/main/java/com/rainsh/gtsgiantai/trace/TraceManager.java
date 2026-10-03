package com.rainsh.gtsgiantai.trace;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.BoneId;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.Map;

/**
 * 世界痕迹系统：
 *  - 脚印：巨人行走实时在脚下生成粒子/临时方块痕迹
 *  - 压痕：坐下/跺脚/落地触发地面粒子
 *  - 植被破坏：行走压坏花草，延迟恢复
 *  - 远古脚印：世界随机小概率刷新"巨人曾路过"的粒子彩蛋
 *  - 建筑恢复队列：村庄坐房/破坏记录的方块延迟还原
 */
public final class TraceManager {

    private final GTSGiantAI plugin;
    /** 待恢复方块：key=world:bx:by:bz, value=原方块数据 */
    private final Map<String, RestoreEntry> restoreQueue = new HashMap<>();
    /** v1.4 脚印方块实体：BlockDisplay → 过期时间（毫秒） */
    private final Map<org.bukkit.entity.BlockDisplay, Long> blockFootprints = new HashMap<>();
    private int ancientTick = 0;
    private Vec3 lastFootL = null;

    private static final class RestoreEntry {
        final World world;
        final BlockData data;
        final long restoreAt;

        RestoreEntry(World world, BlockData data, long restoreAt) {
            this.world = world;
            this.data = data;
            this.restoreAt = restoreAt;
        }
    }

    public TraceManager(GTSGiantAI plugin) {
        this.plugin = plugin;
    }

    /** 主循环：每tick由GiantEntity调用 */
    public void tick(GiantEntity giant, long globalTick) {
        if (!plugin.getCfg().raw().getBoolean("traces.enabled", true)) return;

        // 脚印（移动时）
        if (plugin.getCfg().raw().getBoolean("traces.footprint.enabled", true)) {
            Vec3 foot = giant.boneWorld(BoneId.FOOT_L, giant.getAnim().currentPose());
            if (lastFootL != null) {
                double distSq = foot.distanceSq(lastFootL);
                // 每移动约1格生成一次脚印
                if (distSq >= 1.0) {
                    spawnFootprint(giant.getWorld(), foot, giant.getScale());
                    lastFootL = foot.copy();
                }
            } else {
                lastFootL = foot.copy();
            }
        }

        // 压痕：跺脚/坐下动作
        String action = giant.getAnim().getActionName();
        if (action != null && (action.contains("stomp") || action.contains("sit")
                || action.contains("env_lie") || action.contains("village_sit"))) {
            if (plugin.getCfg().raw().getBoolean("traces.crush_mark.enabled", true)) {
                spawnCrushMark(giant);
            }
        }

        // 植被破坏（移动时）
        if (plugin.getCfg().raw().getBoolean("traces.vegetation.enabled", true)) {
            destroyVegetation(giant);
        }
    }

    /** 远古脚印彩蛋：低频随机生成（由主类调度） */
    public void ancientFootprintTick(long globalTick) {
        if (!plugin.getCfg().raw().getBoolean("traces.enabled", true)) return;
        if (!plugin.getCfg().raw().getBoolean("traces.footprint.enabled", true)) return;
        ancientTick++;
        int interval = plugin.getCfg().raw().getInt("traces.ancient_check_interval", 6000);
        if (ancientTick < interval) return;
        ancientTick = 0;
        double chance = plugin.getCfg().raw().getDouble("traces.footprint.ancient_random_chance", 0.001);
        if (Math.random() > chance) return;
        // 在随机在线玩家附近生成脚印粒子
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        org.bukkit.entity.Player p = Bukkit.getOnlinePlayers().iterator().next();
        Location loc = p.getLocation().clone().add(Math.random() * 40 - 20, 0, Math.random() * 40 - 20);
        loc.setY(loc.getWorld().getHighestBlockYAt(loc) + 0.1);
        double size = 2 + Math.random() * 5; // 远古巨人体型
        for (int i = 0; i < 20; i++) {
            loc.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                    loc.clone().add((Math.random() - 0.5) * size * 2, 0, (Math.random() - 0.5) * size),
                    1, 0, 0, 0, 0.5,
                    loc.getWorld().getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getBlockData());
        }
    }

    private void spawnFootprint(World world, Vec3 foot, double scale) {        Location loc = new Location(world, foot.x, foot.y, foot.z);
        int density = plugin.getCfg().raw().getInt("traces.footprint.particle_density", 12);
        Block b = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        double size = Math.min(4, scale * 0.15);
        for (int i = 0; i < density; i++) {
            world.spawnParticle(Particle.BLOCK_CRACK,
                    loc.clone().add((Math.random() - 0.5) * size, 0.1, (Math.random() - 0.5) * size * 0.6),
                    1, 0, 0, 0, 0.5, b.getBlockData());
        }
        world.spawnParticle(Particle.CLOUD, loc.clone().add(0, 0.1, 0), 3, size * 0.4, 0.1, size * 0.4, 0.05);
        // v1.4 脚印方块实体（压痕，可留存数秒后消失）
        if (plugin.getCfg().raw().getBoolean("traces.footprint.block", true)) {
            spawnFootprintBlock(world, loc, scale);
        }
        // 脚步音效（低频）
        if (plugin.getCfg().raw().getBoolean("sound.enabled", true)) {
            world.playSound(loc, Sound.BLOCK_GRAVEL_STEP, (float) Math.min(2, 0.4 + scale * 0.05), 0.6f);
        }
    }

    /** v1.4：用 BlockDisplay 生成可留存数秒的脚印压痕（雪地白色、草地暗色） */
    private void spawnFootprintBlock(World world, Location loc, double scale) {
        if (blockFootprints.size() > 60) return; // 上限防实体堆积
        try {
            Block ground = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
            Material mat = ground.getType();
            boolean snow = mat == Material.SNOW || mat == Material.SNOW_BLOCK || mat == Material.POWDER_SNOW;
            BlockData data = snow ? Material.SNOW_BLOCK.createBlockData()
                    : (mat == Material.GRASS_BLOCK ? Material.DIRT.createBlockData()
                    : ground.getBlockData().clone());
            double w = Math.min(2.2, 0.5 + scale * 0.12);
            double d = Math.min(1.6, 0.35 + scale * 0.08);
            double h = Math.max(0.03, 0.05 / Math.max(1, scale * 0.1));
            Location at = new Location(world, loc.getX(), ground.getY() + 1.0 + 0.01, loc.getZ());
            org.bukkit.entity.BlockDisplay bd = world.spawn(at, org.bukkit.entity.BlockDisplay.class, e -> {
                e.setBlock(data);
                e.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0, 0, 0), new org.joml.Quaternionf(),
                        new org.joml.Vector3f((float) w, (float) h, (float) d), new org.joml.Quaternionf()));
                e.setViewRange(64);
                e.setPersistent(false);
            });
            blockFootprints.put(bd, System.currentTimeMillis()
                    + (long) (plugin.getCfg().raw().getInt("traces.footprint.block_seconds", 8) * 1000L));
        } catch (Throwable ignored) {
        }
    }

    /** 每tick清理过期的脚印方块实体（由主类 restoreTick 调度） */
    private void clearExpiredFootprints() {
        if (blockFootprints.isEmpty()) return;
        long now = System.currentTimeMillis();
        blockFootprints.entrySet().removeIf(e -> {
            if (now >= e.getValue()) {
                e.getKey().remove();
                return true;
            }
            return false;
        });
    }

    private void spawnCrushMark(GiantEntity giant) {        Vec3 foot = giant.boneWorld(BoneId.FOOT_R, giant.getAnim().currentPose());
        World world = giant.getWorld();
        Location loc = new Location(world, foot.x, foot.y, foot.z);
        double radius = 2 + giant.getScale() * 0.2;
        for (int i = 0; i < 15; i++) {
            double dx = (Math.random() - 0.5) * radius * 2;
            double dz = (Math.random() - 0.5) * radius * 2;
            Block b = world.getBlockAt(loc.getBlockX() + (int) dx, loc.getBlockY() - 1, loc.getBlockZ() + (int) dz);
            world.spawnParticle(Particle.BLOCK_CRACK, loc.clone().add(dx, 0.05, dz),
                    1, 0, 0, 0, 0.6, b.getBlockData());
        }
        world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 2, radius * 0.3, 0.2, radius * 0.3, 0.1);
        if (plugin.getCfg().raw().getBoolean("sound.enabled", true)) {
            world.playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1.5f, 0.4f);
        }
    }

    /** 植被破坏：记录原状态，延迟恢复 */
    private void destroyVegetation(GiantEntity giant) {
        Vec3 a = giant.getAnchor();
        World world = giant.getWorld();
        int bx = (int) Math.floor(a.x);
        int bz = (int) Math.floor(a.z);
        // 采样脚部范围花草
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = world.getBlockAt(bx + dx, world.getHighestBlockYAt(bx + dx, bz + dz), bz + dz);
                if (isVegetation(b.getType()) && !restoreQueue.containsKey(key(b))) {
                    long delay = (long) (plugin.getCfg().raw().getInt("traces.vegetation.restore_seconds", 600) * 20L);
                    restoreQueue.put(key(b), new RestoreEntry(world, b.getBlockData().clone(),
                            System.currentTimeMillis() + delay * 50L));
                    b.setType(Material.AIR);
                }
            }
        }
    }

    /** 记录方块待恢复（村庄坐房破坏调用） */
    public void scheduleRestore(Block b) {
        if (b == null || b.getType() == Material.AIR) return;
        String k = key(b);
        if (restoreQueue.containsKey(k)) return;
        long delay = (long) (plugin.getCfg().raw().getInt("village.building_restore_seconds", 600) * 20L);
        restoreQueue.put(k, new RestoreEntry(b.getWorld(), b.getBlockData().clone(),
                System.currentTimeMillis() + delay * 50L));
        b.setType(Material.AIR);
    }

    /** 每tick检查恢复队列（由主类调度） */
    public void restoreTick() {
        clearExpiredFootprints();
        if (restoreQueue.isEmpty()) return;
        long now = System.currentTimeMillis();
        restoreQueue.entrySet().removeIf(e -> {
            RestoreEntry re = e.getValue();
            if (now >= re.restoreAt) {
                restoreBlock(e.getKey(), re);
                return true;
            }
            return false;
        });
    }

    private void restoreBlock(String key, RestoreEntry re) {
        String[] parts = key.split(":");
        if (parts.length < 4) return;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            Block b = re.world.getBlockAt(x, y, z);
            if (b.getType() == Material.AIR || b.getType() == Material.CAVE_AIR) {
                b.setBlockData(re.data);
            }
        } catch (Exception ignored) {
        }
    }

    private String key(Block b) {
        return b.getWorld().getName() + ":" + b.getX() + ":" + b.getY() + ":" + b.getZ();
    }

    private boolean isVegetation(Material m) {
        switch (m) {
            case GRASS:
            case TALL_GRASS:
            case FERN:
            case LARGE_FERN:
            case POPPY:
            case DANDELION:
            case BLUE_ORCHID:
            case ALLIUM:
            case AZURE_BLUET:
            case OXEYE_DAISY:
            case CORNFLOWER:
            case LILY_OF_THE_VALLEY:
            case DEAD_BUSH:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
                return true;
            default:
                return false;
        }
    }
}
