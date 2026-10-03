package com.rainsh.gtsgiantai.ai;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.ai.bt.Node;
import com.rainsh.gtsgiantai.ai.bt.Selector;
import com.rainsh.gtsgiantai.ai.bt.Status;
import com.rainsh.gtsgiantai.ai.behavior.AlertNode;
import com.rainsh.gtsgiantai.ai.behavior.ChaseNode;
import com.rainsh.gtsgiantai.ai.behavior.IdleNode;
import com.rainsh.gtsgiantai.ai.behavior.SleepNode;
import com.rainsh.gtsgiantai.config.ConfigManager;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.util.LocUtil;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.entity.Player;

/**
 * 巨人主脑：行为树上下文 + 根节点组装。
 * 根节点优先级：CHASE > ALERT > IDLE > SLEEP。
 */
public class GiantBrain {

    private final GTSGiantAI plugin;
    private final GiantEntity giant;
    private final Node root;

    public GiantBrain(GTSGiantAI plugin, GiantEntity giant) {
        this.plugin = plugin;
        this.giant = giant;
        this.root = new Selector("GiantBrain",
                new ChaseNode("chase"),
                new AlertNode("alert"),
                new IdleNode("idle"),
                new SleepNode("sleep"));
    }

    public void tick() {
        Status s = root.tick(this);
        if (plugin.getCfg().behaviorTreeLog()) {
            plugin.getLogger().info("[" + giant.getId().toString().substring(0, 8) + "] BT=" + s);
        }
    }

    // ---------------- 上下文工具 ----------------

    public GiantEntity getGiant() {
        return giant;
    }

    public GTSGiantAI getPlugin() {
        return plugin;
    }

    public ConfigManager getCfg() {
        return plugin.getCfg();
    }

    public boolean isNight() {
        long time = giant.getWorld().getTime() % 24000;
        return time >= 13000 && time <= 23000;
    }

    /** 附近最近的非潜行玩家（用于警戒/追击判定），null表示无 */
    public Player nearestPlayer(double range) {
        Player best = null;
        double bestSq = range * range;
        Vec3 anchor = giant.getAnchor();
        for (Player p : giant.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.isSneaking() && getCfg().sneakImmune()) continue;
            double d = LocUtil.of(p.getLocation()).distanceSq(anchor);
            if (d < bestSq) {
                bestSq = d;
                best = p;
            }
        }
        return best;
    }

    /** 是否满足休眠条件（追逐永远禁止休眠） */
    public boolean canSleep() {
        if (giant.getState() == GiantState.CHASE) return false;
        if (!getCfg().chaseCannotSleep()) return false;
        double range = getCfg().noPlayerRange();
        double rangeSq = range * range;
        Vec3 anchor = giant.getAnchor();
        for (Player p : giant.getWorld().getPlayers()) {
            if (!p.isOnline()) continue;
            if (LocUtil.of(p.getLocation()).distanceSq(anchor) <= rangeSq) return false;
        }
        return true;
    }

    /** 当前休眠概率（基础 + 夜间加成） */
    public double sleepChance() {
        double chance = getCfg().baseSleepChance();
        if (isNight()) chance += getCfg().nightBonus();
        return chance;
    }
}
