package com.rainsh.gtsgiantai.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * 姿势库：全部内置姿势。
 * v1.2 骨架比例重构：translation 改为"正常人体比例"（scale=1 总高约 3.1 格，锚点=脚底），
 * 与 Giantess Toki 模型 pivot 一一对齐（px/16 = 格）：
 *   HEAD 2.08 / TORSO 1.74 / ARM ~2.1 / HAND ~1.3 / LEG ~1.3 / FOOT ~0.2
 */
public final class PoseLibrary {

    public static final class Pose {
        private final Map<BoneId, BonePose> bones = new HashMap<>();

        public void set(BoneId id, BonePose pose) {
            bones.put(id, pose);
        }

        public BonePose get(BoneId id) {
            return bones.get(id);
        }

        public Map<BoneId, BonePose> all() {
            return bones;
        }

        public static Pose copyOf(Pose src) {
            Pose p = new Pose();
            src.bones.forEach((k, v) -> p.bones.put(k, v.copy()));
            return p;
        }
    }

    /** 默认站立：双臂自然下垂（正常人体比例骨架） */
    public static Pose idle() {
        Pose p = new Pose();
        p.set(BoneId.HEAD, BonePose.at(0f, 2.08f, 0f));
        p.set(BoneId.TORSO, BonePose.at(0f, 1.74f, 0f));
        p.set(BoneId.ARM_L, BonePose.at(0.19f, 2.12f, 0f));
        p.set(BoneId.ARM_R, BonePose.at(-0.21f, 2.09f, 0f));
        p.set(BoneId.HAND_L, BonePose.at(0.21f, 1.33f, 0f));
        p.set(BoneId.HAND_R, BonePose.at(-0.22f, 1.25f, 0f));
        p.set(BoneId.LEG_L, BonePose.at(0.16f, 1.33f, 0f));
        p.set(BoneId.LEG_R, BonePose.at(-0.16f, 1.32f, 0f));
        p.set(BoneId.FOOT_L, BonePose.at(0.13f, 0.23f, 0f));
        p.set(BoneId.FOOT_R, BonePose.at(-0.13f, 0.21f, 0f));
        return p;
    }

    /** 休眠：坐姿闭眼（头前倾，手垂膝） */
    public static Pose sleep() {
        Pose p = idle();
        p.set(BoneId.HEAD, BonePose.at(0f, 1.98f, 0.10f).rotationTo(-25f, 8f, 0f));
        p.set(BoneId.TORSO, BonePose.at(0f, 1.59f, 0f).rotationTo(0f, 0f, 8f));
        p.set(BoneId.LEG_L, BonePose.at(0.19f, 1.10f, 0.05f));
        p.set(BoneId.LEG_R, BonePose.at(-0.19f, 1.10f, 0.05f));
        p.set(BoneId.FOOT_L, BonePose.at(0.19f, 0.10f, 0.10f));
        p.set(BoneId.FOOT_R, BonePose.at(-0.19f, 0.10f, 0.10f));
        p.set(BoneId.HAND_L, BonePose.at(0.24f, 1.26f, 0f));
        p.set(BoneId.HAND_R, BonePose.at(-0.24f, 1.26f, 0f));
        return p;
    }

    /** 坐下（空地/村庄）：贴地坐，双腿前伸微弯 */
    public static Pose sit() {
        Pose p = idle();
        p.set(BoneId.TORSO, BonePose.at(0f, 0.60f, 0f).rotationTo(0f, 0f, 5f));
        p.set(BoneId.LEG_L, BonePose.at(0.30f, 0.38f, 0.30f).rotationTo(0f, 0f, -20f));
        p.set(BoneId.LEG_R, BonePose.at(-0.30f, 0.38f, 0.30f).rotationTo(0f, 0f, 20f));
        p.set(BoneId.FOOT_L, BonePose.at(0.45f, 0.20f, 0.62f));
        p.set(BoneId.FOOT_R, BonePose.at(-0.45f, 0.20f, 0.62f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.35f, 0f));
        p.set(BoneId.ARM_L, BonePose.at(0.25f, 0.95f, 0f));
        p.set(BoneId.ARM_R, BonePose.at(-0.27f, 0.95f, 0f));
        p.set(BoneId.HAND_L, BonePose.at(0.28f, 0.62f, 0.15f));
        p.set(BoneId.HAND_R, BonePose.at(-0.28f, 0.62f, 0.15f));
        return p;
    }

