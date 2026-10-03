package com.rainsh.gtsgiantai.entity;

/**
 * 巨人心情系统：影响空闲/亲密/攻击行为的权重分布。
 */
public enum Mood {
    CALM,
    PLAYFUL,
    ANNOYED,
    AGGRESSIVE;

    public static Mood fromString(String s) {
        for (Mood m : values()) {
            if (m.name().equalsIgnoreCase(s)) return m;
        }
        return CALM;
    }
}
