#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Giantess Toki geo.json → GTS GiantAI 物品模型资源包 转换器
映射: 模组骨骼组 → 插件10段; 像素UV → 物品模型归一UV(0-16); 段原点=pivot(插件旋转中心)
用法: python3 tools/gts_geo_convert.py
输出: GTSGiantAI-models-toki/ + GTSGiantAI-models-toki-v1.zip
"""
import json, os, shutil, zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "GTSGiantAI-models-toki")
ZIP = os.path.join(ROOT, "GTSGiantAI-models-toki-v1.zip")
GEO = os.path.join(ROOT, "tools", "toki_gecko.geo.json")
TEX_SRC = os.path.join(ROOT, "tools", "asuma_toki_1.png")

# 骨骼组 → 插件段 (pivot 取组内主骨骼 pivot)
SEGMENTS = {
    "head":  ("AllHead", ["AllHead", "head", "TopHair", "LongHair", "liuhai1", "liuhai2", "liuhai3",
                          "bone87", "bone88", "bone89", "bone90", "bone91",
                          "tuer", "bone52", "bone68", "bone56", "bone53", "bone47", "bone49",
                          "Eyes", "LeftEyes", "LeftEyeBall", "RightEyes", "RightEyeBall",
                          "LeftEyelash", "RightEyelash",
                          "NormalEyebrowLeft", "NormalEyebrowRight",
                          "mouth", "SlightOpen", "Tongue", "Tongue_Middle", "Tongue_Front",
                          "bone55", "_", "V", "hong", "hong2"]),
    "torso": ("AllBody", ["AllBody", "UpBody", "UpperBody", "DownBody", "zuoxiong", "youxiong"]),
    "arm_l": ("LeftArm", ["LeftArm", "LeftForeArm"]),
    "arm_r": ("RightArm", ["RightArm", "RightForeArm"]),
    "hand_l": ("LeftHand", ["LeftHand"]),
    "hand_r": ("RightHand", ["RightHand", "RightHand2"]),
    "leg_l": ("LeftLeg", ["LeftLeg", "LeftLowerLeg"]),
    "leg_r": ("RightLeg", ["RightLeg", "RightLowerLeg"]),
    "foot_l": ("LeftFoot", ["LeftFoot"]),
    "foot_r": ("RightFoot", ["RightFoot"]),
}
CMD = {"head": 1000, "torso": 1001, "arm_l": 1002, "arm_r": 1003,
       "hand_l": 1004, "hand_r": 1005, "leg_l": 1006, "leg_r": 1007,
       "foot_l": 1008, "foot_r": 1009}

def face_name(f):
    # geo 面键可能是 "north"/"east"/"south"/"west"/"up"/"down"
    return f

def convert_cube(cube, pivot, tw, th):
    """cube → 物品模型 element (from/to=origin-pivot; 六面uv归一0-16)"""
    origin = cube["origin"]
    size = cube["size"]
    p = pivot
    frm = [origin[0] - p[0], origin[1] - p[1], origin[2] - p[2]]
    to = [frm[0] + size[0], frm[1] + size[1], frm[2] + size[2]]
    # 钳制极薄面
    for i in range(3):
        if abs(to[i] - frm[i]) < 1e-4:
            to[i] = frm[i] + 0.0625  # 0 厚度面给 1/16 像素，避免被剔除
    faces = {}
    uv = cube.get("uv", {})
    if not isinstance(uv, dict):
        return None
    for f, info in uv.items():
        f = f.lower()
        if f not in ("north", "south", "east", "west", "up", "down"):
            continue
        u, v = info["uv"]
        w, h = info["uv_size"]
        # 物品模型 uv: [x1,y1,x2,y2] 0-16 归一; 负尺寸=翻转
        if h < 0:
            y1, y2 = v + h, v
        else:
            y1, y2 = v, v + h
        if w < 0:
            x1, x2 = u + w, u
        else:
            x1, x2 = u, u + w
        x1n = max(0.0, x1 * 16.0 / tw)
        y1n = max(0.0, y1 * 16.0 / th)
        x2n = min(16.0, x2 * 16.0 / tw)
        y2n = min(16.0, y2 * 16.0 / th)
        faces[f] = {"texture": "#layer0", "uv": [round(x1n, 4), round(y1n, 4), round(x2n, 4), round(y2n, 4)]}
    if not faces:
        return None
    return {"from": [round(x, 4) for x in frm], "to": [round(x, 4) for x in to], "faces": faces}

def main():
    g = json.load(open(GEO, encoding="utf-8"))
    geom = g["minecraft:geometry"][0]
    tw = geom["description"].get("texture_width", 128)
    th = geom["description"].get("texture_height", 128)
    bones = {b["name"]: b for b in geom["bones"]}

    models_dir = os.path.join(OUT, "assets/gtsgiantai/models/item")
    tex_dir = os.path.join(OUT, "assets/gtsgiantai/textures/item")
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)

    for seg, (pivot_bone, members) in SEGMENTS.items():
        pb = bones.get(pivot_bone)
        if not pb:
            print("!! 缺 pivot bone:", seg, pivot_bone)
            continue
        pivot = pb.get("pivot", [0, 0, 0])
        elements = []
        for m in members:
            b = bones.get(m)
            if not b or not b.get("cubes"):
                continue
            for c in b["cubes"]:
                el = convert_cube(c, pivot, tw, th)
                if el:
                    elements.append(el)
        if not elements:
            print("!! 段为空:", seg)
            continue
        model = {
            "textures": {"particle": "gtsgiantai:item/toki", "layer0": "gtsgiantai:item/toki"},
            "elements": elements,
        }
        with open(os.path.join(models_dir, f"{seg}_model.json"), "w", encoding="utf-8") as f:
            json.dump(model, f)
        shell = {
            "parent": "item/generated",
            "textures": {"layer0": "gtsgiantai:item/toki"},
            "overrides": [{"predicate": {"custom_model_data": CMD[seg]}, "model": f"gtsgiantai:item/{seg}_model"}],
        }
        with open(os.path.join(models_dir, f"{seg}.json"), "w", encoding="utf-8") as f:
            json.dump(shell, f)
        print(f"seg {seg}: {len(elements)} elements, pivot={pivot}")

    # 贴图（原模组贴图，命名 toki.png）
    shutil.copy(TEX_SRC, os.path.join(tex_dir, "toki.png"))
    with open(os.path.join(OUT, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"pack": {"pack_format": 15, "description": "§dGTS GiantAI - Toki 模型 v1（Giantess Toki 作者授权使用）"}}, f)

    if os.path.exists(ZIP):
        os.remove(ZIP)
    with zipfile.ZipFile(ZIP, "w", zipfile.ZIP_DEFLATED) as z:
        for root, _, files in os.walk(OUT):
            for fn in files:
                fp = os.path.join(root, fn)
                z.write(fp, os.path.relpath(fp, OUT))
    print("OK ->", ZIP)

if __name__ == "__main__":
    main()
