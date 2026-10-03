package com.rainsh.gtsgiantai.entity;

import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 骨骼姿势：某一段骨骼相对巨人锚点（脚底中心）的局部变换。
 */
public final class BonePose {

    public final Vector3f translation;
    public final Quaternionf rotation;
    public final Vector3f scale;

    public BonePose(Vector3f translation, Quaternionf rotation, Vector3f scale) {
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
    }

    public static BonePose at(float x, float y, float z) {
        return new BonePose(new Vector3f(x, y, z), new Quaternionf(), new Vector3f(1, 1, 1));
    }

    public static BonePose at(float x, float y, float z, float rotDegY) {
        return new BonePose(new Vector3f(x, y, z),
                new Quaternionf().rotationY((float) Math.toRadians(rotDegY)),
                new Vector3f(1, 1, 1));
    }

    /** 设置XYZ欧拉角旋转（度），返回this。X=俯仰，Y=偏航，Z=翻滚。 */
    public BonePose rotationTo(float rotX, float rotY, float rotZ) {
        this.rotation.rotationXYZ((float) Math.toRadians(rotX),
                (float) Math.toRadians(rotY),
                (float) Math.toRadians(rotZ));
        return this;
    }

    public Transformation toTransformation() {
        return new Transformation(translation, rotation, scale, new Quaternionf());
    }

    /** 深拷贝 */
    public BonePose copy() {
        return new BonePose(new Vector3f(translation), new Quaternionf(rotation), new Vector3f(scale));
    }

    /** 向目标姿势插值，t ∈ [0,1]。四元数用nlerp保证平滑且廉价。 */
    public void lerpTo(BonePose target, float t) {
        translation.lerp(target.translation, t);
        rotation.nlerp(target.rotation, t);
        scale.lerp(target.scale, t);
    }
}
