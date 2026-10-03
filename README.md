# RainsGTS（原 GTS GiantAI）

**重度GTS向 · 人型女巨人 · 行为树AI · Paper插件（1.20.1+）· v1.4.1**

为 GTS（Giantess/巨大娘）爱好者打造的 Paper 服务端专属插件：资源包驱动的分段骨骼巨型模型 + 自定义分段碰撞箱 + 行为树 AI，玩家只需加载服务器资源包，**客户端零 mod**。

> v1.4.1：品牌更名为 **RainsGTS**（仓库 https://github.com/leotang0760/RainsGTS ），jar 更名 `RainsGTS-javaXX.jar`；启动时自动迁移旧 `plugins/GTSGiantAI/` 数据目录，服主无需手动搬文件。

---

## ✨ 核心特性

| 模块 | 能力 |
|---|---|
| **行为树 AI** | 追逐 > 警戒 > 空闲 > 休眠 四分支优先级选择器，可扩展节点 |
| **体型缩放** | x2.5 ~ x100 连续倍率，按体型差异化行为/碰撞/灵敏度 |
| **休眠机制** | 附近无玩家概率休眠、夜间加成、**追逐期间绝对禁止休眠** |
| **唤醒规则** | <x10 警戒半径唤醒；≥x10 **仅触碰碰撞盒唤醒**；**潜行蹲下永不唤醒** |
| **分段碰撞箱** | 头/躯干/双臂/双手/双腿/双脚 独立 Interaction 碰撞盒，跟随骨骼动画 |
| **防穿模** | 碰撞盒内缩 + 玩家体内推离 + 动画平滑插值（smoothstep lerp） |
| **GTS 交互** | 捧起（Hold）带**挣扎进度条**、观察、戏弄、挡路、跨过、碾压警告 |
| **多阶段Boss** | 血量随体型公式、**头部3×/躯干1.5×/四肢0.5×弱点伤害**、三阶段狂暴（攻击/速度加成） |
| **战斗** | 踩踏 AOE、挥拳、踢飞，伤害随体型缩放 × 阶段倍率 |
| **村庄互动** | 坐房子（大体型碾压建筑+延迟还原）、玩弄村民、村庄集群生成（3-5只/大体型单只） |
| **世界痕迹** | 行走脚印粒子、坐/跺压痕、植被破坏延迟恢复、**远古脚印彩蛋** |
| **环境探测** | 山体倚靠 / 海边躺卧 / 树林坐 / 平地随机姿态 自动判定 |
| **脚部IK** | 脚部采样地形高度自动贴合，限制最大步差防悬崖穿模 |
| **音效反馈** | 脚步音效（随体型音量）、落地/跺脚震动粒子 |
| **持久化** | 巨人长期保留，重启恢复（giants_data.yml） |
| **心情系统** | CALM / PLAYFUL / ANNOYED / AGGRESSIVE |
| **无聊系统** | 空闲无聊值累积，随机触发跺脚/踢飞/伸展/坐下 |
| **LuckPerms** | 原生接口（反射接入，可选），无 LP 自动降级 Bukkit 权限 |
| **对话系统** | 巨人娘按心情/状态自动说话：10类50+句预设（宠溺/调戏/威胁/哄睡/夸赞…），dialogue.yml 可编辑，/gts reload 热重载 |
| **亲密动作** | 捧起/依偎/低语/轻抚/举高高/轻吻/蹭脸/颠一颠/单手抱/枕膝 10 大亲密姿势 |
| **心动值** | Lv1~Lv5 心动等级（喂食/亲密累积），升级解锁亲昵对话 + ♡ 状态显示，持久化保存 |
| **自动亲密演出** | 被捧起的玩家每 8~14 秒自动切换亲密姿势 + 随机说话；右键手动换姿势 |
| **喂食互动** | 拿食物右键巨人投喂 → 心情提升 + 夸赞回应 + 心形粒子 |
| **状态悬浮条** | 接近巨人时 ActionBar 实时显示 名字·心情·状态·倍率 |
| **Folia 兼容** | 使用 GlobalRegionScheduler，Paper/Folia 通吃 |
| **零外部依赖** | 不 shade 任何第三方库，不与其他插件冲突 |

