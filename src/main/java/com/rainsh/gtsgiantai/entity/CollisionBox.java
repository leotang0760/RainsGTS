package com.rainsh.gtsgiantai.entity;

import com.rainsh.gtsgiantai.util.AABB;
import com.rainsh.gtsgiantai.util.LocUtil;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 分段碰撞盒：一个 Interaction 实体 + 关联骨骼 + 动态AABB。
 * Interaction 负责"原版可交互"（可被点击/攻击事件），
 * AABB 负责我们自己的触碰/唤醒/踩踏检测。
 */
public final class CollisionBox {

    private final BoneId bone;
    private final Interaction entity;
    private final double halfW, halfH, halfD;
    private AABB aabb;

    /** 分层掩码 */
    public enum Layer {
        WAKE_TRIGGER,      // 触碰唤醒
        HAND_INTERACT,     // 手部交互（捧起/戏弄）
        BODY_BLOCK,        // 身体阻挡
        STOMP_DAMAGE       // 脚部踩踏伤害
    }

    private final List<Layer> layers = new ArrayList<>();

    public CollisionBox(World world, Location anchor, BoneId bone,
                        double halfW, double halfH, double halfD,
                        Layer... layerArr) {
        this.bone = bone;
        this.halfW = halfW;
        this.halfH = halfH;
        this.halfD = halfD;
        this.entity = world.spawn(anchor, Interaction.class);
        this.entity.setInteractionWidth((float) Math.min(16, halfW * 2));
        this.entity.setInteractionHeight((float) Math.min(16, halfH * 2));
        // Interaction 默认无重力、无敌、不可移动
        this.entity.setInvulnerable(true);
        this.entity.setSilent(true);
        this.entity.setPersistent(true);
        for (Layer l : layerArr) layers.add(l);
        this.aabb = AABB.ofCenter(LocUtil.of(anchor), halfW, halfH, halfD);
    }

    /** 每tick更新到骨骼世界坐标 */
    public void update(Vec3 boneWorld, double scale) {
        double w = halfW * scale;
        double h = halfH * scale;
        double d = halfD * scale;
        this.aabb = AABB.ofCenter(boneWorld, w, h, d);
        this.entity.teleport(LocUtil.toLocation(boneWorld, entity.getLocation()));
        // Interaction 尺寸上限16，超大巨人允许大于16则钳制（碰撞判定仍走AABB，不受影响）
        this.entity.setInteractionWidth((float) Math.min(16, w * 2));
        this.entity.setInteractionHeight((float) Math.min(16, h * 2));
    }

    public boolean hasLayer(Layer layer) {
        return layers.contains(layer);
    }

    public AABB getAabb() {
        return aabb;
    }

    public BoneId getBone() {
        return bone;
    }

    public Interaction getEntity() {
        return entity;
    }

    /** 是否与玩家相交（触碰检测） */
    public boolean intersectsPlayer(Player p) {
        return aabb.intersectsPlayer(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ());
    }

    public void remove() {
        entity.remove();
    }
}
