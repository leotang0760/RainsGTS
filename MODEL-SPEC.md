# GTS GiantAI 模型接入规范 v1.0

**目标**：为插件制作/接入"分段女巨人"物品模型资源包，与插件的 10 段骨骼系统严丝合缝匹配。
**适用**：Blockbench 建模 / Sketchfab CC 模型转换 / 任何物品模型资源包。
**基准**：插件骨骼以"格"为单位（scale=1 时），模型以 Blockbench 像素为单位（1 格 = 16 像素）。

---

## 1. 骨骼系统总览（与插件一一对应）

插件将巨人渲染为 **10 段 ItemDisplay**，每段一个独立物品模型：

```
        HEAD
         │
       TORSO
     ┌───┴───┐
   ARM_L   ARM_R
     │        │
  HAND_L   HAND_R
    LEG_L   LEG_R
     │        │
  FOOT_L   FOOT_R
```

| 骨骼 | 插件key | 碰撞盒半尺寸(格) | 模型全尺寸(格) | 模型全尺寸(px) | CustomModelData |
|---|---|---|---|---|---|
| 头 | head | 0.30×0.50×0.30 | 0.60×1.00×0.60 | 60×100×60 | 1000 |
| 躯干 | torso | 0.60×1.20×0.35 | 1.20×2.40×0.70 | 120×240×70 | 1001 |
| 左臂 | arm_l | 0.25×1.10×0.25 | 0.50×2.20×0.50 | 50×220×50 | 1002 |
| 右臂 | arm_r | 0.25×1.10×0.25 | 0.50×2.20×0.50 | 50×220×50 | 1003 |
| 左手 | hand_l | 0.28×0.28×0.16 | 0.56×0.56×0.32 | 56×56×32 | 1004 |
| 右手 | hand_r | 0.28×0.28×0.16 | 0.56×0.56×0.32 | 56×56×32 | 1005 |
| 左腿 | leg_l | 0.30×1.00×0.30 | 0.60×2.00×0.60 | 60×200×60 | 1006 |
| 右腿 | leg_r | 0.30×1.00×0.30 | 0.60×2.00×0.60 | 60×200×60 | 1007 |
| 左脚 | foot_l | 0.30×0.25×0.55 | 0.60×0.50×1.10 | 60×50×110 | 1008 |
| 右脚 | foot_r | 0.30×0.25×0.55 | 0.60×0.50×1.10 | 60×50×110 | 1009 |

> CMD 起始值可改：`config.yml → render.custom_model_data`（默认 1000）。

## 2. 锚点（Pivot）规则 —— 最重要的一条

每个骨骼模型的 **pivot（旋转中心）必须位于该段的"连接点"**，否则动画会散架：

| 骨骼 | pivot (px, 模型底部为0) | 说明 |
|---|---|---|
| head | (30, 0, 30) | 底部（脖子连接点） |
| torso | (60, 20, 35) | 底部偏上 20px（骨盆/跨连接点） |
| arm_l | (25, 0, 25) | 底部（肩部连接点），**Y 向下延伸** |
| arm_r | (25, 0, 25) | 底部（肩部连接点），**Y 向下延伸** |
| hand_l | (28, 0, 16) | 底部（手腕连接点） |
| hand_r | (28, 0, 16) | 底部（手腕连接点） |
| leg_l | (30, 0, 30) | 底部（髋部连接点），**Y 向下延伸** |
| leg_r | (30, 0, 30) | 底部（髋部连接点），**Y 向下延伸** |
| foot_l | (30, 0, 55) | 底部（脚踝连接点） |
| foot_r | (30, 0, 55) | 底部（脚踝连接点） |

> 简单记法：**所有骨骼 pivot 放模型底部中心**（除 torso 放在胯部旋转点）。插件按"脚底锚点 + 每段局部变换"组装，pivot 决定姿势弯曲的位置。

## 3. 模型制作规范

- **总面数**：全模型 ≤ 2400 面（每段 ≤ 300），否则多人服务器卡顿
- **命名**：Blockbench 内模型名用插件 key（`head` / `torso` / `arm_l`…），导出后便于对照
- **贴图**：单张 256×256（或 512×512）材质，按标准 Minecraft 物品模型 UV 展开；风格建议低饱和日式动漫（与 GTS 题材匹配）
- **朝向**：模型正面朝 -Z（Blockbench 默认），与插件 yaw 逻辑一致
- **不要**在模型内做手脚关节（手指/发丝动画）：插件动画是整段摆姿势，段内细节保持静态
- **可接受**：段内带少量"随动部件"（如发辫并入 head、裙摆并入 torso）

## 4. 资源包结构（最终产物）

```
resourcepack/
├─ pack.mcmeta
└─ assets/
   └─ gtsgiantai/
      ├─ models/item/
      │  ├─ head.json      # 引用 1000
      │  ├─ torso.json     # 1001
      │  ├─ arm_l.json     # 1002
      │  ├─ arm_r.json     # 1003
      │  ├─ hand_l.json    # 1004
      │  ├─ hand_r.json    # 1005
      │  ├─ leg_l.json     # 1006
      │  ├─ leg_r.json     # 1007
      │  ├─ foot_l.json    # 1008
      │  └─ foot_r.json    # 1009
      └─ textures/item/
         ├─ head.png
         ├─ torso.png
         └─ …（每段一张，或共享一张图用 UV 引用）
```