---

## 🚀 安装（单 jar · 玩家零下载）

**只需一个文件**：`RainsGTS-java17.jar` 同时是插件和模型资源包（内置 Toki 模型与 pack.mcmeta）。

1. 把 jar 放入服务器 `plugins/` 目录，重启服务器（旧版 GTSGiantAI 数据会自动迁移到 `plugins/RainsGTS/`）
2. **玩家什么都不用做**：插件内置 HTTP 分发服务器（默认端口 25564）自动把 jar 本身作为资源包推送给进服玩家（`resourcepack.auto: true`，进服 60tick 后自动弹出加载提示，点接受即显示模型）
3. 也可手动补推：`/gts resourcepack send [玩家]`；控制台 `gts spawn toki 10` 生成测试巨人

> 服务端要求：**Paper 1.20.1+**（含 Folia）。Spigot/CraftBukkit 不支持。
> Java 要求：17 / 21 / 25 按 jar 版本对应（java25 版需 Paper 1.21.5+ 新核心）。
> 只放这一个 jar（勿与旧版 GTSGiantAI.jar 同目录，会触发插件名歧义）。
> 公网IP自动探测失败时，在 `config.yml → resourcepack.server.publicHost` 手动填写服务器公网IP/域名；如需在玩家端强制加载，设 `resourcepack.force: true`。

## ⌨️ 指令（OP / gtsgiantai.admin）

```
/gts spawn <type> <scale> [x y z]   生成巨人（可指定坐标/世界）
/gts remove <id>                    删除巨人（支持8位短ID）
/gts list                           列出全部巨人
/gts state <id> <SLEEP|IDLE|ALERT|CHASE>  强制切换状态
/gts scale <id> <scale>             修改倍率（自动重算碰撞盒）
/gts mood <id> <CALM|PLAYFUL|ANNOYED|AGGRESSIVE>  设置心情
/gts hp <id>                        查看Boss血量与阶段
/gts forceaction <id> <action>      强制播放动作（idle/sit/sleep/lie/lean/stomp/punch/
                                    hold/cuddle/whisper/pat/lift/observe/block/stretch）
/gts reload                         热重载配置（含 dialogue.yml）
/gts resourcepack send [player]     推送资源包（内置分发或直链）
/gts trace                          碰撞盒可视化开关（调试）
```

> v1.3/v1.4 亲密动作：**cuddle** 依偎 / **whisper** 低语 / **pat** 轻抚 / **lift** 举高高 / **kiss** 轻吻额头 / **nuzzle** 蹭脸 / **bounce** 颠一颠 / **carry** 单手抱 / **lap** 枕膝。捧起的玩家会自动跟随手掌位置演出（HAND_R），每 8~14 秒自动切换姿势，右键手动换。

## 🔐 权限

| 权限节点 | 说明 |
|---|---|
| `gtsgiantai.admin` | 全部管理指令（默认 OP） |
| `gtsgiantai.no-interact` | 玩家不会被巨人捧起/戏弄 |
| `gtsgiantai.bypass.builddamage` | 玩家建筑不受巨人破坏 |

## ⚙️ 配置（config.yml）

关键区块一览：

