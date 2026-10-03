package com.rainsh.gtsgiantai.entity;

/**
 * 巨人骨骼分段枚举。
 * 每段对应一个 Display 渲染实体 + 一个 Interaction 碰撞盒。
 */
public enum BoneId {
    HEAD("head", 0.35, 0.50, 0.35),
    TORSO("torso", 0.40, 0.85, 0.35),
    ARM_L("arm_l", 0.22, 0.60, 0.22),
    ARM_R("arm_r", 0.22, 0.60, 0.22),
    HAND_L("hand_l", 0.15, 0.15, 0.15),
    HAND_R("hand_r", 0.15, 0.15, 0.15),
    LEG_L("leg_l", 0.25, 0.80, 0.25),
    LEG_R("leg_r", 0.25, 0.80, 0.25),
    FOOT_L("foot_l", 0.22, 0.20, 0.42),
    FOOT_R("foot_r", 0.22, 0.20, 0.42);

    private final String key;
    /** 基准碰撞盒半宽（scale=1时，格） */
    private final double halfW;
    /** 基准碰撞盒半高（scale=1时，格） */
    private final double halfH;
    /** 基准碰撞盒半深（scale=1时，格） */
    private final double halfD;

    BoneId(String key, double halfW, double halfH, double halfD) {
        this.key = key;
        this.halfW = halfW;
        this.halfH = halfH;
        this.halfD = halfD;
    }

    public String getKey() {
        return key;
    }

    public double getHalfW() {
        return halfW;
    }

    public double getHalfH() {
        return halfH;
    }

    public double getHalfD() {
        return halfD;
    }

    public static BoneId fromKey(String key) {
        for (BoneId b : values()) {
            if (b.key.equalsIgnoreCase(key)) return b;
        }
        return null;
    }
}
