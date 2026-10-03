package com.rainsh.gtsgiantai.manager;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.config.ConfigManager;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.PoseLibrary;
import com.rainsh.gtsgiantai.event.GiantSpawnEvent;
import com.rainsh.gtsgiantai.event.GiantStateChangeEvent;
import com.rainsh.gtsgiantai.permission.PermissionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 巨人管理器：生成/销毁/查询/持久化/自然生成调度/事件广播。
 */
public final class GiantManager {

    private final GTSGiantAI plugin;
    private final ConfigManager cfg;
    private final PermissionManager permissions;
    private final Map<UUID, GiantEntity> giants = new ConcurrentHashMap<>();

    public GiantManager(GTSGiantAI plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getCfg();
        this.permissions = plugin.getPermissions();
    }

    // ---------------- 生成 / 删除 ----------------

    public GiantEntity spawnGiant(String type, double scale, Location loc) {
        if (loc.getWorld() == null) return null;
        int count = countInWorld(loc.getWorld());
        if (count >= cfg.maxGiantsPerWorld()) return null;

        GiantEntity g = new GiantEntity(this, UUID.randomUUID(), type, scale);
        g.spawn(loc);

        GiantSpawnEvent evt = new GiantSpawnEvent(g);
        Bukkit.getPluginManager().callEvent(evt);
        if (evt.isCancelled()) {
            g.despawn();
            return null;
        }

        giants.put(g.getId(), g);
        saveAll();
        return g;
    }

    public boolean removeGiant(UUID id) {
        GiantEntity g = giants.remove(id);
        if (g == null) return false;
        g.despawn();
        saveAll();
        return true;
    }

    public GiantEntity getGiant(UUID id) {
        return giants.get(id);
    }

    /** 根据渲染/碰撞实体反查巨人 */
    public GiantEntity findByEntity(org.bukkit.entity.Entity entity) {
        for (GiantEntity g : giants.values()) {
            if (g.matchesEntity(entity)) return g;
        }
        return null;
    }

    public Collection<GiantEntity> allGiants() {
        return giants.values();
    }

    public List<GiantEntity> giantsInWorld(World w) {
        List<GiantEntity> out = new ArrayList<>();
        for (GiantEntity g : giants.values()) {
            if (g.getWorld() != null && g.getWorld().equals(w)) out.add(g);
        }
        return out;
    }

    public int countInWorld(World w) {
        return giantsInWorld(w).size();
    }

    // ---------------- 主循环 ----------------

    public void tick(long globalTick) {
        for (GiantEntity g : giants.values()) {
            try {
                g.tick(globalTick);
                // 世界痕迹
                plugin.getTraceManager().tick(g, globalTick);
            } catch (Exception e) {
                plugin.getLogger().warning("巨人tick异常 " + g.getId() + ": " + e.getMessage());
            }
        }
    }

    // ---------------- 持久化 ----------------

    @SuppressWarnings("unchecked")
    public void loadAll() {
        if (!cfg.persistGiants()) return;
        List<?> list = plugin.getGiantsData().getList("giants");
        if (list == null) return;
        for (Object o : list) {
            try {
                GiantEntity g = GiantEntity.fromMap(this, (Map<String, Object>) o);
                if (g != null) giants.put(g.getId(), g);
            } catch (Exception e) {
                plugin.getLogger().warning("加载巨人数据失败: " + e.getMessage());
            }
        }
        plugin.getLogger().info("已加载 " + giants.size() + " 只持久巨人");
    }

    public void saveAll() {
        if (!cfg.persistGiants()) return;
        List<Map<String, Object>> list = new ArrayList<>();
        for (GiantEntity g : giants.values()) {
            if (g.isSpawned()) list.add(g.toMap());
        }
        plugin.getGiantsData().set("giants", list);
        plugin.saveGiantsData();
    }

    // ---------------- 自然生成（基础版） ----------------

    private int naturalTick = 0;

