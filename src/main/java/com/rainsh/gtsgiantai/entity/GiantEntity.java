package com.rainsh.gtsgiantai.entity;

import com.rainsh.gtsgiantai.ai.GiantBrain;
import com.rainsh.gtsgiantai.manager.GiantManager;
import com.rainsh.gtsgiantai.util.LocUtil;
import com.rainsh.gtsgiantai.util.Vec3;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 巨人主实体：管理全部 Display 渲染体 + Interaction 碰撞盒 + 行为树 + 状态。
 * 渲染：锚点（脚底中心）+ 每段骨骼局部变换（translation*scale / rotation / scale*scale）。
 * 碰撞：每段骨骼世界坐标由 Matrix4f 计算，Interaction 定位到该点，AABB 自行判定。
 */
public class GiantEntity {

    private final UUID id;
    private String typeName;
    private String displayName;
    private double scale;
    private GiantState state = GiantState.IDLE;
    private Mood mood = Mood.CALM;
    private int boredom = 0;
    private int cooldownAfterChase = 0;
    private UUID grabbedPlayerId = null;
    private long lastWakeTick = 0;
    private long lastDamageTick = 0;

    private World world;
    private Vec3 anchor;
    private float yaw = 0f;

    private final GiantManager manager;
    private final Map<BoneId, BoneDisplay> displays = new EnumMap<>(BoneId.class);
    private final Map<BoneId, CollisionBox> boxes = new EnumMap<>(BoneId.class);
    private final AnimationController anim;
    private GiantBrain brain;
    private boolean spawned = false;
    private int tickCounter = 0;

    private Player chaseTarget = null;

    // ---- Boss 血量/阶段 ----
    private double hp = 0;
    private double maxHp = 0;
    private int phase = 1;
    private boolean dead = false;

    // ---- 抓取挣扎 ----
    private org.bukkit.boss.BossBar grabBar = null;
    private double grabProgress = 0;
    private int squeezeTick = 0;

    // ---- 对话（v1.3） ----
    private com.rainsh.gtsgiantai.dialogue.DialogueEngine dialogue = null;
    private int nextDialogueTick = 0;
    private int statusBarTick = 0;

    // ---- 心动值（v1.4） ----
    private double affection = 0;
    private int affectionLevel = 1;
    private int nextIntimateTick = 0;
    private int affectionTick = 0;
    private int ambientTick = 0;

    public GiantEntity(GiantManager manager, UUID id, String typeName, double scale) {
        this.manager = manager;
        this.id = id;
        this.typeName = typeName;
        this.scale = Math.max(2.5, scale);
        this.displayName = typeName;
        this.anim = new AnimationController(PoseLibrary.idle());
    }

    // ---------------- 生成 / 销毁 ----------------

    public void spawn(Location loc) {
        this.world = loc.getWorld();
        this.anchor = LocUtil.of(loc);
        this.yaw = loc.getYaw();
        Material mat = manager.getBaseMaterial();
        int cmd = manager.getCustomModelData();
        this.dialogue = manager.getPlugin().getDialogue();

        for (BoneId bone : BoneId.values()) {
            BoneDisplay bd = new BoneDisplay(world, loc, bone, new ItemStack(mat), cmd);
            displays.put(bone, bd);
            CollisionBox box = new CollisionBox(world, loc, bone,
                    bone.getHalfW() + 0.05, bone.getHalfH() + 0.05, bone.getHalfD() + 0.05,
                    layerFor(bone));
            boxes.put(bone, box);
        }
        this.spawned = true;
        applyPose(PoseLibrary.idle());
        syncAllCollision(PoseLibrary.idle());
        setState(GiantState.IDLE);
        this.brain = new GiantBrain(manager.getPlugin(), this);
        initHealth();
    }