```yaml
global:
  max_giants_per_world: 4      # 每个世界上限（持久巨人务必压低）
  persist_giants: true         # 重启恢复
  natural_spawn: true          # 自然生成

sleep_rules:
  chase_cannot_sleep: true     # 追逐禁止休眠
  no_player_range: 32          # 无玩家才可休眠
  base_chance: 0.15
  night_bonus: 0.35            # 夜间更容易睡着

wake_rules:
  small_alert_range: 24        # <x10 警戒半径
  huge_touch_only: true        # ≥x10 仅触碰唤醒
  sneak_immune: true           # 潜行永不唤醒

collision:
  shrink_offset: 0.2           # 碰撞盒内缩量（防穿模）
  player_push_out: true        # 玩家卡入身体自动推出
  sync_interval_idle: 3        # 空闲碰撞同步频率（tick）
  sync_interval_sleep: 20      # 休眠碰撞同步频率

boredom_actions:               # 无聊行为权重
  stomp: 0.20
  kick_player: 0.15
  look_around: 0.40
  sit_rest: 0.25

environmental_stance:          # 环境待机姿态权重
  sit: 0.30
  lean: 0.25
  lie_down: 0.20
  rest_against_mountain: 0.10
  rest_at_seaside: 0.15

# ---- v1.1.0 新增配置 ----
village:
  enabled: true
  villager_count_threshold: 3   # 16格内3个村民视为村庄
  scan_radius: 16
  small_group_min: 3            # 村庄集群：中小型3-5只
  small_group_max: 5
  huge_forced_single: true      # 大体型村庄内强制单只（直接碾压）
  group_spawn_chance: 0.15      # 村庄集群生成概率
  behaviors:                    # 村庄行为权重
    wander_through: 0.30
    watch_villager: 0.20
    tease_villager: 0.15
    sit_on_house: 0.10
    lean_on_building: 0.10
    crush_structure: 0.10
    destroy_building: 0.05
  huge_can_destroy_buildings: true
  player_buildings_targetable: false   # 玩家建筑是否可被坐/碾
  sit_house_break_radius: 3
  building_restore_seconds: 600        # 破坏方块自动还原

traces:
  enabled: true
  footprint:
    enabled: true
    particle_density: 12
    ancient_random_chance: 0.001       # 远古脚印彩蛋概率
    lifetime: 300
  crush_mark: { enabled: true, restore_seconds: 300 }
  vegetation: { enabled: true, restore_seconds: 600 }
  ancient_check_interval: 6000         # 远古脚印检查间隔(tick)

boss:
  enabled: true
  hp_base: 100.0                       # 血量 = base + scale * per_scale
  hp_per_scale: 20.0                   # x20 → 500HP
  head_damage_multiplier: 3.0          # 头部弱点3倍
  torso_damage_multiplier: 1.5
  limb_damage_multiplier: 0.5
  phases: { phase2_below: 0.66, phase3_below: 0.33 }
  phase2_attack_multiplier: 1.25
  phase3_attack_multiplier: 1.6
  phase2_speed_multiplier: 1.15
  phase3_speed_multiplier: 1.3
  death_experience: 100

grab:
  enabled: true
  struggle_per_tick: 1.5               # 按住Shift挣扎进度速度
  squeeze_damage: 1.0                  # 挣扎时挤压伤害
  squeeze_interval_ticks: 40

sound:
  enabled: true
  step_interval_ticks: 12
  impact_shake_strength: 0.8
  impact_radius: 12

ik:
  enabled: true
  sample_radius: 2
  max_step_height: 2.0                 # 超过不贴合（防悬崖穿模）

render:
  base_material: PLAYER_HEAD   # 骨骼Display承载物品
  custom_model_data: 1000      # 资源包模型CMD
```

## 🎨 资源包接入（高品质模型）

插件渲染架构：**每个骨骼 = 一个 `ItemDisplay` 实体**，承载指定 `CustomModelData` 的模型，用 `Transformation` 摆姿势。

### v1.2.0：内置 Toki 真模型资源包（已交付）

已交付 **`GTSGiantAI-models-toki-v1.zip`**：基于 Giantess Toki 模组（NeoForge 1.21.1，GPL-3.0 代码）授权的真实角色模型转换而来——79 骨骼 GeckoLib 模型 → 插件 10 段物品模型（head 365 元素 / torso 79 / 四肢脚部 80+），128×128 原版贴图，CMD 1000-1009。

安装（二选一）：
1. **自动推送**：把 zip 托管到任意 HTTPS 直链，填 `config.yml → resourcepack.url`，玩家进服用 `/gts resourcepack send` 或自动提示加载；
2. **手动安装**：玩家把 zip 拖入 单机资源包/服务器资源包文件夹（`.minecraft/resourcepacks/`），开服后手动启用。

