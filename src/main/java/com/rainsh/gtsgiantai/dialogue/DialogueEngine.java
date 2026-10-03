package com.rainsh.gtsgiantai.dialogue;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.Mood;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话引擎（v1.3）：巨人娘心情/状态驱动的句子预设。
 * 数据源 dialogue.yml（可编辑，/gts reload 热重载）。
 * 通道：ActionBar（默认，不刷聊天）或 Chat。
 */
public final class DialogueEngine {

    private static final Map<String, String> MOOD_COLOR = Map.of(
            "CALM", "&d", "PLAYFUL", "&b", "ANNOYED", "&6", "AGGRESSIVE", "&c");

    private final GTSGiantAI plugin;
    private final Map<String, List<String>> lines = new ConcurrentHashMap<>();
    private boolean enabled = true;
    private int interval = 300;
    private int radius = 48;
    private boolean actionbar = true;
    private String prefix = "&d❝{name}&d❞ &r";

    public DialogueEngine(GTSGiantAI plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        lines.clear();
        File f = new File(plugin.getDataFolder(), "dialogue.yml");
        if (!f.exists()) {
            plugin.saveResource("dialogue.yml", false);
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        InputStream def = plugin.getResource("dialogue.yml");
        if (def != null) {
            y.setDefaults(YamlConfiguration.loadConfiguration(
                    new InputStreamReader(def, StandardCharsets.UTF_8)));
        }
        enabled = y.getBoolean("enabled", true);
        interval = Math.max(40, y.getInt("interval_ticks", 300));
        radius = Math.max(8, y.getInt("radius", 48));
        actionbar = !"chat".equalsIgnoreCase(y.getString("channel", "actionbar"));
        prefix = y.getString("prefix", "&d❝{name}&d❞ &r");
        for (String cat : new String[]{"greet", "affection", "affection2", "affection3",
                "tease", "comfort", "threat", "sleepy", "praise", "possession",
                "village", "laugh", "levelup"}) {
            List<String> l = y.getStringList(cat);
            lines.put(cat, l == null ? new ArrayList<>() : l);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getInterval() {
        return interval;
    }

    /** 对某个巨人自动挑一句并说话（状态/心情驱动），返回是否说了 */
    public boolean autoSay(GiantEntity g) {
        if (!enabled || g == null || !g.isSpawned()) return false;
        if (!hasNearby(g)) return false;
        String cat = pickCategory(g);
        if (cat == null) return false;
        Player target = nearest(g);
        return say(g, cat, target);
    }

    /** 按状态/心情选类别（小概率随机其他类别增加多样性） */
    private String pickCategory(GiantEntity g) {
        GiantState st = g.getState();
        Mood mood = g.getMood();
        String main;
        if (st == GiantState.SLEEP) {
            main = "sleepy";
        } else if (st == GiantState.CHASE || mood == Mood.AGGRESSIVE) {
            main = "threat";
        } else if (mood == Mood.PLAYFUL) {
            main = Math.random() < 0.45 ? "tease" : (Math.random() < 0.5 ? "laugh" : "affection");
        } else if (mood == Mood.ANNOYED) {
            main = Math.random() < 0.6 ? "threat" : "laugh";
        } else {
            main = Math.random() < 0.4 ? affectionCat(g) : (Math.random() < 0.5 ? "comfort" : "greet");
        }
        // 10% 跳到另一个类别
        if (Math.random() < 0.1) {
            String[] all = {"greet", "affection", "affection2", "affection3", "tease", "comfort", "threat", "laugh"};
            String alt = all[(int) (Math.random() * all.length)];
            if (!alt.equals(main)) main = alt;
        }
        return lines.containsKey(main) && !lines.get(main).isEmpty() ? main : null;
    }

    /** 按心动等级选择宠溺句子池：Lv4+ 专属亲昵 / Lv3+ 熟悉宠溺 / 默认 */
    private String affectionCat(GiantEntity g) {
        int lv = g.getAffectionLevel();
        if (lv >= 4 && lines.containsKey("affection3") && !lines.get("affection3").isEmpty()) {
            return "affection3";
        }
        if (lv >= 3 && lines.containsKey("affection2") && !lines.get("affection2").isEmpty()) {
            return "affection2";
        }
        return "affection";
    }

    /** 指定类别说一句给指定玩家（可能null=附近随机） */
    public boolean say(GiantEntity g, String category, Player target) {
        if (!enabled || g == null || !g.isSpawned()) return false;
        List<String> pool = lines.get(category);
        if (pool == null || pool.isEmpty()) return false;
        String text = pool.get((int) (Math.random() * pool.size()));
        if (target != null) {
            text = text.replace("{name}", g.getDisplayName()).replace("{scale}", String.valueOf((int) g.getScale()));
            String color = MOOD_COLOR.getOrDefault(g.getMood().name(), "&f");
            String msg = plugin.msg(prefix.replace("{name}", g.getDisplayName()) + color + text);
            if (actionbar) {
                target.sendActionBar(msg);
            } else {
                target.sendMessage(msg);
            }
            return true;
        }
        // 广播给半径内玩家
        Player n = nearest(g);
        if (n != null) return say(g, category, n);
        return false;
    }

    /** 附近（半径内）是否有玩家 */
    private boolean hasNearby(GiantEntity g) {
        return nearest(g) != null;
    }

    private Player nearest(GiantEntity g) {
        Player best = null;
        double bd = radius * radius;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (p.getWorld() != g.getWorld()) continue;
            double d = p.getLocation().distanceSquared(
                    new org.bukkit.Location(g.getWorld(), g.getAnchor().x, g.getAnchor().y, g.getAnchor().z));
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** 状态悬浮条：接近的玩家周期性看到 名字·心情·心动·状态 */
    public void statusBar(GiantEntity g, Player p) {
        String moodName = switch (g.getMood()) {
            case CALM -> "&d温柔";
            case PLAYFUL -> "&b调皮";
            case ANNOYED -> "&6不满";
            case AGGRESSIVE -> "&c暴怒";
        };
        String stateName = switch (g.getState()) {
            case IDLE -> "&7徘徊";
            case ALERT -> "&e警戒";
            case CHASE -> "&c追逐";
            case SLEEP -> "&7沉睡";
        };
        int lv = g.getAffectionLevel();
        String hearts = switch (lv) {
            case 1 -> "&8♡";
            case 2 -> "&7♡";
            case 3 -> "&e♡";
            case 4 -> "&d♡";
            default -> "&c♡&d♡";
        };
        p.sendActionBar(plugin.msg("&d❝" + g.getDisplayName() + "&d❞ &f心情 " + moodName
                + " &f| &f心动 " + hearts + " Lv" + lv
                + " &f| &f状态 " + stateName + " &f| &f倍率 x" + (int) g.getScale()));
    }
}