    /** 初始化血量：base + scale * per_scale */
    public void initHealth() {
        org.bukkit.configuration.file.FileConfiguration cfg = manager.getPlugin().getConfig();
        if (!cfg.getBoolean("boss.enabled", true)) {
            this.maxHp = 0;
            this.hp = 0;
            this.phase = 1;
            return;
        }
        this.maxHp = cfg.getDouble("boss.hp_base", 100) + scale * cfg.getDouble("boss.hp_per_scale", 20);
        this.hp = maxHp;
        this.phase = 1;
        this.dead = false;
    }

    private CollisionBox.Layer[] layerFor(BoneId bone) {
        switch (bone) {
            case HAND_L:
            case HAND_R:
                return new CollisionBox.Layer[]{CollisionBox.Layer.HAND_INTERACT};
            case FOOT_L:
            case FOOT_R:
                return new CollisionBox.Layer[]{
                        CollisionBox.Layer.WAKE_TRIGGER, CollisionBox.Layer.STOMP_DAMAGE};
            default:
                return new CollisionBox.Layer[]{
                        CollisionBox.Layer.BODY_BLOCK, CollisionBox.Layer.WAKE_TRIGGER};
        }
    }

    public void despawn() {
        for (BoneDisplay bd : displays.values()) bd.remove();
        for (CollisionBox cb : boxes.values()) cb.remove();
        displays.clear();
        boxes.clear();
        spawned = false;
    }

    // ---------------- Tick ----------------

    public void tick(long globalTick) {
        if (!spawned) return;
        tickCounter++;
        if (cooldownAfterChase > 0) cooldownAfterChase--;

        // 动画推进
        PoseLibrary.Pose pose = anim.tick();
        applyPose(pose);

        // 碰撞同步（频率分级：战斗1 / 空闲3 / 休眠20）
        int syncInterval = state == GiantState.CHASE ? 1
                : state == GiantState.SLEEP ? manager.getCfg().syncIntervalSleep()
                : manager.getCfg().syncIntervalIdle();
        if (tickCounter % Math.max(1, syncInterval) == 0) {
            syncAllCollision(pose);
            pushOutPlayers();
            syncGrabbedPlayer();
        }

        // 唤醒检测（CHASE 状态下不检测；SLEEP/IDLE 检测）
        if (state != GiantState.CHASE) {
            int scanInterval = state == GiantState.SLEEP ? 20 : 10;
            if (tickCounter % scanInterval == 0) {
                detectWake();
            }
        }

        // 行为树 AI tick（分级频率）
        if (brain != null) {
            int aiInterval = state == GiantState.CHASE ? 1
                    : state == GiantState.SLEEP ? manager.getCfg().tickIntervalSleep()
                    : manager.getCfg().tickIntervalIdle();
            if (tickCounter % Math.max(1, aiInterval) == 0) {
                brain.tick();
            }
        }

        // 无聊值累加
        if (state == GiantState.IDLE) {
            boredom++;
        }

        // 对话（v1.3）：间隔触发 + 状态悬浮条
        if (dialogue != null && dialogue.isEnabled()) {
            if (tickCounter >= nextDialogueTick) {
                if (dialogue.autoSay(this)) {
                    nextDialogueTick = tickCounter + dialogue.getInterval()
                            + (int) (Math.random() * dialogue.getInterval());
                } else {
                    nextDialogueTick = tickCounter + 60;
                }
            }
            if (++statusBarTick >= 40) {
                statusBarTick = 0;
                org.bukkit.Location aloc = new org.bukkit.Location(world, anchor.x, anchor.y, anchor.z);
                for (org.bukkit.entity.Player p : world.getNearbyPlayers(aloc, 20)) {
                    dialogue.statusBar(this, p);
                }
            }
        }

        // 环境音（v1.4）：附近玩家听到低沉心跳/低语
        if (++ambientTick >= 100) {
            ambientTick = 0;
            if (manager.getPlugin().getConfig().getBoolean("dialogue.ambient_sound", true)
                    && !world.getNearbyPlayers(new org.bukkit.Location(world, anchor.x, anchor.y, anchor.z), 15).isEmpty()) {
                org.bukkit.Sound s = Math.random() < 0.5
                        ? org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS
                        : org.bukkit.Sound.ENTITY_EVOKER_AMBIENT;
                world.playSound(new org.bukkit.Location(world, anchor.x, anchor.y + 1, anchor.z),
                        s, (float) Math.min(1.2, 0.3 + scale * 0.04), 0.4f);
            }
        }
    }

