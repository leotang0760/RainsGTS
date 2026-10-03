package com.rainsh.gtsgiantai.config;

import com.rainsh.gtsgiantai.GTSGiantAI;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 配置文件类型化访问（config.yml）
 */
public final class ConfigManager {

    private final GTSGiantAI plugin;
    private FileConfiguration cfg;

    public ConfigManager(GTSGiantAI plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        cfg = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return cfg;
    }

    // ---------- 全局 ----------
    public int maxGiantsPerWorld() { return cfg.getInt("global.max_giants_per_world", 4); }
    public boolean persistGiants() { return cfg.getBoolean("global.persist_giants", true); }
    public boolean naturalSpawn() { return cfg.getBoolean("global.natural_spawn", true); }

    // ---------- 体型 ----------
    public double hugeMinScale() { return cfg.getDouble("scale_tiers.huge_min", 10.0); }

    // ---------- 休眠 ----------
    public boolean chaseCannotSleep() { return cfg.getBoolean("sleep_rules.chase_cannot_sleep", true); }
    public double noPlayerRange() { return cfg.getDouble("sleep_rules.no_player_range", 32); }
    public int sleepCheckInterval() { return cfg.getInt("sleep_rules.check_interval_ticks", 200); }
    public double baseSleepChance() { return cfg.getDouble("sleep_rules.base_chance", 0.15); }
    public double nightBonus() { return cfg.getDouble("sleep_rules.night_bonus", 0.35); }

    // ---------- 唤醒 ----------
    public double smallAlertRange() { return cfg.getDouble("wake_rules.small_alert_range", 24); }
    public boolean hugeTouchOnly() { return cfg.getBoolean("wake_rules.huge_touch_only", true); }
    public boolean sneakImmune() { return cfg.getBoolean("wake_rules.sneak_immune", true); }

    // ---------- 行为 ----------
    public int tickIntervalAlert() { return cfg.getInt("behavior.tick_interval_alert", 1); }
    public int tickIntervalIdle() { return cfg.getInt("behavior.tick_interval_idle", 2); }
    public int tickIntervalSleep() { return cfg.getInt("behavior.tick_interval_sleep", 20); }
    public double patrolRadius() { return cfg.getDouble("behavior.patrol_radius", 40); }
    public double chaseMaxDistance() { return cfg.getDouble("behavior.chase_max_distance", 80); }
    public int cooldownAfterChase() { return cfg.getInt("behavior.cooldown_after_chase", 200); }
    public int boredomMax() { return cfg.getInt("behavior.boredom_max", 600); }

    // ---------- 碰撞 ----------
    public boolean playerPushOut() { return cfg.getBoolean("collision.player_push_out", true); }
    public int syncIntervalIdle() { return cfg.getInt("collision.sync_interval_idle", 3); }
    public int syncIntervalSleep() { return cfg.getInt("collision.sync_interval_sleep", 20); }
    public double lodFar() { return cfg.getDouble("collision.lod.far", 60); }
    public boolean layersWake() { return cfg.getBoolean("collision.layers.wake_trigger", true); }
    public boolean layersHand() { return cfg.getBoolean("collision.layers.hand_interact", true); }
    public boolean layersBody() { return cfg.getBoolean("collision.layers.body_block", true); }
    public boolean layersStomp() { return cfg.getBoolean("collision.layers.stomp_damage", true); }

    // ---------- 渲染 ----------
    public String baseMaterialName() { return cfg.getString("render.base_material", "PLAYER_HEAD"); }
    public int customModelData() { return cfg.getInt("render.custom_model_data", 1000); }

    // ---------- 亲密交互 ----------
    public double intimateMinDistance() { return cfg.getDouble("intimate_actions.min_distance", 3); }
    public double intimateMaxDistance() { return cfg.getDouble("intimate_actions.max_distance", 6); }
    public int intimateCooldownAfterAttack() { return cfg.getInt("intimate_actions.cooldown_after_attack", 600); }

    // ---------- 村庄 ----------
    public boolean villageEnabled() { return cfg.getBoolean("village.enabled", true); }
    public boolean hugeForcedSingle() { return cfg.getBoolean("village.huge_forced_single", true); }
    public boolean playerBuildingsTargetable() { return cfg.getBoolean("village.player_buildings_targetable", false); }

    // ---------- 破坏 ----------
    public boolean respectClaims() { return cfg.getBoolean("damage.respect_claims", true); }

    // ---------- 调试 ----------
    public boolean showCollisionBoxes() { return cfg.getBoolean("debug.show_collision_boxes", false); }
    public boolean behaviorTreeLog() { return cfg.getBoolean("debug.behavior_tree_log", false); }

    /** 从jar内默认资源读取原始配置（供校验/恢复默认） */
    public FileConfiguration defaults() {
        InputStream is = plugin.getResource("config.yml");
        if (is == null) return new YamlConfiguration();
        return YamlConfiguration.loadConfiguration(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    public File dataFile(String name) {
        return new File(plugin.getDataFolder(), name);
    }
}
