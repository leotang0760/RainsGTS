package com.rainsh.gtsgiantai.entity;

import java.util.Map;

/**
 * 动画控制器：当前姿势向目标姿势平滑插值（blend-to-pose）。
 * 行为树每次触发动作时调用 setTarget(pose, blendTicks)，
 * 每tick调用 tick() 推进插值，并把结果应用到各骨骼Display。
 */
public final class AnimationController {

    /** 当前姿势（每段骨骼当前值） */
    private PoseLibrary.Pose current;
    /** 目标姿势 */
    private PoseLibrary.Pose target;
    /** 插值进度 */
    private int blendTicks;
    private int elapsed;
    /** 当前动作名（调试/日志） */
    private String actionName = "idle";
    /** 动作剩余保持时间（tick），<=0 表示不自动切换 */
    private int holdTicks = -1;

    public AnimationController(PoseLibrary.Pose initial) {
        this.current = PoseLibrary.Pose.copyOf(initial);
        this.target = PoseLibrary.Pose.copyOf(initial);
    }

    /** 设置目标姿势，blendTicks=过渡时长（游戏刻） */
    public void setTarget(PoseLibrary.Pose pose, int blendTicks) {
        this.target = pose;
        this.blendTicks = Math.max(1, blendTicks);
        this.elapsed = 0;
    }

    /** 设置目标姿势并附带动作保持时长 */
    public void play(PoseLibrary.Pose pose, int blendTicks, int holdTicks, String name) {
        setTarget(pose, blendTicks);
        this.holdTicks = holdTicks;
        this.actionName = name;
    }

    /** 推进一tick，返回当前姿势（各骨骼值） */
    public PoseLibrary.Pose tick() {
        if (elapsed < blendTicks) {
            float t = (float) elapsed / blendTicks;
            t = t * t * (3 - 2 * t); // smoothstep
            for (Map.Entry<BoneId, BonePose> e : target.all().entrySet()) {
                BonePose cur = current.get(e.getKey());
                if (cur != null) {
                    cur.lerpTo(e.getValue(), t);
                }
            }
            elapsed++;
        } else if (elapsed == blendTicks) {
            // 最后一帧直接对齐目标
            for (Map.Entry<BoneId, BonePose> e : target.all().entrySet()) {
                BonePose cur = current.get(e.getKey());
                if (cur != null) {
                    cur.lerpTo(e.getValue(), 1f);
                }
            }
            elapsed++;
        }
        if (holdTicks > 0) {
            holdTicks--;
        }
        return current;
    }

    /** 返回当前姿势（不推进插值） */
    public PoseLibrary.Pose currentPose() {
        return current;
    }

    public String getActionName() {
        return actionName;
    }

    public boolean isHoldFinished() {
        return holdTicks == 0;
    }

    public void clearHold() {
        holdTicks = -1;
    }
}