    // ---------------- 姿势应用 ----------------

    private void applyPose(PoseLibrary.Pose pose) {
        // 脚部IK：贴合地形
        if (manager.getPlugin().getConfig().getBoolean("ik.enabled", true)) {
            applyFootIK(pose);
        }
        for (Map.Entry<BoneId, BonePose> e : pose.all().entrySet()) {
            BoneDisplay bd = displays.get(e.getKey());
            if (bd == null) continue;
            BonePose bp = e.getValue();
            bd.applyPose(new BonePose(
                    new Vector3f(bp.translation).mul((float) scale),
                    bp.rotation,
                    new Vector3f(bp.scale).mul((float) scale)));
        }
    }

    /** 脚部IK：采样脚下地形高度，整体平移锚点使脚贴合地面（身体联动、比例保持，限制最大步差防悬崖穿模） */
    private void applyFootIK(PoseLibrary.Pose pose) {
        double maxStep = manager.getPlugin().getConfig().getDouble("ik.max_step_height", 2.0);
        double totalDiff = 0;
        int n = 0;
        for (BoneId foot : new BoneId[]{BoneId.FOOT_L, BoneId.FOOT_R}) {
            BonePose bp = pose.get(foot);
            if (bp == null) continue;
            Vec3 w = boneWorld(foot, pose);
            int gx = (int) Math.floor(w.x);
            int gz = (int) Math.floor(w.z);
            if (world.getBlockAt(gx, (int) Math.floor(w.y) + 1, gz).isLiquid()) continue;
            int groundY = world.getHighestBlockYAt(gx, gz);
            // 渲染脚底（世界）：anchor + (translation.y + 元素底偏移) * scale；脚元素底约 -0.21~-0.24 格
            double footBottom = w.y - 0.22 * scale;
            double diff = (groundY + 0.05) - footBottom;
            if (Math.abs(diff) > maxStep) continue;
            totalDiff += diff;
            n++;
        }
        if (n > 0) {
            // 整体平移锚点（世界格），所有骨骼相对 anchor 上堆 → 身体联动、比例不破坏
            anchor.y += totalDiff / n;
        }
    }

    /** 计算骨骼世界坐标：锚点 + 旋转(局部平移*scale) */
    public Vec3 boneWorld(BoneId bone, PoseLibrary.Pose pose) {
        BonePose bp = pose.get(bone);
        if (bp == null) return anchor.copy();
        Matrix4f m = new Matrix4f()
                .translate((float) anchor.x, (float) anchor.y, (float) anchor.z)
                .rotate(bp.rotation)
                .translate(new Vector3f(bp.translation).mul((float) scale));
        Vector3f out = new Vector3f();
        m.transformPosition(0f, 0f, 0f, out);
        return new Vec3(out.x, out.y, out.z);
    }

    private void syncAllCollision(PoseLibrary.Pose pose) {
        for (BoneId bone : BoneId.values()) {
            CollisionBox cb = boxes.get(bone);
            if (cb == null) continue;
            cb.update(boneWorld(bone, pose), scale);
        }
    }

    // ---------------- 唤醒检测 ----------------

