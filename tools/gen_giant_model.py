#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GTS GiantAI 素体模型资源包生成器
生成与插件 10 段骨骼严格对齐的 Blockbench 物品模型 + 像素风贴图 + 资源包 zip。
用法: python3 tools/gen_giant_model.py
输出: GTSGiantAI/models/ + GTSGiantAI-models-v1.zip
"""
import json, os, zipfile
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "GTSGiantAI-models")
ZIP = os.path.join(ROOT, "GTSGiantAI-models-v1.zip")

# 段: (key, box尺寸(px W,H,D), CMD)
BONES = [
    ("head",    (60, 100, 60),  1000),
    ("torso",   (120, 240, 70), 1001),
    ("arm_l",   (50, 220, 50),  1002),
    ("arm_r",   (50, 220, 50),  1003),
    ("hand_l",  (56, 56, 32),   1004),
    ("hand_r",  (56, 56, 32),   1005),
    ("leg_l",   (60, 200, 60),  1006),
    ("leg_r",   (60, 200, 60),  1007),
    ("foot_l",  (60, 50, 110),  1008),
    ("foot_r",  (60, 50, 110),  1009),
]

# ---------------- 像素贴图绘制 ----------------
def make_head_tex():
    """64x64 头部贴图：肤色底 + 日系大眼 + 刘海 + 双马尾"""
    im = Image.new("RGBA", (64, 64), (255, 214, 190, 255))
    d = ImageDraw.Draw(im)
    skin = (255, 214, 190, 255)
    hair = (66, 48, 86, 255)      # 紫黑色长发
    hair2 = (94, 72, 122, 255)
    eye_b = (36, 30, 46, 255)
    eye_h = (255, 255, 255, 255)
    blush = (255, 170, 150, 160)
    # 底色
    im.paste((255, 214, 190, 255), (0, 0, 64, 64))
    # 顶部刘海
    for x in range(64):
        d.line([(x, 0), (x, 12)], fill=hair)
    # 刘海下沿锯齿
    for x in range(0, 64, 4):
        d.line([(x, 12), (x, 16)], fill=hair2)
    # 侧发
    d.rectangle([0, 0, 6, 64], fill=hair)
    d.rectangle([58, 0, 64, 64], fill=hair)
    # 眼睛（日系大眼）
    d.ellipse([12, 24, 26, 42], fill=eye_b)
    d.ellipse([38, 24, 52, 42], fill=eye_b)
    d.ellipse([16, 28, 24, 38], fill=eye_h)
    d.ellipse([42, 28, 50, 38], fill=eye_h)
    d.ellipse([18, 30, 21, 33], fill=(60, 90, 200, 255))
    d.ellipse([44, 30, 47, 33], fill=(60, 90, 200, 255))
    # 嘴
    d.arc([28, 44, 36, 52], 0, 180, fill=(190, 90, 90, 255), width=2)
    # 腮红
    d.ellipse([6, 34, 14, 42], fill=blush)
    d.ellipse([50, 34, 58, 42], fill=blush)
    return im

def make_torso_tex():
    """64x64 躯干贴图：白色衬衫 + 深色短裙/领带"""
    im = Image.new("RGBA", (64, 64), (240, 240, 248, 255))
    d = ImageDraw.Draw(im)
    # 领口深色条
    d.rectangle([24, 4, 40, 14], fill=(44, 40, 60, 255))
    # 领带
    d.polygon([(30, 8), (34, 8), (32, 26)], fill=(200, 60, 80, 255))
    # 胸部高光线
    d.line([(8, 26), (20, 30)], fill=(220, 220, 235, 255), width=3)
    d.line([(44, 26), (56, 30)], fill=(220, 220, 235, 255), width=3)
    # 下摆短裙色
    d.rectangle([0, 52, 64, 64], fill=(52, 48, 68, 255))
    d.line([(0, 52), (64, 52)], fill=(90, 84, 110, 255), width=2)
    return im

def make_limb_tex(base, shade, accent=None):
    """64x64 四肢贴图：肤色+衣边"""
    im = Image.new("RGBA", (64, 64), base)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 64, 10], fill=shade)   # 顶部连接处阴影
    if accent:
        d.rectangle([0, 40, 64, 52], fill=accent)
    return im

def make_foot_tex():
    """64x64 脚部贴图：靴子"""
    im = Image.new("RGBA", (64, 64), (70, 62, 90, 255))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 64, 12], fill=(50, 44, 66, 255))
    d.rectangle([0, 30, 64, 34], fill=(110, 100, 130, 255))
    return im

def make_hand_tex():
    return make_limb_tex((255, 214, 190, 255), (235, 190, 165, 255))

TEX = {
    "head":   make_head_tex,
    "torso":  make_torso_tex,
    "arm":    lambda: make_limb_tex((255, 214, 190, 255), (235, 190, 165, 255), (240, 240, 248, 255)),
    "hand":   make_hand_tex,
    "leg":    lambda: make_limb_tex((255, 214, 190, 255), (235, 190, 165, 255), (52, 48, 68, 255)),
    "foot":   make_foot_tex,
}

# ---------------- 物品模型 json ----------------
def face_uv(w, h, d, x0, y0):
    """为 64x64 贴图生成六面 UV（简单平铺）"""
    u = 64
    def f(sx, sy, ex, ey): return [sx, sy, ex, ey]
    return {
        "north": f(x0, y0, x0 + w, y0 + h), "south": f(x0, y0, x0 + w, y0 + h),
        "east":  f(x0, y0, x0 + d, y0 + h), "west":  f(x0, y0, x0 + d, y0 + h),
        "up":    f(x0, y0, x0 + w, y0 + d), "down":  f(x0, y0, x0 + w, y0 + d),
    }

def bone_elements(key, w, h, d):
    """素体：1-2 个 box 构成该段轮廓"""
    els = []
    if key == "head":
        els.append({"from": [0, 0, 0], "to": [w, h, d],
                    "faces": face_uv(w, h, d, 0, 0)})
        # 后发披散 box
        els.append({"from": [-4, 14, -6], "to": [64, 100, 66],
                    "faces": face_uv(68, 86, 72, 0, 0)})
    elif key == "torso":
        els.append({"from": [0, 0, 0], "to": [w, h, d],
                    "faces": face_uv(w, h, d, 0, 0)})
        # 裙摆
        els.append({"from": [-6, h - 60, -10], "to": [126, h, 80],
                    "faces": face_uv(132, 60, 90, 0, 52)})
    else:
        els.append({"from": [0, 0, 0], "to": [w, h, d],
                    "faces": face_uv(w, h, d, 0, 0)})
    return els

def build_model_json(key, w, h, d, cmd):
    model = {
        "textures": {"particle": f"gtsgiantai:item/{key}"},
        "elements": bone_elements(key, w, h, d),
    }
    # 材质层：简单 box 各面指向贴图
    # （faces 里未指定 texture 时用模型 textures 缺省；为稳妥，逐面补 texture）
    for e in model["elements"]:
        for k in e["faces"]:
            e["faces"][k] = {"texture": "#layer0", "uv": e["faces"][k]}
    model["textures"]["layer0"] = f"gtsgiantai:item/{key}"
    return model

def build_shell_json(cmd, model_path):
    return {
        "parent": "item/generated",
        "textures": {"layer0": "gtsgiantai:item/placeholder"},
        "overrides": [
            {"predicate": {"custom_model_data": cmd}, "model": model_path}
        ],
    }

def main():
    models_dir = os.path.join(OUT, "assets/gtsgiantai/models/item")
    tex_dir = os.path.join(OUT, "assets/gtsgiantai/textures/item")
    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(tex_dir, exist_ok=True)

    for key, (w, h, d), cmd in BONES:
        model_key = key
        # 模型本体
        m = build_model_json(model_key, w, h, d, cmd)
        with open(os.path.join(models_dir, f"{model_key}_model.json"), "w", encoding="utf-8") as f:
            json.dump(m, f)
        # CMD 壳（指向模型本体）
        shell = build_shell_json(cmd, f"gtsgiantai:item/{model_key}_model")
        with open(os.path.join(models_dir, f"{model_key}.json"), "w", encoding="utf-8") as f:
            json.dump(shell, f)
        # 贴图
        tex_fn = TEX["arm" if key in ("arm_l", "arm_r") else
                    "leg" if key in ("leg_l", "leg_r") else
                    "foot" if key in ("foot_l", "foot_r") else
                    "hand" if key in ("hand_l", "hand_r") else key]
        tex_fn().save(os.path.join(tex_dir, f"{model_key}.png"))

    # pack.mcmeta
    with open(os.path.join(OUT, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump({"pack": {"pack_format": 15, "description": "§dGTS GiantAI 素体模型 v1（CC0，Rainsh Studio 程序生成）"}}, f)

    # 打包 zip
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
