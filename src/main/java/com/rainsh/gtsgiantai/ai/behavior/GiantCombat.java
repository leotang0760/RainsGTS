package com.rainsh.gtsgiantai.ai.behavior;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.BoneId;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.util.LocUtil;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 巨人战斗判定：踩踏AOE / 挥拳 / 踢飞。
 * 伤害随体型倍率缩放，贴近GTS"体型压制"体验。
 */
public final class GiantCombat {

    private GiantCombat() {
    }

    /** 踩踏：脚部位置AOE伤害+击飞+尘土粒子 */
    public static void stomp(GTSGiantAI plugin, GiantEntity giant) {
        World world = giant.getWorld();
        Vec3 foot = giant.boneWorld(BoneId.FOOT_R, giant.getAnim().currentPose());
        Location loc = new Location(world, foot.x, foot.y, foot.z);
        double radius = 2.5 + giant.getScale() * 0.35;
        double damage = (3 + giant.getScale() * 0.6) * giant.getAttackMultiplier();

        // 粒子 + 音效
        world.spawnParticle(Particle.BLOCK_CRACK, loc, 60, radius, 0.3, radius, 0.5,
                world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ()).getBlockData());
        world.spawnParticle(Particle.CLOUD, loc, 30, radius * 0.6, 0.2, radius * 0.6, 0.1);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.4f);

        for (Player p : world.getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (p.getLocation().distanceSquared(loc) > radius * radius) continue;
            double dmg = damage * (1 - p.getLocation().distance(loc) / radius * 0.5);
            p.damage(Math.max(1, dmg), giant.getFirstDisplayEntity());
            p.setVelocity(new Vector(0, 0.6, 0).add(
                    p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8)));
        }
    }

    /** 挥拳：前方单体伤害 */
    public static void punch(GTSGiantAI plugin, GiantEntity giant, LivingEntity target) {
        if (target == null || target.isDead()) return;
        double damage = (4 + giant.getScale() * 0.8) * giant.getAttackMultiplier();
        target.damage(damage, giant.getFirstDisplayEntity());
        target.setVelocity(target.getLocation().getDirection().multiply(0.6).setY(0.5));
        giant.getWorld().playSound(new Location(giant.getWorld(),
                giant.getAnchor().x, giant.getAnchor().y, giant.getAnchor().z),
                Sound.ENTITY_PLAYER_ATTACK_STRONG, 1.5f, 0.7f);
    }

    /** 踢飞：把玩家抛远（无聊行为/戏弄，低伤害高击飞） */
    public static void kick(GTSGiantAI plugin, GiantEntity giant, Player target) {
        if (target == null || !target.isOnline()) return;
        Vec3 anchor = giant.getAnchor();
        Vector dir = target.getLocation().toVector().subtract(
                new Vector(anchor.x, anchor.y, anchor.z)).normalize();
        target.setVelocity(dir.multiply(2.0).setY(1.0));
        target.damage(2, giant.getFirstDisplayEntity());
        giant.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.8f);
    }

    /** 是否在攻击范围（按体型放大） */
    public static boolean inAttackRange(GiantEntity giant, Player target) {
        double range = 2.5 + giant.getScale() * 0.3;
        return LocUtil.of(target.getLocation()).distanceSq(giant.getAnchor()) <= range * range;
    }

    /** 附近可攻击的生物（玩家 + 原版生物），手动距离过滤，避免API签名差异 */
    public static List<LivingEntity> nearbyLiving(GiantEntity giant, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 a = giant.getAnchor();
        double r2 = radius * radius;
        for (Player p : giant.getWorld().getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            if (LocUtil.of(p.getLocation()).distanceSq(a) <= r2) out.add(p);
        }
        for (LivingEntity le : giant.getWorld().getLivingEntities()) {
            if (le instanceof Player) continue;
            if (LocUtil.of(le.getLocation()).distanceSq(a) <= r2) out.add(le);
        }
        return out;
    }
}
