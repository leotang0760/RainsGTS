package com.rainsh.gtsgiantai.listener;

import com.rainsh.gtsgiantai.GTSGiantAI;
import com.rainsh.gtsgiantai.entity.BoneId;
import com.rainsh.gtsgiantai.entity.GiantEntity;
import com.rainsh.gtsgiantai.entity.GiantState;
import com.rainsh.gtsgiantai.event.GiantInteractEvent;
import com.rainsh.gtsgiantai.manager.GiantManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * 玩家与巨人交互监听器：
 * 1. 攻击巨人的渲染/碰撞体 → 不造成实体伤害，转为部位弱点伤害 + 唤醒 + 仇恨
 * 2. 右键巨人的碰撞体 → 亲密交互（捧起），按权限/潜行/状态过滤
 * 3. 玩家进服 → 自动推送内置模型资源包（零下载零操作）
 */
public final class PlayerListener implements Listener {

    private final GTSGiantAI plugin;
    private final GiantManager manager;

    public PlayerListener(GTSGiantAI plugin) {
        this.plugin = plugin;
        this.manager = plugin.getGiantManager();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("resourcepack.auto", true)) return;
        var rps = plugin.getResourcePackServer();
        if (rps == null || !rps.isReady()) return;
        int delay = Math.max(0, plugin.getConfig().getInt("resourcepack.delay", 60));
        plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin,
                task -> {
                    if (e.getPlayer().isOnline()) {
                        rps.pushTo(e.getPlayer());
                    }
                }, delay);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof Interaction) && !(e.getEntity() instanceof ItemDisplay)) return;

        GiantEntity g = manager.findByEntity(e.getEntity());
        if (g == null) return;
        // 渲染体/碰撞体不可被击杀，取消原版实体伤害
        e.setCancelled(true);

        // 命中部位判定
        BoneId hitBone = g.findBoneByEntity(e.getEntity());

        // 部位弱点伤害（Boss血量系统）
        if (g.isBoss() && hitBone != null && !g.isDead()) {
            double raw = Math.max(1, e.getFinalDamage() > 0 ? e.getFinalDamage() : 5);
            g.damageByPlayer(p, hitBone, raw);
            if (hitBone == BoneId.HEAD && !g.isDead()) {
                p.sendMessage(plugin.msg("&c弱点命中！头部造成3倍伤害"));
            }
        }

        // 攻击 → 唤醒 + 仇恨（亲密中断）
        g.setLastDamageTick(System.currentTimeMillis());
        g.sayHurt(p);
        if (g.getState() == GiantState.SLEEP) {
            plugin.msg(p, "player.wake-up");
            g.setState(GiantState.ALERT);
        }
        if (g.getState() != GiantState.CHASE) {
            g.startChase(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Interaction)) return;
        Player p = e.getPlayer();
        GiantEntity g = manager.findByEntity(e.getRightClicked());
        if (g == null) return;

        // 过滤
        if (manager.isExempt(p)) return;               // no-interact 权限
        if (g.getState() == GiantState.CHASE) return;  // 战斗中不亲密
        if (p.isSneaking()) return;                    // 潜行不触发

        // v1.3 喂食互动：主手拿食物右键 → 喂给巨人
        var held = p.getInventory().getItemInMainHand();
        if (plugin.getConfig().getBoolean("dialogue.feed", true)
                && held != null && !held.getType().isAir() && held.getType().isEdible()) {
            g.feed(p);
            e.setCancelled(true);
            p.sendMessage(plugin.msg("&a" + g.getDisplayName() + " 收下了你的投喂，心情好多了 ♡"));
            return;
        }

        // 已有玩家被抓取：被捧着的玩家右键 → 手动切换亲密动作（v1.4）
        if (g.getGrabbedPlayerId() != null) {
            if (p.getUniqueId().equals(g.getGrabbedPlayerId())) {
                g.nextIntimateAction(p);
                e.setCancelled(true);
                p.sendMessage(plugin.msg("&d巨人换了个姿势把你捧在手中……"));
            }
            return;
        }

        // 亲密交互：捧起
        g.grabPlayer(p);
        e.setCancelled(true);
        Bukkit.getPluginManager().callEvent(new GiantInteractEvent(g, p, GiantInteractEvent.Action.GRAB));
    }
}