`head.json` 示例（其余同理，改 model/材质/CMD）：

```json
{
  "parent": "item/generated",
  "textures": { "layer0": "gtsgiantai:item/head" },
  "overrides": [
    { "predicate": { "custom_model_data": 1000 }, "model": "gtsgiantai:item/head_model" }
  ]
}
```

> 更省事的做法：所有骨骼共用 `parent: item/generated` + 各自 CMD override 指向独立模型文件。Blockbench 的 **"Export Item Model"** 可直接生成整个覆盖链。

## 5. 从 Sketchfab CC 模型转换流程（推荐路线）

Sketchfab 下载的 GLB 是高模角色，需 **Blender → Blockbench** 两步降级分段：

1. **下载**：Sketchfab 免费账号可下载 CC 模型（GLB/GLTF）
2. **Blender 处理**（免费，约 30 分钟）：
   - 导入 GLB → 选中角色 → 减面（Decimate，目标总面数 ≤ 2400）
   - 按插件 10 段**切分网格**（Edit Mode 分离：头/躯干/左右臂/左右手/左右腿/左右脚）
   - 每段重建 pivot（对齐到段底部中心 / 胯部旋转点）
   - 导出为 10 个独立 `.glb`
3. **Blockbench 装配**（免费）：
   - 新建 "Minecraft Item" 项目 → 逐个导入 10 个 GLB 为模型部件
   - 按第 2 节设置 pivot，按第 1 节尺寸微调
   - 材质烘焙/指定贴图 → **Export Item Model**
4. **打包**：按第 4 节结构打包 zip → 配置 `resourcepack.url` 直链 → 玩家进服自动加载

## 6. 可用模型源（CC 许可，已核实可商用/需署名）

| 模型 | 许可 | 面数/顶点 | 说明 |
|---|---|---|---|
| [Basic female cartoon (CC0)](https://sketchfab.com/3d-models/basic-female-cartoon-cc0-8cc83c972db04437ad93283be3373adc) | **CC0**（免署名） | 15.7k 三角 | 夏季女装卡通角色，最适合直接转换 |
| [Anime Style Waifu Base // 8k Verts](https://sketchfab.com/3d-models/anime-style-waifu-base-8k-verts-free-802619672d564ce4bc0f29150b5afeab) | CC-BY | 8k 顶点 | **已绑定骨骼**（rigged），动漫体型，转换省事 |
| [Cute Anime Girl - Fully Rigged](https://sketchfab.com/3d-models/cute-anime-girl-fully-rigged-3d-model-ef055d864b9d426691f973fb60af0457) | CC-BY | 高模 | 完整贴图+骨骼，Blender 可直接对齐插件分段 |
| [FREE Female Character RIGGED](https://sketchfab.com/3d-models/free-female-character-rigged-8c63855ced6c4c81847487e278bf45fa) | CC-BY | quad 拓扑 | 游戏就绪、四边形拓扑，减面友好 |
| [Anime-style young female (4k tri)](https://sketchfab.com/3d-models/anime-style-young-female-character-00371e77ed004a4ab3462b9b2c5f19ff) | CC-BY | 4k 三角 | AI 生成但面数极低，转换成本最小 |

> **署名要求**：CC-BY 模型需在服务器页面/资源包内注明作者与许可（一行文本即可）。CC0 完全免署名。
> **禁止**：CC-BY-NC 模型不可用于商业服务器；不要从"All Rights Reserved"模型（如 Giantess Toki）提取。

## 7. 从零建模流程（Blockbench，无外部模型）

1. Blockbench → 新建 **Minecraft Item** 项目
2. 按第 1 节尺寸建 10 个 box（可用"Generate"批量生成骨架占位）
3. 逐段捏造型（头部脸型/发型、躯干曲线、腿型）
4. 按第 2 节设置 pivot → 展开 UV → 绘制材质（自带贴图画板）
5. Export Item Model → 按第 4 节打包

## 8. 交付前检查清单

- [ ] 10 个模型文件命名与插件 key 一致（head/torso/arm_l/arm_r/hand_l/hand_r/leg_l/leg_r/foot_l/foot_r）
- [ ] CMD 1000~1009 与 config `render.custom_model_data` 一致
- [ ] 每段 pivot 在第 2 节规定位置（可进游戏用 `/gts forceaction <id> sit` 目测，坐姿弯曲正常无错位）
- [ ] 总面数 ≤ 2400，单人测试 3 只巨人帧率下降 < 15%
- [ ] CC-BY 模型已注明作者署名
- [ ] 资源包 zip 已配置直链，`/gts resourcepack send` 玩家可加载

---

*本规范与插件 v1.1.0 骨骼系统（BoneId.java / PoseLibrary.java）严格对齐。模型完成后发我资源包，我做装配联调。*