    /** 每N tick尝试一次自然生成（低概率、远距离、开阔校验从简） */
    public void naturalSpawnTick(long globalTick) {
        if (!cfg.naturalSpawn()) return;
        naturalTick++;
        if (naturalTick < 600) return; // 每30秒检查一次
        naturalTick = 0;
        if (Math.random() > 0.35) return;

        for (World world : Bukkit.getWorlds()) {
            List<GiantEntity> list = giantsInWorld(world);
            if (list.size() >= cfg.maxGiantsPerWorld()) continue;
            // 只在世界有在线玩家时生成
            if (world.getPlayers().isEmpty()) continue;
            Player target = world.getPlayers().get((int) (Math.random() * world.getPlayers().size()));
            if (target == null) continue;
            // 玩家周围 48~96 格随机方向
            double angle = Math.random() * Math.PI * 2;
            double dist = 48 + Math.random() * 48;
            Location loc = target.getLocation().clone()
                    .add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
            loc.setY(world.getHighestBlockYAt(loc) + 1);
            // 体型权重：小60 / 中30 / 大10
            double r = Math.random();
            double scale = r < 0.60 ? 2.5 + Math.random() * 2.5
                    : r < 0.90 ? 8 + Math.random() * 2
                    : 10 + Math.random() * 10;

            // 村庄集群生成：优先在村庄附近
            if (plugin.getCfg().raw().getBoolean("village.enabled", true)) {
                // 检测目标玩家附近是否有村庄（村民聚集）
                if (nearbyVillagerCount(target.getLocation(), 24) >= 3
                        && Math.random() < plugin.getCfg().raw().getDouble("village.group_spawn_chance", 0.15)) {
                    plugin.getVillageManager().tryGroupSpawn(world, target.getLocation());
                    continue;
                }
            }

            spawnGiant("femalegiant", scale, loc);
        }
    }

    /** 统计某位置周边村民数 */
    private int nearbyVillagerCount(Location c, double radius) {
        int n = 0;
        double r2 = radius * radius;
        for (org.bukkit.entity.Entity e : c.getWorld().getEntities()) {
            if (!(e instanceof org.bukkit.entity.Villager)) continue;
            if (e.getLocation().distanceSquared(c) <= r2) n++;
        }
        return n;
    }

    // ---------------- 事件 / 钩子 ----------------

    public void onStateChange(GiantEntity g, GiantState newState) {
        Bukkit.getPluginManager().callEvent(new GiantStateChangeEvent(g, newState));
        if (cfg.behaviorTreeLog()) {
            plugin.getLogger().info("巨人 " + g.getId() + " -> " + newState);
        }
    }

    /** 玩家是否被插件豁免（不被抓取/不被破坏建筑） */
    public boolean isExempt(Player p) {
        return permissions.has(p, "gtsgiantai.no-interact");
    }

    public Material getBaseMaterial() {
        try {
            return Material.valueOf(cfg.baseMaterialName());
        } catch (Exception e) {
            return Material.PLAYER_HEAD;
        }
    }

    public int getCustomModelData() {
        return cfg.customModelData();
    }

    /** 解析行为树动作名 -> 姿势 */
    public PoseLibrary.Pose resolvePose(String actionName) {
        switch (actionName.toLowerCase()) {
            case "sit": return PoseLibrary.sit();
            case "sleep": return PoseLibrary.sleep();
            case "lie": case "lie_down": return PoseLibrary.lieDown();
            case "lean": return PoseLibrary.lean();
            case "stomp": return PoseLibrary.stomp();
            case "punch": return PoseLibrary.punch();
            case "hold": return PoseLibrary.hold();
            case "cuddle": return PoseLibrary.cuddle();
            case "whisper": return PoseLibrary.whisper();
            case "pat": return PoseLibrary.pat();
            case "lift": return PoseLibrary.lift();
            case "kiss": return PoseLibrary.kiss();
            case "nuzzle": return PoseLibrary.nuzzle();
            case "bounce": return PoseLibrary.bounce();
            case "carry": return PoseLibrary.carry();
            case "lap": return PoseLibrary.lap();
            case "observe": return PoseLibrary.observe();
            case "block": case "block_path": return PoseLibrary.blockPath();
            case "stretch": return PoseLibrary.stretch();
            case "idle": default: return PoseLibrary.idle();
        }
    }

    public GTSGiantAI getPlugin() {
        return plugin;
    }

    public ConfigManager getCfg() {
        return cfg;
    }
}
