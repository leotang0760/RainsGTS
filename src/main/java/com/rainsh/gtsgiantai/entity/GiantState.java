package com.rainsh.gtsgiantai.entity;

/**
 * 巨人生命周期状态。
 * 行为树根节点优先级即按此排列：CHASE > ALERT > IDLE > SLEEP。
 */
public enum GiantState {
    SLEEP,
    IDLE,
    ALERT,
    CHASE;

    public static GiantState fromString(String s) {
        for (GiantState g : values()) {
            if (g.name().equalsIgnoreCase(s)) return g;
        }
        return IDLE;
    }
}