    private void detectWake() {
        boolean huge = scale >= manager.getCfg().hugeMinScale();
        double alertRange = manager.getCfg().smallAlertRange();
        double probeRange = huge ? Math.max(8, alertRange * 0.5) : alertRange;
        double probeSq = probeRange * probeRange;

        for (Player p : world.getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            Vec3 pp = LocUtil.of(p.getLocation());
            if (pp.distanceSq(anchor) > probeSq) continue;
            // 潜行免疫
            if (p.isSneaking() && manager.getCfg().sneakImmune()) continue;
            if (manager.isExempt(p)) continue;

            boolean wake = false;
            if (huge) {
                // 大体型：仅触碰碰撞盒唤醒
                for (CollisionBox cb : boxes.values()) {
                    if (cb.hasLayer(CollisionBox.Layer.WAKE_TRIGGER) && cb.intersectsPlayer(p)) {
                        wake = true;
                        break;
                    }
                }
            } else {
                // 小体型：进入警戒半径唤醒
                wake = true;
            }

            if (wake) {
                if (state == GiantState.SLEEP) {
                    manager.getPlugin().msg(p, "player.wake-up");
                }
                setState(GiantState.ALERT);
                lastWakeTick = System.currentTimeMillis();
                anim.play(PoseLibrary.observe(), 20, 60, "wake");
                break;
            }
        }
    }

    // ---------------- 玩家推离（防穿模） ----------------

    private void pushOutPlayers() {
        if (!manager.getCfg().playerPushOut()) return;
        if (grabbedPlayerId != null) return; // 捧起状态不推离
        double probe = 6 + scale;
        double probeSq = probe * probe;
        for (Player p : world.getPlayers()) {
            if (!p.isOnline() || p.isDead()) continue;
            Vec3 pos = LocUtil.of(p.getLocation());
            if (pos.distanceSq(anchor) > probeSq) continue;
            if (p.isSneaking() && manager.getCfg().sneakImmune()) continue;
            for (CollisionBox cb : boxes.values()) {
                if (!cb.hasLayer(CollisionBox.Layer.BODY_BLOCK)) continue;
                if (cb.getAabb().intersectsPlayer(pos.x, pos.y, pos.z)) {
                    Vec3 pushed = cb.getAabb().pushOut(pos, 0.05);
                    p.teleport(new Location(world, pushed.x, pushed.y, pushed.z,
                            p.getLocation().getYaw(), p.getLocation().getPitch()));
                    break;
                }
            }
        }
    }

    // ---------------- 亲密互动（v1.3/v1.4：喂食/心形粒子/心动值） ----------------

    /** 喂食：心动值+10、mood 提升一档 + 夸赞对话 + 心形粒子 + 短暂捧抱姿势 */
    public void feed(Player p) {
        addAffection(10, p);
        if (mood.ordinal() < Mood.PLAYFUL.ordinal()) {
            setMood(Mood.values()[mood.ordinal() + 1]);
        }
        if (dialogue != null) {
            dialogue.say(this, "praise", p);
        }
        hearts();
        anim.play(PoseLibrary.cuddle(), 10, 40, "cuddle");
    }

    /** 心动值累积：满25升1级（Lv1陌生→Lv5专属），升级时祝贺对话+心形粒子 */
    public void addAffection(double amount, Player p) {
        int before = affectionLevel;
        affection = Math.min(100, affection + amount);
        affectionLevel = 1 + (int) (affection / 25);
        if (affectionLevel > before) {
            if (dialogue != null) {
                dialogue.say(this, "levelup", p);
            }
            hearts();
            if (p != null) {
                p.sendMessage(manager.getPlugin().msg("&d♡ 心动等级提升至 Lv" + affectionLevel + "！"));
            }
        }
    }

    public double getAffection() {
        return affection;
    }

    public int getAffectionLevel() {
        return affectionLevel;
    }

    /** 手动/自动切换亲密动作（捧起演出用） */
    public void nextIntimateAction(Player target) {
        String[] acts = {"cuddle", "whisper", "pat", "lift", "kiss", "nuzzle", "bounce", "carry"};
        String act = acts[(int) (Math.random() * acts.length)];
        PoseLibrary.Pose pose = manager.resolvePose(act);
        if (pose != null) {
            anim.play(pose, 12, 100, act);
        }
        if (dialogue != null) {
            String cat = target != null && Math.random() < 0.5 ? "tease" : "affection";
            dialogue.say(this, cat, target);
        }
    }

