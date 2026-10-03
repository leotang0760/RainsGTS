package com.rainsh.gtsgiantai.ai.behavior;

import com.rainsh.gtsgiantai.ai.GiantBrain;
import com.rainsh.gtsgiantai.ai.bt.Node;
import com.rainsh.gtsgiantai.ai.bt.Status;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.entity.PoseLibrary;
import com.rainsh.gtsgiantai.env.EnvironmentScanner;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.util.List;

/**
 * 空闲分支：巡逻 + 无聊行为 + 环境姿态（山/海/平地自动）+ 村庄互动（坐房子/玩弄村民）。
 * 由 Selector 保证：仅当不在 CHASE/ALERT 时执行。
 */
public class IdleNode extends Node {

    private int patrolTick = 0;
    private double patrolAngle = 0;
    private int envTick = 0;
    private int villageTick = 0;

    public IdleNode(String name) {
        super(name);
    }

    @Override
    public Status tick(GiantBrain ctx) {
        GiantEntity g = ctx.getGiant();
        if (g.getState() == GiantState.CHASE || g.getState() == GiantState.ALERT) {
            return Status.FAILURE;
        }
        if (g.getState() != GiantState.IDLE) {
            g.setState(GiantState.IDLE);
            g.getAnim().play(PoseLibrary.idle(), 20, -1, "idle");
        }

        // 巡逻：每60tick换方向，缓步移动
        patrolTick++;
        if (patrolTick >= 60) {
            patrolTick = 0;
            patrolAngle = Math.random() * Math.PI * 2;
        }
        double step = 0.12;
        g.moveAnchor(g.getAnchor().x + Math.cos(patrolAngle) * step,
                g.getAnchor().y,
                g.getAnchor().z + Math.sin(patrolAngle) * step);

        // 无聊行为
        if (g.getBoredom() >= ctx.getCfg().boredomMax()) {
            g.resetBoredom();
            doBoredomAction(ctx, g);
        }

        // 村庄互动（低频概率触发，优先级高于环境姿态）
        villageTick++;
        if (villageTick >= 300 && Math.random() < 0.25) {
            villageTick = 0;
            if (doVillageAction(ctx, g)) return Status.RUNNING;
        }

        // 环境姿态（低频概率触发）
        envTick++;
        if (envTick >= 400 && Math.random() < 0.3) {
            envTick = 0;
            doEnvironmentalPose(ctx, g);
        }
        return Status.RUNNING;
    }

    /** 村庄行为：坐房子 / 戏弄村民 / 观察村民 */
    private boolean doVillageAction(GiantBrain ctx, GiantEntity g) {
        var village = ctx.getPlugin().getVillageManager();
        if (!village.isInVillage(g)) return false;

        double r = Math.random();
        var cfg = ctx.getCfg().raw();
        double sit = cfg.getDouble("village.behaviors.sit_on_house", 0.10);
        double tease = cfg.getDouble("village.behaviors.tease_villager", 0.15);
        double watch = cfg.getDouble("village.behaviors.watch_villager", 0.20);
        double lean = cfg.getDouble("village.behaviors.lean_on_building", 0.10);

        List<Villager> villagers = village.nearbyVillagers(g, 12);
        if (r < sit) {
            if (village.sitOnHouse(g)) {
                g.getAnim().play(PoseLibrary.sit(), 30, 600, "village_sit");
                return true;
            }
            // 坐不了房子则观察
            g.getAnim().play(PoseLibrary.observe(), 20, 80, "village_observe");
            return true;
        } else if (r < sit + tease) {
            if (!villagers.isEmpty()) {
                village.teaseVillager(g, villagers.get((int) (Math.random() * villagers.size())));
                g.getAnim().play(PoseLibrary.punch(), 10, 30, "village_tease");
            }
            return true;
        } else if (r < sit + tease + watch) {
            g.getAnim().play(PoseLibrary.observe(), 20, 120, "village_watch");
            return true;
        } else if (r < sit + tease + watch + lean) {
            g.getAnim().play(PoseLibrary.lean(), 30, 400, "village_lean");
            return true;
        }
        // 漫步穿过村庄
        g.getAnim().play(PoseLibrary.stretch(), 40, 200, "village_walk");
        return true;
    }

    private void doBoredomAction(GiantBrain ctx, GiantEntity g) {
        double r = Math.random();
        double stomp = ctx.getCfg().raw().getDouble("boredom_actions.stomp", 0.20);
        double kick = ctx.getCfg().raw().getDouble("boredom_actions.kick_player", 0.15);
        double look = ctx.getCfg().raw().getDouble("boredom_actions.look_around", 0.40);

        if (r < stomp) {
            g.getAnim().play(PoseLibrary.stomp(), 10, 20, "bored_stomp");
            GiantCombat.stomp(ctx.getPlugin(), g);
        } else if (r < stomp + kick) {
            Player target = ctx.nearestPlayer(ctx.getCfg().smallAlertRange() * 0.6);
            if (target != null) {
                g.getAnim().play(PoseLibrary.punch(), 8, 15, "bored_kick");
                GiantCombat.kick(ctx.getPlugin(), g, target);
            } else {
                g.getAnim().play(PoseLibrary.stretch(), 20, 40, "bored_stretch");
            }
        } else if (r < stomp + kick + look) {
            g.getAnim().play(PoseLibrary.stretch(), 20, 40, "bored_stretch");
        } else {
            g.getAnim().play(PoseLibrary.sit(), 25, 120, "bored_sit");
        }
    }

    private void doEnvironmentalPose(GiantBrain ctx, GiantEntity g) {
        // 环境探测：山体靠 / 海边躺 / 平地坐 / 树林趴
        var env = EnvironmentScanner.scan(g.getWorld(), g.getAnchor(), 8);
        switch (env) {
            case MOUNTAIN:
                g.getAnim().play(PoseLibrary.lean(), 35, 600, "env_mountain_lean");
                return;
            case SEASIDE:
                g.getAnim().play(PoseLibrary.lieDown(), 40, 600, "env_seaside_lie");
                return;
            case FOREST:
                g.getAnim().play(PoseLibrary.sit(), 30, 400, "env_forest_sit");
                return;
            default:
                break;
        }
        // 平地：原随机姿态
        double r = Math.random();
        double sit = ctx.getCfg().raw().getDouble("environmental_stance.sit", 0.30);
        double lean = ctx.getCfg().raw().getDouble("environmental_stance.lean", 0.25);
        double lie = ctx.getCfg().raw().getDouble("environmental_stance.lie_down", 0.20);

        if (r < sit) {
            g.getAnim().play(PoseLibrary.sit(), 30, 400, "env_sit");
        } else if (r < sit + lean) {
            g.getAnim().play(PoseLibrary.lean(), 30, 400, "env_lean");
        } else if (r < sit + lean + lie) {
            g.getAnim().play(PoseLibrary.lieDown(), 40, 400, "env_lie");
        } else {
            g.getAnim().play(PoseLibrary.observe(), 20, 80, "env_observe");
        }
    }
}