    /** 躺卧（海边/平地）：仰躺 */
    public static Pose lieDown() {
        Pose p = idle();
        p.set(BoneId.TORSO, BonePose.at(0f, 0.35f, 0f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.HEAD, BonePose.at(0f, 0.40f, 0.55f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.ARM_L, BonePose.at(0.25f, 0.30f, 0.10f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.ARM_R, BonePose.at(-0.25f, 0.30f, 0.10f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.HAND_L, BonePose.at(0.25f, 0.20f, 0.55f));
        p.set(BoneId.HAND_R, BonePose.at(-0.25f, 0.20f, 0.55f));
        p.set(BoneId.LEG_L, BonePose.at(0.20f, 0.25f, 0.85f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.LEG_R, BonePose.at(-0.20f, 0.25f, 0.85f).rotationTo(90f, 0f, 0f));
        p.set(BoneId.FOOT_L, BonePose.at(0.20f, 0.15f, 1.45f));
        p.set(BoneId.FOOT_R, BonePose.at(-0.20f, 0.15f, 1.45f));
        return p;
    }

    /** 倚靠（山体/建筑，坐靠） */
    public static Pose lean() {
        Pose p = sit();
        p.set(BoneId.TORSO, BonePose.at(0f, 0.65f, -0.15f).rotationTo(0f, 0f, 8f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.35f, -0.20f).rotationTo(-10f, 0f, 0f));
        return p;
    }

    /** 跺脚（脚抬起后踏下）：这里取"抬起"瞬间，配合特效 */
    public static Pose stomp() {
        Pose p = idle();
        p.set(BoneId.LEG_R, BonePose.at(-0.30f, 1.48f, -0.10f).rotationTo(0f, 0f, 35f));
        p.set(BoneId.FOOT_R, BonePose.at(-0.42f, 0.55f, -0.20f));
        p.set(BoneId.ARM_L, BonePose.at(0.24f, 2.30f, 0f).rotationTo(0f, 0f, 20f));
        p.set(BoneId.ARM_R, BonePose.at(-0.30f, 2.20f, 0f).rotationTo(0f, 0f, -20f));
        return p;
    }

    /** 挥拳攻击 */
    public static Pose punch() {
        Pose p = idle();
        p.set(BoneId.ARM_R, BonePose.at(-0.38f, 1.62f, -0.40f).rotationTo(-40f, 0f, -15f));
        p.set(BoneId.HAND_R, BonePose.at(-0.52f, 1.45f, -0.75f));
        p.set(BoneId.TORSO, BonePose.at(0f, 1.74f, 0f).rotationTo(0f, 10f, 0f));
        return p;
    }

    /** 捧起（双手合拢在胸前，掌心向上） */
    public static Pose hold() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.25f, 1.25f, 0.20f).rotationTo(0f, 0f, 30f));
        p.set(BoneId.ARM_R, BonePose.at(-0.27f, 1.25f, 0.20f).rotationTo(0f, 0f, -30f));
        p.set(BoneId.HAND_L, BonePose.at(0.22f, 1.00f, 0.35f).rotationTo(0f, 0f, -90f));
        p.set(BoneId.HAND_R, BonePose.at(-0.22f, 1.00f, 0.35f).rotationTo(0f, 0f, 90f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.95f, 0.20f).rotationTo(-15f, 0f, 0f));
        return p;
    }

    /** 低头观察 */
    public static Pose observe() {
        Pose p = idle();
        p.set(BoneId.HEAD, BonePose.at(0f, 1.95f, 0.10f).rotationTo(-35f, 0f, 0f));
        p.set(BoneId.TORSO, BonePose.at(0f, 1.65f, 0f).rotationTo(-8f, 0f, 0f));
        return p;
    }

    /** 挡路（叉腰站立） */
    public static Pose blockPath() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.30f, 1.45f, 0f).rotationTo(0f, 0f, 80f));
        p.set(BoneId.ARM_R, BonePose.at(-0.32f, 1.45f, 0f).rotationTo(0f, 0f, -80f));
        p.set(BoneId.HAND_L, BonePose.at(0.48f, 1.20f, 0f));
        p.set(BoneId.HAND_R, BonePose.at(-0.48f, 1.20f, 0f));
        return p;
    }

    /** 伸懒腰 */
    public static Pose stretch() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.30f, 2.35f, -0.10f).rotationTo(0f, 0f, 15f));
        p.set(BoneId.ARM_R, BonePose.at(-0.32f, 2.32f, -0.10f).rotationTo(0f, 0f, -15f));
        p.set(BoneId.HAND_L, BonePose.at(0.40f, 2.62f, -0.25f));
        p.set(BoneId.HAND_R, BonePose.at(-0.40f, 2.60f, -0.25f));
        p.set(BoneId.HEAD, BonePose.at(0f, 2.22f, -0.05f).rotationTo(-15f, 0f, 0f));
        return p;
    }

    /** 依偎（v1.3）：双手拢在胸前，低头贴着怀中的小人类 */
    public static Pose cuddle() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.22f, 1.60f, 0.05f).rotationTo(0f, 0f, 45f));
        p.set(BoneId.ARM_R, BonePose.at(-0.24f, 1.60f, 0.05f).rotationTo(0f, 0f, -45f));
        p.set(BoneId.HAND_L, BonePose.at(0.20f, 1.35f, 0.30f).rotationTo(0f, 0f, -60f));
        p.set(BoneId.HAND_R, BonePose.at(-0.20f, 1.35f, 0.30f).rotationTo(0f, 0f, 60f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.90f, 0.25f).rotationTo(-20f, 0f, 0f));
        return p;
    }

    /** 低语（v1.3）：一手托起小人类贴到脸侧，侧头低语 */
    public static Pose whisper() {
        Pose p = idle();
        p.set(BoneId.TORSO, BonePose.at(0f, 1.74f, 0f).rotationTo(0f, 10f, 0f));
        p.set(BoneId.ARM_R, BonePose.at(-0.35f, 1.95f, -0.05f).rotationTo(0f, 0f, -50f));
        p.set(BoneId.HAND_R, BonePose.at(-0.30f, 1.85f, 0.15f).rotationTo(0f, 0f, -80f));
        p.set(BoneId.HEAD, BonePose.at(0f, 2.05f, -0.10f).rotationTo(-15f, -15f, 8f));
        return p;
    }

    /** 轻抚（v1.3）：一手轻轻按压怀中小人类的头顶 */
    public static Pose pat() {
        Pose p = idle();
        p.set(BoneId.ARM_R, BonePose.at(-0.30f, 1.85f, -0.10f).rotationTo(0f, 0f, -60f));
        p.set(BoneId.HAND_R, BonePose.at(-0.30f, 1.55f, 0.05f).rotationTo(0f, 0f, -90f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.90f, 0.20f).rotationTo(-18f, 0f, 0f));
        return p;
    }

    /** 举高高（v1.3）：双手把小人类举过头顶，仰头逗弄 */
    public static Pose lift() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.30f, 2.60f, -0.10f).rotationTo(0f, 0f, 20f));
        p.set(BoneId.ARM_R, BonePose.at(-0.32f, 2.58f, -0.10f).rotationTo(0f, 0f, -20f));
        p.set(BoneId.HAND_L, BonePose.at(0.35f, 2.85f, -0.15f).rotationTo(0f, 0f, -40f));
        p.set(BoneId.HAND_R, BonePose.at(-0.35f, 2.83f, -0.15f).rotationTo(0f, 0f, 40f));
        p.set(BoneId.HEAD, BonePose.at(0f, 2.20f, 0.15f).rotationTo(-25f, 0f, 0f));
        return p;
    }

    /** 轻吻（v1.4）：低头轻轻吻怀里小人类的额头（玩家位置=胸前HAND_R） */
    public static Pose kiss() {
        Pose p = idle();
        p.set(BoneId.HEAD, BonePose.at(0f, 1.75f, 0.35f).rotationTo(-38f, 0f, 0f));
        p.set(BoneId.ARM_L, BonePose.at(0.22f, 1.60f, 0.05f).rotationTo(0f, 0f, 40f));
        p.set(BoneId.ARM_R, BonePose.at(-0.24f, 1.60f, 0.05f).rotationTo(0f, 0f, -40f));
        p.set(BoneId.HAND_L, BonePose.at(0.20f, 1.35f, 0.30f).rotationTo(0f, 0f, -60f));
        p.set(BoneId.HAND_R, BonePose.at(-0.20f, 1.35f, 0.30f).rotationTo(0f, 0f, 60f));
        return p;
    }

    /** 蹭脸（v1.4）：侧头贴着手里的小人类蹭一蹭 */
    public static Pose nuzzle() {
        Pose p = idle();
        p.set(BoneId.HEAD, BonePose.at(0f, 1.95f, 0.20f).rotationTo(-20f, -20f, 10f));
        p.set(BoneId.ARM_L, BonePose.at(0.22f, 1.60f, 0.05f).rotationTo(0f, 0f, 40f));
        p.set(BoneId.ARM_R, BonePose.at(-0.28f, 1.60f, 0.15f).rotationTo(0f, 0f, -45f));
        p.set(BoneId.HAND_L, BonePose.at(0.20f, 1.40f, 0.25f).rotationTo(0f, 0f, -60f));
        p.set(BoneId.HAND_R, BonePose.at(-0.26f, 1.45f, 0.25f).rotationTo(0f, 0f, -70f));
        return p;
    }

    /** 颠一颠（v1.4）：托到眼前逗弄（玩家位置=脸前） */
    public static Pose bounce() {
        Pose p = idle();
        p.set(BoneId.ARM_L, BonePose.at(0.25f, 1.75f, 0.05f).rotationTo(0f, 0f, 30f));
        p.set(BoneId.ARM_R, BonePose.at(-0.27f, 1.75f, 0.05f).rotationTo(0f, 0f, -30f));
        p.set(BoneId.HAND_L, BonePose.at(0.22f, 1.55f, 0.15f).rotationTo(0f, 0f, -45f));
        p.set(BoneId.HAND_R, BonePose.at(-0.22f, 1.55f, 0.15f).rotationTo(0f, 0f, 45f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.95f, 0.05f).rotationTo(-15f, 0f, 0f));
        return p;
    }

    /** 单手抱（v1.4）：托在腰侧抱着走（玩家位置=腰侧） */
    public static Pose carry() {
        Pose p = idle();
        p.set(BoneId.ARM_R, BonePose.at(-0.35f, 1.45f, 0.05f).rotationTo(0f, 0f, -55f));
        p.set(BoneId.HAND_R, BonePose.at(-0.32f, 1.30f, 0.15f).rotationTo(0f, 0f, -75f));
        p.set(BoneId.HEAD, BonePose.at(0f, 2.00f, -0.05f).rotationTo(-5f, 5f, 0f));
        return p;
    }

    /** 枕膝（v1.4）：坐着把小人类放在膝上（玩家位置=膝上） */
    public static Pose lap() {
        Pose p = sit();
        p.set(BoneId.HAND_L, BonePose.at(0.22f, 0.55f, 0.35f).rotationTo(0f, 0f, -30f));
        p.set(BoneId.HAND_R, BonePose.at(-0.22f, 0.55f, 0.35f).rotationTo(0f, 0f, 30f));
        p.set(BoneId.ARM_L, BonePose.at(0.25f, 0.80f, 0.15f).rotationTo(0f, 0f, 35f));
        p.set(BoneId.ARM_R, BonePose.at(-0.27f, 0.80f, 0.15f).rotationTo(0f, 0f, -35f));
        p.set(BoneId.HEAD, BonePose.at(0f, 1.30f, 0f).rotationTo(-12f, 0f, 0f));
        return p;
    }
}