    /** 心形粒子：在胸口位置喷出（TORSO 骨骼上方） */
    public void hearts() {
        if (!manager.getPlugin().getConfig().getBoolean("dialogue.hearts", true)) return;
        Vec3 chest = boneWorld(BoneId.TORSO, anim.currentPose());
        world.spawnParticle(org.bukkit.Particle.HEART,
                new org.bukkit.Location(world, chest.x, chest.y + 0.25, chest.z),
                6, 0.4 * scale, 0.3 * scale, 0.4 * scale, 0.01);
    }

    // ---------------- 捧起玩家（挣扎进度条） ----------------

    public void grabPlayer(Player p) {
        this.grabbedPlayerId = p.getUniqueId();
        this.grabProgress = 0;
        anim.play(PoseLibrary.hold(), 15, -1, "hold");
        manager.getPlugin().msg(p, "player.grabbed");
        if (dialogue != null) {
            dialogue.say(this, "possession", p);
        }
        hearts();
        // 挣扎进度条
        if (manager.getPlugin().getConfig().getBoolean("grab.enabled", true)) {
            this.grabBar = org.bukkit.Bukkit.createBossBar(
                    manager.getPlugin().msg("&c挣扎中...按住Shift挣脱！"),
                    org.bukkit.boss.BarColor.RED, org.bukkit.boss.BarStyle.SOLID);
            this.grabBar.addPlayer(p);
            this.grabBar.setProgress(0);
        }
    }

    public void releaseGrabbed() {
        if (grabBar != null) {
            grabBar.removeAll();
            grabBar = null;
        }
        if (grabbedPlayerId != null) {
            Player p = manager.getPlugin().getServer().getPlayer(grabbedPlayerId);
            if (p != null && p.isOnline()) {
                manager.getPlugin().msg(p, "player.released");
                // 放到手掌位置正下方地面
                Vec3 hand = boneWorld(BoneId.HAND_R, anim.currentPose());
                p.teleport(new Location(world, hand.x, hand.y + 0.5, hand.z, p.getLocation().getYaw(), p.getLocation().getPitch()));
            }
        }
        grabbedPlayerId = null;
        grabProgress = 0;
        anim.clearHold();
    }

    private void syncGrabbedPlayer() {
        if (grabbedPlayerId == null) return;
        Player p = manager.getPlugin().getServer().getPlayer(grabbedPlayerId);
        if (p == null || !p.isOnline() || p.isDead()) {
            releaseGrabbed();
            return;
        }
        // 跟随右手手掌
        Vec3 hand = boneWorld(BoneId.HAND_R, anim.currentPose());
        // 潜行=持续挣扎：进度累积，满100挣脱
        boolean struggleEnabled = manager.getPlugin().getConfig().getBoolean("grab.enabled", true);
        if (p.isSneaking() && struggleEnabled) {
            grabProgress += manager.getPlugin().getConfig().getDouble("grab.struggle_per_tick", 1.5);
            if (grabBar != null) {
                grabBar.setProgress(Math.min(1.0, grabProgress / 100.0));
            }
            if (grabProgress >= 100) {
                releaseGrabbed();
                return;
            }
            // 挣扎时挤压伤害
            squeezeTick++;
            int interval = manager.getPlugin().getConfig().getInt("grab.squeeze_interval_ticks", 40);
            if (squeezeTick >= interval) {
                squeezeTick = 0;
                double dmg = manager.getPlugin().getConfig().getDouble("grab.squeeze_damage", 1.0);
                p.damage(dmg);
            }
        }
        p.teleport(new Location(world, hand.x, hand.y + 0.4, hand.z, p.getLocation().getYaw(), p.getLocation().getPitch()));

        // v1.4 心动累积 + 自动亲密演出
        if (++affectionTick >= 100) {
            affectionTick = 0;
            addAffection(1, p);
        }
        if (tickCounter >= nextIntimateTick) {
            nextIntimateTick = tickCounter + 160 + (int) (Math.random() * 120); // 8~14秒切换
            nextIntimateAction(p);
        }
    }

