package com.rainsh.gtsgiantai.ai.behavior;

import com.rainsh.gtsgiantai.ai.GiantBrain;
import com.rainsh.gtsgiantai.ai.bt.Node;
import com.rainsh.gtsgiantai.ai.bt.Status;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.PoseLibrary;
import com.rainsh.gtsgiantai.util.LocUtil;
import org.bukkit.entity.Player;

/**
 * 警戒分支：转头注视玩家、缓慢逼近。
 * 玩家进入攻击范围 → 切换追击；玩家消失 → 回空闲。
 */
public class AlertNode extends Node {

    public AlertNode(String name) {
        super(name);
    }

    @Override
    public Status tick(GiantBrain ctx) {
        GiantEntity g = ctx.getGiant();
        if (g.getState() != GiantState.ALERT) return Status.FAILURE;

        // 播放观察动画（一次性触发）
        if (!"wake".equals(g.getAnim().getActionName()) && !"observe".equals(g.getAnim().getActionName())) {
            g.getAnim().play(PoseLibrary.observe(), 20, 60, "observe");
        }

        // 找到最近玩家（放宽到警戒范围1.5倍）
        Player nearest = ctx.nearestPlayer(ctx.getCfg().smallAlertRange() * 1.5);
        if (nearest == null) {
            // 目标消失：缓慢退回空闲
            g.setState(GiantState.IDLE);
            g.getAnim().play(PoseLibrary.idle(), 20, -1, "idle");
            return Status.FAILURE;
        }

        // 玩家进入攻击范围 → 追击
        if (GiantCombat.inAttackRange(g, nearest)) {
            g.startChase(nearest);
            return Status.RUNNING;
        }

        // 缓慢逼近目标（警戒步速，比追击慢）
        double speed = 0.18 * Math.pow(5.0 / g.getScale(), 0.25);
        g.moveToward(nearest.getLocation(), speed);
        return Status.RUNNING;
    }
}