### 骨架比例（v1.2.0 重构）

- `PoseLibrary` 骨架由紧凑比例重排为**正常人体比例**（scale=1 全高约 3.1 格），与 Toki 模型 pivot 一一对齐（HEAD y=2.08 / TORSO 1.74 / ARM ~2.1 / HAND ~1.3 / LEG ~1.3 / FOOT ~0.2，px/16=格）；
- `BoneId` 碰撞盒同步收紧贴合模型（防"空气墙"），IK 脚底采样改用 BoneId 半高，不再硬编码；
- 模型几何以段 pivot 为原点（ItemDisplay 旋转中心=关节），负坐标元素在实体渲染下正常显示。

制作/替换自定义模型（Blockbench 或任意 3D 工具）：
1. **Blockbench** 制作低面数人型模型（头/躯干/手臂/腿/脚 10 段，≤1200面）
2. 导出为资源包物品模型，绑定 `custom_model_data: 1000+`
3. 每段骨骼对应一个 CMD 物品，配置 `render.custom_model_data` 起始值
4. 打包资源包，配置 `resourcepack.url` 直链，玩家进服自动加载
5. 段 pivot 必须=该骨骼在站立姿态下的世界坐标（锚点=脚底），否则关节错位

> 默认无资源包时使用玩家头颅占位显示，便于先跑通逻辑。
> 动画采用 **blend-to-pose 插值**：每个行为设置目标姿势 + 过渡时长，平滑过渡、无跳帧。

## 🧠 行为树结构

```
Root: Selector（优先级从高到低）
├─ 1. ChaseNode      追逐：追击/踩踏/挥拳，Boss阶段速度加成，目标丢失→脱战
├─ 2. AlertNode      警戒：注视/缓慢逼近，入攻击范围→追击
├─ 3. IdleNode       空闲：巡逻 + 无聊行为 + 环境探测姿态 + 村庄互动分支
└─ 4. SleepNode      休眠：条件满足+概率→SLEEP，追逐禁止
```

扩展行为：继承 `com.rainsh.gtsgiantai.ai.bt.Node`，在 `GiantBrain.buildTree()` 挂载即可。

## 📦 项目结构

```
GTSGiantAI/
├─ src/main/java/com/rainsh/gtsgiantai/
│  ├─ GTSGiantAI.java        # 主类（主循环调度 trace/村庄/自然生成）
│  ├─ ai/                    # 行为树引擎 + 分支
│  │  ├─ bt/                 # Node/Selector/Sequence/Parallel/ConditionDecorator
│  │  ├─ behavior/           # Chase/Alert/Idle/Sleep + 战斗判定
│  │  └─ GiantBrain.java     # 主脑（树组装 + 上下文）
│  ├─ entity/                # GiantEntity/BoneId/BonePose/PoseLibrary/
│  │                         # AnimationController/BoneDisplay/CollisionBox
│  ├─ manager/GiantManager   # 生成/删除/持久化/自然生成/事件
│  ├─ env/EnvironmentScanner # 山/海/树林/平地探测 + 脚部地面采样
│  ├─ village/VillageManager # 村庄检测/坐房破坏/玩弄村民/集群生成
│  ├─ trace/TraceManager     # 脚印/压痕/植被破坏/远古脚印/方块恢复队列
│  ├─ command/GTSCommand     # 全部指令 + Tab补全（含 hp）
│  ├─ listener/PlayerListener# 攻击唤醒/部位弱点伤害/亲密交互
│  ├─ permission/            # LuckPerms 反射接口
│  ├─ event/                 # 自定义事件
│  └─ util/                  # Vec3/AABB/插值工具（零依赖）
└─ src/main/resources/       # plugin.yml/config.yml/messages.yml/giants_data.yml
```

## ⚠️ 已知限制（Paper 底层边界）