    // ---------------- 移动 ----------------

    /** 水平朝目标移动 */
    public void moveToward(Location target, double speed) {
        Vec3 t = LocUtil.of(target);
        double dx = t.x - anchor.x;
        double dz = t.z - anchor.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 0.5) return;
        double step = Math.min(speed, dist);
        double nx = anchor.x + dx / dist * step;
        double nz = anchor.z + dz / dist * step;
        this.yaw = (float) Math.toDegrees(Math.atan2(-(t.x - anchor.x), (t.z - anchor.z)));
        moveAnchor(nx, anchor.y, nz);
    }

    /** 移动锚点（水平），同步全部Display与Interaction */
    public void moveAnchor(double x, double y, double z) {
        this.anchor = new Vec3(x, y, z);
        Location loc = new Location(world, x, y, z, yaw, 0);
        for (BoneDisplay bd : displays.values()) {
            bd.getDisplay().teleport(loc);
        }
        PoseLibrary.Pose pose = anim.currentPose();
        for (CollisionBox cb : boxes.values()) {
            cb.update(boneWorld(cb.getBone(), pose), scale);
        }
    }

    // ---------------- 状态 / 属性 ----------------

    public void setState(GiantState newState) {
        if (this.state == newState) return;
        this.state = newState;
        if (newState == GiantState.IDLE) {
            this.boredom = 0;
            this.chaseTarget = null;
        }
        manager.onStateChange(this, this.state);
    }

    public void startChase(Player p) {
        this.chaseTarget = p;
        setState(GiantState.CHASE);
        anim.play(PoseLibrary.punch(), 10, -1, "chase");
        if (dialogue != null) {
            dialogue.say(this, "threat", p);
        }
    }

    public void endChase() {
        this.chaseTarget = null;
        this.cooldownAfterChase = manager.getCfg().cooldownAfterChase();
        setState(GiantState.IDLE);
        anim.play(PoseLibrary.idle(), 20, -1, "idle");
    }

    public boolean isHuge() {
        return scale >= manager.getCfg().hugeMinScale();
    }

    public void forceAction(String actionName) {
        PoseLibrary.Pose pose = manager.resolvePose(actionName);
        if (pose != null) {
            anim.play(pose, 15, 120, actionName);
        }
    }

    public void setScale(double newScale) {
        this.scale = Math.max(2.5, newScale);
        applyPose(anim.tick());
    }

    public void addBoredom(int amount) {
        this.boredom += amount;
    }

    public void resetBoredom() {
        this.boredom = 0;
    }

    // ---------------- 持久化 ----------------

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id.toString());
        m.put("type", typeName);
        m.put("scale", scale);
        m.put("state", state.name());
        m.put("mood", mood.name());
        m.put("affection", affection);
        m.put("world", world == null ? "" : world.getName());
        m.put("x", anchor == null ? 0 : anchor.x);
        m.put("y", anchor == null ? 0 : anchor.y);
        m.put("z", anchor == null ? 0 : anchor.z);
        m.put("yaw", yaw);
        return m;
    }

    public static GiantEntity fromMap(GiantManager manager, Map<?, ?> m) {
        UUID uid = UUID.fromString(String.valueOf(m.get("id")));
        GiantEntity g = new GiantEntity(manager, uid,
                String.valueOf(m.get("type")),
                ((Number) m.get("scale")).doubleValue());
        g.state = GiantState.fromString(String.valueOf(m.get("state")));
        g.mood = Mood.fromString(String.valueOf(m.get("mood")));
        if (m.containsKey("affection")) {
            g.affection = Math.min(100, ((Number) m.get("affection")).doubleValue());
            g.affectionLevel = 1 + (int) (g.affection / 25);
        }
        String worldName = String.valueOf(m.get("world"));
        World w = manager.getPlugin().getServer().getWorld(worldName);
        if (w == null) return null;
        double x = ((Number) m.get("x")).doubleValue();
        double y = ((Number) m.get("y")).doubleValue();
        double z = ((Number) m.get("z")).doubleValue();
        Object yawObj = m.containsKey("yaw") ? m.get("yaw") : 0f;
        g.yaw = ((Number) yawObj).floatValue();
        g.spawn(new Location(w, x, y, z, g.yaw, 0));
        return g;
    }

    // ---------------- getters ----------------

    public UUID getId() { return id; }
    public String getTypeName() { return typeName; }
    public double getScale() { return scale; }
    public GiantState getState() { return state; }
    public void setMood(Mood mood) { this.mood = mood; }
    public Mood getMood() { return mood; }
    public int getBoredom() { return boredom; }
    public boolean isSpawned() { return spawned; }
    public Player getChaseTarget() { return chaseTarget; }
    public World getWorld() { return world; }
    public Vec3 getAnchor() { return anchor; }
    public String getDisplayName() { return displayName; }
    public GiantBrain getBrain() { return brain; }
    public AnimationController getAnim() { return anim; }
    public int getCooldownAfterChase() { return cooldownAfterChase; }
    public UUID getGrabbedPlayerId() { return grabbedPlayerId; }
    public long getLastDamageTick() { return lastDamageTick; }
    public void setLastDamageTick(long t) { this.lastDamageTick = t; }

    /** 被攻击时说话（威胁/委屈按心情） */
    public void sayHurt(Player attacker) {
        if (dialogue == null) return;
        dialogue.say(this, mood == Mood.CALM ? "comfort" : "threat", attacker);
    }

    /** 判断某实体是否属于指定骨骼（Display或Interaction） */
    public boolean matchesBoneEntity(org.bukkit.entity.Entity ent, BoneId bone) {
        BoneDisplay bd = displays.get(bone);
        if (bd != null && bd.getDisplay() == ent) return true;
        CollisionBox cb = boxes.get(bone);
        return cb != null && cb.getEntity() == ent;
    }

    /** 根据实体反查骨骼 */
    public BoneId findBoneByEntity(org.bukkit.entity.Entity ent) {
        for (BoneId bone : BoneId.values()) {
            if (matchesBoneEntity(ent, bone)) return bone;
        }
        return null;
    }

    /** 第一个Display实体（作为伤害来源/交互源） */
    public org.bukkit.entity.Entity getFirstDisplayEntity() {
        for (BoneDisplay bd : displays.values()) {
            if (bd.getDisplay() != null && bd.getDisplay().isValid()) return bd.getDisplay();
        }
        return null;
    }

    // ---------------- Boss 血量 / 阶段 / 死亡 ----------------

    /** 玩家攻击巨人：按部位弱点倍率计算伤害，返回实际扣除血量 */
    public double damageByPlayer(Player attacker, BoneId hitBone, double rawDamage) {
        if (dead || maxHp <= 0) return 0;
        org.bukkit.configuration.file.FileConfiguration cfg = manager.getPlugin().getConfig();
        double mult = 1.0;
        if (hitBone == BoneId.HEAD) {
            mult = cfg.getDouble("boss.head_damage_multiplier", 3.0);
        } else if (hitBone == BoneId.TORSO) {
            mult = cfg.getDouble("boss.torso_damage_multiplier", 1.5);
        } else {
            mult = cfg.getDouble("boss.limb_damage_multiplier", 0.5);
        }
        double dmg = rawDamage * mult;
        hp -= dmg;

        // 阶段切换
        int newPhase = phase;
        double pct = hp / maxHp;
        if (pct <= cfg.getDouble("boss.phases.phase3_below", 0.33)) newPhase = 3;
        else if (pct <= cfg.getDouble("boss.phases.phase2_below", 0.66)) newPhase = 2;
        if (newPhase > phase) {
            phase = newPhase;
            onPhaseUp(attacker);
        }

        // 死亡
        if (hp <= 0) {
            die(attacker);
        }
        return dmg;
    }

    private void onPhaseUp(Player attacker) {
        world.playSound(new Location(world, anchor.x, anchor.y, anchor.z),
                org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.5f, 0.6f);
        world.spawnParticle(org.bukkit.Particle.EXPLOSION_HUGE,
                new Location(world, anchor.x, anchor.y + scale * 0.8, anchor.z), 4);
        if (attacker != null) {
            attacker.sendMessage(manager.getPlugin().msg(
                    "&c巨人进入第 " + phase + " 阶段！它狂暴了！"));
        }
        // 狂暴表现：切换到攻击姿势 + 音效
        getAnim().play(PoseLibrary.punch(), 10, 40, "phase_up");
    }

    public void die(Player killer) {
        if (dead) return;
        dead = true;
        // 死亡特效
        world.spawnParticle(org.bukkit.Particle.EXPLOSION_HUGE,
                new Location(world, anchor.x, anchor.y + scale * 0.5, anchor.z), 3);
        world.spawnParticle(org.bukkit.Particle.SMOKE_LARGE,
                new Location(world, anchor.x, anchor.y + scale * 0.5, anchor.z), 40,
                scale * 0.4, scale * 0.4, scale * 0.4, 0.1);
        world.playSound(new Location(world, anchor.x, anchor.y, anchor.z),
                org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 2f, 0.4f);
        // 掉落
        if (killer != null) {
            int xp = manager.getPlugin().getConfig().getInt("boss.death_experience", 100);
            killer.giveExp(xp);
            world.dropItemNaturally(new Location(world, anchor.x, anchor.y + 1, anchor.z),
                    new ItemStack(Material.NETHERITE_INGOT, 1 + (int) (Math.random() * 3)));
            world.dropItemNaturally(new Location(world, anchor.x, anchor.y + 1, anchor.z),
                    new ItemStack(Material.DIAMOND, 3 + (int) (Math.random() * 5)));
            killer.sendMessage(manager.getPlugin().msg("&a你击败了巨人！"));
        }
        // 移除并持久化删除
        manager.removeGiant(id);
    }

    public double getHp() { return hp; }
    public double getMaxHp() { return maxHp; }
    public int getPhase() { return phase; }
    public boolean isBoss() { return maxHp > 0; }
    public boolean isDead() { return dead; }

    /** 当前阶段攻击倍率 */
    public double getAttackMultiplier() {
        org.bukkit.configuration.file.FileConfiguration cfg = manager.getPlugin().getConfig();
        if (phase >= 3) return cfg.getDouble("boss.phase3_attack_multiplier", 1.6);
        if (phase == 2) return cfg.getDouble("boss.phase2_attack_multiplier", 1.25);
        return 1.0;
    }

    /** 当前阶段速度倍率 */
    public double getSpeedMultiplier() {
        org.bukkit.configuration.file.FileConfiguration cfg = manager.getPlugin().getConfig();
        if (phase >= 3) return cfg.getDouble("boss.phase3_speed_multiplier", 1.3);
        if (phase == 2) return cfg.getDouble("boss.phase2_speed_multiplier", 1.15);
        return 1.0;
    }

    /** 判断实体是否属于本巨人（Display或Interaction） */
    public boolean matchesEntity(org.bukkit.entity.Entity entity) {
        for (BoneDisplay bd : displays.values()) {
            if (bd.getDisplay() != null && bd.getDisplay().equals(entity)) return true;
        }
        for (CollisionBox cb : boxes.values()) {
            if (cb.getEntity() != null && cb.getEntity().equals(entity)) return true;
        }
        return false;
    }
}
