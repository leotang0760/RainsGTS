package com.rainsh.gtsgiantai.util;

/**
 * 轻量三维向量（避免引入外部向量库，零依赖）
 */
public final class Vec3 {

    public double x, y, z;

    public Vec3() {
        this(0, 0, 0);
    }

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 copy() {
        return new Vec3(x, y, z);
    }

    public Vec3 add(Vec3 o) {
        this.x += o.x;
        this.y += o.y;
        this.z += o.z;
        return this;
    }

    public Vec3 add(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        return this;
    }

    public Vec3 mul(double m) {
        this.x *= m;
        this.y *= m;
        this.z *= m;
        return this;
    }

    public double lengthSq() {
        return x * x + y * y + z * z;
    }

    public double length() {
        return Math.sqrt(lengthSq());
    }

    public double distanceSq(Vec3 o) {
        double dx = x - o.x;
        double dy = y - o.y;
        double dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double distance(Vec3 o) {
        return Math.sqrt(distanceSq(o));
    }

    /** 线性插值：this -> target，t ∈ [0,1] */
    public Vec3 lerp(Vec3 target, double t) {
        this.x += (target.x - x) * t;
        this.y += (target.y - y) * t;
        this.z += (target.z - z) * t;
        return this;
    }

    /** 逐分量取最小值（用于AABB） */
    public static Vec3 min(Vec3 a, Vec3 b) {
        return new Vec3(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.min(a.z, b.z));
    }

    /** 逐分量取最大值（用于AABB） */
    public static Vec3 max(Vec3 a, Vec3 b) {
        return new Vec3(Math.max(a.x, b.x), Math.max(a.y, b.y), Math.max(a.z, b.z));
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}