1. **玩家原生碰撞盒不可改**：巨人碰撞全部走外挂 Interaction + 自定义 AABB，网络高延迟下可有 1~2tick 判定延迟
2. **无实时物理形变**：所有动画为预烘焙姿势 + 插值，无布料/肌肉/软体物理
3. **视角无法完全无穿模**：摄像机仍是原版规则，超大体型在室内可能视角穿墙
4. **流体交互有限**：踩水/踩岩浆无大范围物理飞溅，仅粒子模拟
5. **IK 覆盖有限**：脚部已贴合地形，但手部/坐姿臀部贴合未做，超悬崖边缘仍可能悬空
6. **Interaction 尺寸上限 16 格**：x50+ 超巨型碰撞盒尺寸被钳制，但自定义 AABB 判定不受影响
7. **村庄判定依赖村民分布**：集群生成需世界自然生成过村庄；玩家自建村需 `player_buildings_targetable` 联动

## 🗺️ 路线图（后续迭代）

已完成（v1.1.0）：
- [x] 村庄互动系统（Sit On House / 玩弄村民 / 3-5只集群生成 / 大体型单只碾压）
- [x] 世界痕迹系统（脚印/压痕/植被破坏/远古脚印彩蛋/建筑延迟还原）
- [x] 环境探测（山体/海边/树林/平地自动姿态）
- [x] 多阶段 Boss 机制与部位弱点伤害（头3×/躯干1.5×/四肢0.5×，三阶段狂暴）
- [x] 抓取挣扎进度条（BossBar，按住 Shift 挣扎 + 挤压伤害）
- [x] 脚步音效（随体型音量）+ 落地/跺脚震动粒子
- [x] 脚部 IK 贴合地形（限制最大步差防悬崖穿模）

已完成（v1.2.0）：
- [x] 真实模型接入：Giantess Toki 授权模型转换（10 段物品模型 + 原版贴图 + CMD 1000-1009）
- [x] 骨架比例重构为正常人体比例，与模型 pivot 精确对齐（无关节错位）
- [x] 碰撞盒贴合新比例（消除空气墙），IK 脚半高去硬编码

已完成（v1.3.0）：
- [x] 对话系统：10 类 50+ 句预设（宠溺/调戏/威胁/哄睡/夸赞/霸占/旁白/轻笑），心情状态驱动，ActionBar/Chat 双通道，dialogue.yml 可编辑热重载
- [x] 亲密动作：依偎 cuddle / 低语 whisper / 轻抚 pat / 举高高 lift（捧起玩家自动跟随手掌演出）
- [x] 喂食互动：食物右键投喂 → 心情提升 + 夸赞回应 + 心形粒子
- [x] 状态悬浮条：接近巨人实时显示 名字·心情·状态·倍率

已完成（v1.4.0）：
- [x] 心动值系统：Lv1~Lv5（喂食+10/捧起累积），升级祝贺对话 + ♡ 状态条 + 持久化
- [x] 亲密动作扩展：轻吻 kiss / 蹭脸 nuzzle / 颠一颠 bounce / 单手抱 carry / 枕膝 lap（共 10 大亲密姿势）
- [x] 自动亲密演出：被捧玩家每 8~14 秒自动切换姿势 + 随机对话；右键手动换姿势
- [x] 脚印方块实体：BlockDisplay 压痕可留存（雪地白色/草地暗色），超时自动消失
- [x] 环境音：接近巨人听到低沉心跳/低语（随体型音量）

待迭代：
- [ ] 玩家变大模式（可选：玩家临时巨大化互动）
- [ ] 阶段专属攻击动画（狂暴第三阶段新动作组）
- [ ] 世界生成物联动（巨大骸骨/巨人营地结构）
- [ ] 完整音频包（自定义音效文件：呼吸/心跳/低语）

## 🛠️ 构建

```bash
./build.sh          # java17（默认）
./build.sh all      # java17 + java21 + java25
./build.sh 21       # 仅 java21
```

> pom.xml 提供三个 profile（java17/java21/java25），同一份源码多字节码版本输出。
> Java 17 编译的 jar 在 21/25 JVM 上同样可运行；分版本是为了满足极端兼容需求。

## 📄 License

内部项目 · Rainsh Studio · 仅供授权服务器使用
