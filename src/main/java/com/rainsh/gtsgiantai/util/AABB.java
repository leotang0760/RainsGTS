package com.rainsh.gtsgiantai.util;

/**
 * 轴对齐包围盒（AABB），用于自定义碰撞检测。
 * Interaction 实体没有原版碰撞事件，触碰/踩踏/唤醒全部由本工具判定。
 */
public final class AABB {

    public final Vec3 min;
    public final Vec3 max;

    public AABB(Vec3 min, Vec3 max) {
        this.min = min;
        this.max = max;
    }

    /** 由中心点 + 半尺寸构造 */
    public static AABB ofCenter(Vec3 center, double halfW, double halfH, double halfD) {
        return new AABB(
                new Vec3(center.x - halfW, center.y - halfH, center.z - halfD),
                new Vec3(center.x + halfW, center.y + halfH, center.z + halfD));
    }

    public boolean containsPoint(Vec3 p) {
        return p.x >= min.x && p.x <= max.x
                && p.y >= min.y && p.y <= max.y
                && p.z >= min.z && p.z <= max.z;
    }

    /** 与另一个AABB是否相交 */
    public boolean intersects(AABB o) {
        return min.x <= o.max.x && max.x >= o.min.x
                && min.y <= o.max.y && max.y >= o.min.y
                && min.z <= o.max.z && max.z >= o.min.z;
    }

    /** 与玩家AABB（0.6宽 × 1.8高，底部在脚底）是否相交 */
    public boolean intersectsPlayer(double px, double py, double pz) {
        return intersects(new AABB(
                new Vec3(px - 0.3, py, pz - 0.3),
                new Vec3(px + 0.3, py + 1.8, pz + 0.3)));
    }

    /** 把点从盒内推到最近表面（用于玩家卡入巨人身体时推出），返回修正后的坐标 */
    public Vec3 pushOut(Vec3 p, double margin) {
        double cx = clamp(p.x, min.x, max.x);
        double cy = clamp(p.y, min.y, max.y);
        double cz = clamp(p.z, min.z, max.z);
        double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 1e-6) {
            // 玩家完全在盒内且恰好在中心：往上推出
            return new Vec3(p.x, max.y + margin, p.z);
        }
        double k = margin / d;
        return new Vec3(p.x + dx * k, p.y + dy * k, p.z + dz * k);
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    @Override
    public String toString() {
        return "AABB" + min + " ~ " + max;
    }
}
