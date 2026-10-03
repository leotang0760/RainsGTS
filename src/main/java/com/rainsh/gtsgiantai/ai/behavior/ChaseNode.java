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
 * 追逐分支（最高优先级）。
 * 条件：存在有效追击目标。行为：朝目标移动、近战攻击（挥拳/踩踏）。
 * 目标丢失/超距 → 脱战回空闲。追逐期间绝对禁止休眠（由 SleepNode 条件保证）。
 */
public class ChaseNode extends Node {

    private int attackCooldown = 0;

    public ChaseNode(String name) {
        super(name);
    }

    @Override
    public Status tick(GiantBrain ctx) {
        GiantEntity g = ctx.getGiant();
        Player target = g.getChaseTarget();
        if (target == null || !target.isOnline() || target.isDead()) {
            g.endChase();
            return Status.FAILURE;
        }
        double chaseMax = ctx.getCfg().chaseMaxDistance();
        if (LocUtil.of(target.getLocation()).distanceSq(g.getAnchor()) > chaseMax * chaseMax) {
            g.endChase();
            return Status.FAILURE;
        }

        // 追击移动：体型越大越慢（压迫感），但步幅大；Boss阶段加成速度
        double speed = 0.55 * Math.pow(5.0 / g.getScale(), 0.25) * g.getSpeedMultiplier();
        g.moveToward(target.getLocation(), speed);

        // 攻击冷却
        if (attackCooldown > 0) attackCooldown--;
        if (attackCooldown <= 0 && GiantCombat.inAttackRange(g, target)) {
            attackCooldown = 20;
            if (Math.random() < 0.5) {
                g.getAnim().play(PoseLibrary.punch(), 8, 12, "attack");
                GiantCombat.punch(ctx.getPlugin(), g, target);
            } else {
                g.getAnim().play(PoseLibrary.stomp(), 12, 14, "stomp");
                GiantCombat.stomp(ctx.getPlugin(), g);
            }
        }
        return Status.RUNNING;
    }

    @Override
    public void reset(GiantBrain ctx) {
        attackCooldown = 0;
    }
}
