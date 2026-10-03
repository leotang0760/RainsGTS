package com.rainsh.gtsgiantai.ai.behavior;

import com.rainsh.gtsgiantai.ai.GiantBrain;
import com.rainsh.gtsgiantai.ai.bt.Node;
import com.rainsh.gtsgiantai.ai.bt.Status;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.PoseLibrary;

/**
 * 休眠分支（最低优先级）。
 * 条件：可休眠（无玩家 + 追逐禁止休眠）且概率判定（夜间加成）。
 * 行为：进入SLEEP状态，播放闭眼坐姿呼吸动画，AI/碰撞降频。
 */
public class SleepNode extends Node {

    public SleepNode(String name) {
        super(name);
    }

    @Override
    public Status tick(GiantBrain ctx) {
        GiantEntity g = ctx.getGiant();
        if (!ctx.canSleep()) {
            // 不可休眠：若当前正在睡，唤醒回空闲
            if (g.getState() == GiantState.SLEEP) {
                g.setState(GiantState.IDLE);
                g.getAnim().play(PoseLibrary.idle(), 20, -1, "idle");
            }
            return Status.FAILURE;
        }

        // 已休眠：保持运行
        if (g.getState() == GiantState.SLEEP) {
            return Status.RUNNING;
        }

        // 概率进入休眠（基础 + 夜间加成；体型越大睡眠越沉→判定越频繁进入）
        double chance = ctx.sleepChance();
        if (g.isHuge()) chance = Math.min(1.0, chance + 0.2); // 大体型睡得更沉
        if (Math.random() > chance) return Status.FAILURE;

        g.setState(GiantState.SLEEP);
        g.getAnim().play(PoseLibrary.sleep(), 40, -1, "sleep");
        return Status.RUNNING;
    }
}
