#!/usr/bin/env python3
"""
Scary Monsters asset generator (single source of truth for all three models).

Writes textures (PNG) AND the matching Java geometry so UVs never drift:
  gear   -> dino parts rendered ON TOP of the player (tail, snout, claws, spikes, scales), per form 1-4
  raptor -> the Raptor minion
  rex    -> the T-Rex minion
Run:  python3 tools/generate_assets.py     (needs Pillow)
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
RES = os.path.join(ROOT, "src/main/resources/assets/scarymonsters")
JAVA = os.path.join(ROOT, "src/main/java/com/scarymonsters/client")
random.seed(77)
TEX = 128


def rgb(*a):
    return tuple(a)


def pal(base, light, dark):
    return (base, light, dark)


COMMON = {
    "claw": pal(rgb(244, 238, 218), rgb(255, 252, 240), rgb(120, 110, 90)),
    "dark": pal(rgb(40, 40, 32), rgb(62, 62, 50), rgb(20, 20, 16)),
}
GEAR_PAL = dict(COMMON, **{
    "scale": pal(rgb(118, 146, 52), rgb(150, 176, 76), rgb(78, 100, 34)),
    "stripe": pal(rgb(112, 140, 50), rgb(142, 170, 72), rgb(60, 84, 28)),
    "belly": pal(rgb(226, 208, 140), rgb(244, 230, 170), rgb(180, 160, 100)),
    "crest": pal(rgb(232, 118, 30), rgb(255, 160, 60), rgb(150, 70, 15)),
})
RAPTOR_PAL = dict(COMMON, **{
    "scale": pal(rgb(58, 140, 104), rgb(86, 170, 130), rgb(34, 92, 66)),
    "stripe": pal(rgb(54, 128, 96), rgb(80, 160, 122), rgb(20, 60, 48)),
    "belly": pal(rgb(232, 214, 150), rgb(248, 234, 180), rgb(190, 170, 110)),
    "crest": pal(rgb(240, 130, 40), rgb(255, 170, 70), rgb(160, 80, 20)),
})
REX_PAL = dict(COMMON, **{
    "scale": pal(rgb(128, 86, 52), rgb(160, 116, 76), rgb(84, 54, 30)),
    "stripe": pal(rgb(120, 80, 48), rgb(150, 108, 70), rgb(60, 38, 22)),
    "belly": pal(rgb(214, 184, 130), rgb(232, 206, 158), rgb(170, 140, 96)),
    "crest": pal(rgb(200, 60, 30), rgb(235, 100, 60), rgb(130, 30, 14)),
})


class Spec:
    def __init__(self, key, cls, palette):
        self.key, self.cls, self.pal = key, cls, palette
        self.parts = []

    def P(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0), box=None, mat="scale",
          dil=0.0, lo=1, hi=4, decor=None):
        self.parts.append(dict(name=name, parent=parent, pivot=pivot, rot=rot, box=box, mat=mat,
                               dil=dil, lo=lo, hi=hi, decor=decor))

    def chain(self, name):
        by = {p["name"]: p for p in self.parts}
        out, cur = [], by[name]
        while cur:
            out.append(cur["name"])
            cur = by[cur["parent"]] if cur["parent"] else None
        return "/".join(reversed(out))


# =====================================================================================
# GEAR: the dinosaur body parts drawn over the player.
# Parts live inside "g_*" groups that follow the matching vanilla player part
# (vanilla biped coordinates: Y down, front is -Z). lo/hi = range of forms (1-4) it is shown in.
# =====================================================================================
def build_gear():
    G = Spec("gear", "GearParts", GEAR_PAL)
    for g in ("g_head", "g_body", "g_rarm", "g_larm", "g_rleg", "g_lleg"):
        G.P(g)

    # ---- head: snout grows with the form; forms 3-4 cover the whole face with a dino skull
    G.P("snout_a", "g_head", box=(-2, -4, -8, 4, 3, 4), lo=1, hi=1, decor="teeth_up")
    G.P("jaw_a", "g_head", pivot=(0, -1, -4), box=(-1.5, 0, -4, 3, 1, 4), mat="belly", lo=1, hi=1, decor="teeth_low")
    G.P("snout_b", "g_head", box=(-2, -5, -11, 4, 4, 7), lo=2, hi=2, decor="teeth_up")
    G.P("jaw_b", "g_head", pivot=(0, -1, -4), box=(-1.5, 0, -7, 3, 1, 7), mat="belly", lo=2, hi=2, decor="teeth_low")
    G.P("mask_c", "g_head", box=(-4, -8, -4, 8, 8, 8), dil=0.55, lo=3, hi=4, decor="eyes_front")
    G.P("snout_c", "g_head", box=(-3, -6, -13, 6, 5, 9), lo=3, hi=4, decor="teeth_up")
    G.P("jaw_c", "g_head", pivot=(0, -1, -4), box=(-2.5, 0, -9, 5, 2, 9), mat="belly", lo=3, hi=4, decor="teeth_low")
    G.P("brow_c", "g_head", box=(-4, -8, -5, 8, 2, 1), dil=0.6, mat="dark", lo=3, hi=4)
    G.P("crest_a", "g_head", box=(-0.5, -10, -3, 1, 2, 6), mat="crest", lo=1, hi=2)
    G.P("crest_c", "g_head", box=(-1, -13, -4, 2, 5, 8), mat="crest", lo=3, hi=4)
    G.P("horn_r", "g_head", pivot=(-4, -7, -2), rot=(-0.5, 0, -0.5), box=(-1, -6, -1, 2, 6, 2), mat="claw", lo=3, hi=4)
    G.P("horn_l", "g_head", pivot=(4, -7, -2), rot=(-0.5, 0, 0.5), box=(-1, -6, -1, 2, 6, 2), mat="claw", lo=3, hi=4)

    # ---- body
    G.P("belt", "g_body", box=(-4, 10, -2, 8, 2, 4), dil=0.3, mat="dark")
    G.P("band_b", "g_body", box=(-4, 2, -2, 8, 3, 4), dil=0.35, mat="crest", lo=2, hi=2)
    G.P("shell_c", "g_body", box=(-4, 0, -2, 8, 12, 4), dil=0.4, mat="stripe", lo=3, hi=4, decor="belly_front")

    # tails: thin (forms 1-2) and thick (3-4); each is a 3-segment chain animated by the model
    G.P("tailA_1", "g_body", pivot=(0, 10.5, 2), rot=(0.12, 0, 0), box=(-2, -2, 0, 4, 4, 6), mat="stripe", lo=1, hi=2)
    G.P("tailA_2", "tailA_1", pivot=(0, 0, 6), box=(-1.5, -1.5, 0, 3, 3, 6), mat="stripe", lo=1, hi=2)
    G.P("tailA_3", "tailA_2", pivot=(0, 0, 6), box=(-1, -1, 0, 2, 2, 6), mat="stripe", lo=1, hi=2)
    G.P("tailA_tip", "tailA_3", pivot=(0, 0, 6), box=(-0.5, -0.5, 0, 1, 1, 3), mat="claw", lo=2, hi=2)
    for i, (par, y) in enumerate((("tailA_1", -2.5), ("tailA_2", -2.0), ("tailA_3", -1.5))):
        G.P(f"tspA_{i}", par, box=(-0.5, y - 1.5, 1.5, 1, 2, 2), mat="crest", lo=2, hi=2)
    G.P("tailB_1", "g_body", pivot=(0, 10.5, 2), rot=(0.1, 0, 0), box=(-3, -3, 0, 6, 6, 7), mat="stripe", lo=3, hi=4)
    G.P("tailB_2", "tailB_1", pivot=(0, 0, 7), box=(-2.5, -2.5, 0, 5, 5, 7), mat="stripe", lo=3, hi=4)
    G.P("tailB_3", "tailB_2", pivot=(0, 0, 7), box=(-2, -2, 0, 4, 4, 7), mat="stripe", lo=3, hi=4)
    G.P("tailB_tip", "tailB_3", pivot=(0, 0, 7), box=(-1, -1, 0, 2, 2, 5), mat="claw", lo=3, hi=4)
    for i, (par, y) in enumerate((("tailB_1", -3.0), ("tailB_2", -2.5), ("tailB_3", -2.0))):
        G.P(f"tspB_{i}", par, box=(-1, y - 2.5, 1.5, 2, 3, 3), mat="crest", lo=3, hi=4)

    # dorsal plates
    for i in range(4):
        G.P(f"plate2_{i}", "g_body", pivot=(0, 1 + 3 * i, 2), rot=(0.3, 0, 0), box=(-0.5, 0, 0, 1, 3, 2), mat="crest", lo=2, hi=2)
        G.P(f"plate3_{i}", "g_body", pivot=(0, 1 + 3 * i, 2), rot=(0.3, 0, 0), box=(-1, 0, 0, 2, 3, 3), mat="crest", lo=3, hi=4)

    # ---- arms (right arm box x=-3..1, left x=-1..3) and legs
    for s, grp, bx, sx in (("r", "g_rarm", -3, -1), ("l", "g_larm", -1, 1)):
        G.P(f"ashell_{s}", grp, box=(bx, -2, -2, 4, 12, 4), dil=0.3, mat="scale", lo=2, hi=4)
        G.P(f"elbow_{s}", grp, pivot=(3 * sx, 5, 0), box=(0 if sx > 0 else -3, -0.5, -0.5, 3, 1, 1), mat="claw", lo=2, hi=4)
        for i in range(3):
            x = bx + 0.4 + 1.1 * i
            G.P(f"claw_a_{s}{i}", grp, pivot=(x, 12, -0.5), rot=(-0.35, 0, 0), box=(-0.5, 0, -0.5, 1, 3, 1), mat="claw", lo=1, hi=2)
            G.P(f"claw_b_{s}{i}", grp, pivot=(x, 12, -0.5), rot=(-0.45, 0, 0), box=(-0.5, 0, -0.5, 1, 5, 1), mat="claw", lo=3, hi=4)
    for s, grp in (("r", "g_rleg"), ("l", "g_lleg")):
        G.P(f"lshell_{s}", grp, box=(-2, 0, -2, 4, 12, 4), dil=0.3, mat="scale", lo=2, hi=4)
        for i in range(3):
            G.P(f"toe_{s}{i}", grp, pivot=(-1.2 + 1.2 * i, 11, -2), box=(-0.5, 0, -3, 1, 1, 3), mat="claw", lo=1, hi=4)
        G.P(f"sickle_{s}", grp, pivot=(0, 7, -2.2), rot=(-0.7, 0, 0), box=(-0.5, 0, -0.5, 1, 4, 1), mat="claw", lo=2, hi=4)
        G.P(f"kneespike_{s}", grp, pivot=(0, 5, -2), box=(-1, -1, -3, 2, 2, 3), mat="claw", lo=3, hi=4)
    return G


# =====================================================================================
# Dinosaur minions: one parametric builder, two parameter sets.
# =====================================================================================
def build_dino(key, cls, palette, p):
    S = Spec(key, cls, palette)
    a1, a2, a3 = p["ang"]
    l1, l2, l3 = p["len"]
    w1, w2, w3 = p["wid"]
    bw, bh, bl = p["body"]
    vertical = l1 * math.cos(a1) + l2 * math.cos(a1 + a2) + l3 * math.cos(a1 + a2 + a3)
    hip_y = 24 - vertical - 1
    S.P("body", None, pivot=(0, hip_y, 0), box=(-bw / 2, -bh / 2, -bl / 2, bw, bh, bl), mat="stripe")
    nw, nl, nr = p["neck"]
    S.P("neck", "body", pivot=(0, -bh / 2 + 0.5, -bl / 2 + 1), rot=(nr, 0, 0), box=(-nw / 2, -nl, -nw / 2, nw, nl, nw), mat="scale")
    hw, hh, hl = p["head"]
    S.P("head", "neck", pivot=(0, -nl, 0), rot=(p["head_rot"], 0, 0), box=(-hw / 2, -hh / 2, -hl + 1, hw, hh, hl), mat="scale", decor="eyes_side")
    sw, sh, sl = p["snout"]
    S.P("snout", "head", box=(-sw / 2, hh / 2 - sh, -hl + 1 - sl, sw, sh, sl), mat="scale", decor="teeth_up")
    jl = sl + 3
    S.P("jaw", "head", pivot=(0, hh / 2, -hl + 4), box=(-sw / 2, 0, -jl, sw, 1, jl), mat="belly", decor="teeth_low")
    S.P("crest", "head", box=(-0.5, -hh / 2 - 1.5, -hl / 2, 1, 2, hl), mat="crest")
    (t1w, t1h, t1l), (t2w, t2h, t2l), (t3w, t3h, t3l) = p["tail"]
    S.P("tail1", "body", pivot=(0, 0, bl / 2 - 1), rot=(0.1, 0, 0), box=(-t1w / 2, -t1h / 2, 0, t1w, t1h, t1l), mat="stripe")
    S.P("tail2", "tail1", pivot=(0, 0, t1l), box=(-t2w / 2, -t2h / 2, 0, t2w, t2h, t2l), mat="stripe")
    S.P("tail3", "tail2", pivot=(0, 0, t2l), box=(-t3w / 2, -t3h / 2, 0, t3w, t3h, t3l), mat="stripe")
    S.P("tail_tip", "tail3", pivot=(0, 0, t3l), box=(-0.5, -0.5, 0, 1, 1, 3), mat="claw")
    for s, sx in (("r", -1), ("l", 1)):
        S.P(f"arm_{s}", "body", pivot=(sx * (bw / 2 + 0.3), 1, -bl / 2 + 2), rot=(0.9, 0, 0),
            box=(-0.5, 0, -0.5, 1, p["arm"], 1), mat="scale")
        S.P(f"armclaw_{s}", f"arm_{s}", pivot=(0, p["arm"], 0), rot=(-0.5, 0, 0), box=(-0.5, 0, -0.5, 1, 2, 1), mat="claw")
        S.P(f"thigh_{s}", "body", pivot=(sx * p["hip_x"], 1, p["hip_z"]), rot=(a1, 0, 0),
            box=(-w1 / 2, 0, -w1 / 2, w1, l1, w1), mat="scale")
        S.P(f"shin_{s}", f"thigh_{s}", pivot=(0, l1, 0), rot=(a2, 0, 0), box=(-w2 / 2, 0, -w2 / 2, w2, l2, w2), mat="stripe")
        S.P(f"foot_{s}", f"shin_{s}", pivot=(0, l2, 0), rot=(a3, 0, 0), box=(-w3 / 2, 0, -w3 / 2, w3, l3, w3), mat="scale")
        S.P(f"toes_{s}", f"foot_{s}", box=(-w3 / 2 - 0.5, l3 - 1, -4, w3 + 1, 1, 4), mat="scale")
        S.P(f"toeclaw_{s}", f"foot_{s}", pivot=(0, l3 - 1, -4), rot=(-0.5, 0, 0), box=(-0.5, 0, -1, 1, 2, 1), mat="claw")
        S.P(f"sickle_{s}", f"foot_{s}", pivot=(0, l3 - 1.5, -3), rot=(-0.9, 0, 0), box=(-0.5, 0, -0.5, 1, 3, 1), mat="claw")
    return S


def build_raptor():
    return build_dino("raptor", "RaptorParts", RAPTOR_PAL, dict(
        body=(6, 6, 12), neck=(3, 6, 0.45), head=(4, 4, 4), head_rot=-0.5, snout=(3, 2, 3),
        tail=((4, 4, 6), (3, 3, 6), (2, 2, 6)), arm=4, hip_x=4, hip_z=2,
        len=(6, 6, 5), wid=(3, 2, 2), ang=(0.7, -1.3, 0.6)))


def build_rex():
    return build_dino("rex", "RexParts", REX_PAL, dict(
        body=(8, 8, 16), neck=(5, 5, 0.2), head=(6, 6, 6), head_rot=-0.2, snout=(5, 3, 5),
        tail=((6, 6, 8), (5, 5, 8), (3, 3, 8)), arm=3, hip_x=5, hip_z=3,
        len=(8, 7, 6), wid=(4, 3, 3), ang=(0.5, -1.0, 0.5)))


# =====================================================================================
# UV packing / painting
# =====================================================================================
def footprint(box):
    _, _, _, w, h, d = box
    return int(2 * (d + w)), int(d + h)


def pack(S):
    items = [p for p in S.parts if p["box"]]
    items.sort(key=lambda p: -footprint(p["box"])[1])
    x = y = shelf = 0
    for p in items:
        fw, fh = footprint(p["box"])
        if x + fw > TEX:
            x, y, shelf = 0, y + shelf, 0
        assert y + fh <= TEX, f"{S.key}: texture atlas overflow"
        p["uv"] = (x, y)
        x += fw
        shelf = max(shelf, fh)


def faces(u, v, w, h, d):
    w, h, d = int(w), int(h), int(d)
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


def jit(c, amt=5):
    return tuple(max(0, min(255, v + random.randint(-amt, amt))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def paint_face(img, rect, P, mat, kind):
    x, y, w, h = rect
    base, light, dark = P[mat]
    belly = P["belly"][0] if "belly" in P else base
    for j in range(h):
        for i in range(w):
            c = base
            if mat in ("scale", "stripe"):
                ph = (i + (j // 2) * 2) % 4
                if ph == 0 and j % 2 == 1:
                    c = dark
                elif ph == 2 and j % 2 == 0:
                    c = light
                if mat == "stripe" and kind == "side" and (i % 6) in (0, 1):
                    c = mix(base, P["dark"][0], 0.55)
            if kind == "top":
                c = mix(c, light, 0.45)
            elif kind == "bottom":
                c = mix(c, belly, 0.8) if mat in ("scale", "stripe") else mix(c, dark, 0.4)
            else:
                if mat not in ("scale", "stripe") and (j == 0 or i == 0):
                    c = light
                elif mat not in ("scale", "stripe") and (j == h - 1 or i == w - 1):
                    c = dark
            img.putpixel((x + i, y + j), jit(c) + (255,))


def put(img, x, y, c):
    img.putpixel((x, y), tuple(c) + (255,))


EYE = (255, 226, 60)


def decorate(kind, f, img, P):
    white = P["claw"][0]
    if kind == "eyes_side":
        for key in ("left", "right"):
            sx, sy, sw, sh = f[key]
            ex = sx + (sw - 3 if key == "right" else 1)
            for dx in range(2):
                for dy in range(2):
                    put(img, ex + dx, sy + 1 + dy, EYE)
            put(img, ex + (1 if key == "right" else 0), sy + 2, (20, 20, 10))
    elif kind == "eyes_front":
        fx, fy, fw, fh = f["front"]
        for ex in (fx + 1, fx + fw - 3):
            for dx in range(2):
                put(img, ex + dx, fy + 2, EYE)
                put(img, ex + dx, fy + 3, EYE)
            put(img, ex + (1 if ex == fx + 1 else 0), fy + 3, (20, 20, 10))
    elif kind in ("teeth_up", "teeth_low"):
        for key in ("left", "right", "front"):
            sx, sy, sw, sh = f[key]
            j = sh - 1 if kind == "teeth_up" else 0
            for i in range(0, sw, 2):
                put(img, sx + i, sy + j, white)
    elif kind == "belly_front":
        fx, fy, fw, fh = f["front"]
        belly, light, dark = P["belly"]
        for j in range(fh):
            for i in range(fw):
                put(img, fx + i, fy + j, jit(dark if j % 3 == 2 else belly, 4))


def build_texture(S, name):
    img = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    for p in S.parts:
        if not p["box"]:
            continue
        u, v = p["uv"]
        _, _, _, w, h, d = p["box"]
        f = faces(u, v, w, h, d)
        for fname, rect in f.items():
            kind = fname if fname in ("top", "bottom") else "side"
            paint_face(img, rect, S.pal, p["mat"], kind)
        if p["decor"]:
            decorate(p["decor"], f, img, S.pal)
    out = os.path.join(RES, "textures/entity")
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, name))


# =====================================================================================
# Java emission
# =====================================================================================
def fl(x):
    return f"{float(x):.4f}f"


def emit_java(S, with_visibility):
    L = []
    L.append("// GENERATED by tools/generate_assets.py - do not edit by hand (edit the generator and re-run).")
    L.append("package com.scarymonsters.client;")
    L.append("")
    L.append("import net.minecraft.client.model.Dilation;")
    L.append("import net.minecraft.client.model.ModelPartBuilder;")
    L.append("import net.minecraft.client.model.ModelPartData;")
    L.append("import net.minecraft.client.model.ModelTransform;")
    L.append("")
    L.append(f"final class {S.cls} {{")
    L.append(f"    private {S.cls}() {{}}")
    L.append("")
    if with_visibility:
        boxed = [p for p in S.parts if p["box"]]
        L.append("    static final String[] VIS_PATH = {" + ", ".join(f'"{S.chain(p["name"])}"' for p in boxed) + "};")
        L.append("    static final int[] VIS_LO = {" + ", ".join(str(p["lo"]) for p in boxed) + "};")
        L.append("    static final int[] VIS_HI = {" + ", ".join(str(p["hi"]) for p in boxed) + "};")
        L.append("")
    L.append("    static void build(ModelPartData root) {")
    for p in S.parts:
        parent = "root" if not p["parent"] else f"v_{p['parent']}"
        b = "ModelPartBuilder.create()"
        if p["box"]:
            x, y, z, w, h, d = p["box"]
            u, v = p["uv"]
            dil = f", new Dilation({fl(p['dil'])})" if p["dil"] else ""
            b += f".uv({u}, {v}).cuboid({fl(x)}, {fl(y)}, {fl(z)}, {fl(w)}, {fl(h)}, {fl(d)}{dil})"
        px_, py_, pz_ = p["pivot"]
        rx, ry, rz = p["rot"]
        tr = f"ModelTransform.of({fl(px_)}, {fl(py_)}, {fl(pz_)}, {fl(rx)}, {fl(ry)}, {fl(rz)})"
        L.append(f'        ModelPartData v_{p["name"]} = {parent}.addChild("{p["name"]}", {b}, {tr});')
    L.append("    }")
    L.append("}")
    with open(os.path.join(JAVA, S.cls + ".java"), "w") as fh:
        fh.write("\n".join(L) + "\n")


# =====================================================================================
# Item icons: a big curved claw whose look grows with the form
# =====================================================================================
def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def draw_icon(form):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    claw_l, claw_d = (255, 250, 232), (176, 160, 128)
    green, green_d = (118, 146, 52), (70, 92, 30)
    orange, red = (240, 130, 40), (220, 50, 30)
    dist = {}
    steps = 90
    for s in range(steps + 1):
        t = s / steps
        cx = 2.4 + 11.2 * (t ** 0.9)
        cy = 13.4 - 11.0 * (math.sin(t * math.pi * 0.5) ** 1.3)
        th = 3.8 * (1 - t) + 0.8
        for y in range(16):
            for x in range(16):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d <= th / 2 + 0.25:
                    r = d / (th / 2)
                    if (x, y) not in dist or r < dist[(x, y)][0]:
                        dist[(x, y)] = (r, t)
    for (x, y), (r, t) in dist.items():
        if t < 0.22:
            c = lerp(green, green_d, min(1, r))
            if (x + y) % 3 == 0:
                c = lerp(c, green_d, 0.5)
        else:
            c = lerp(claw_l, claw_d, min(1, r) * 0.9)
            if t > 0.9 and form >= 3:
                c = lerp(c, red if form == 4 else orange, (t - 0.9) / 0.1)
        if form >= 2 and 0.22 <= t < 0.3:
            c = orange
        img.putpixel((x, y), c + (255,))
    base = img.copy()
    for y in range(16):
        for x in range(16):
            if base.getpixel((x, y))[3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and base.getpixel((nx, ny))[3] > 0:
                        img.putpixel((x, y), (36, 44, 20, 255))
                        break
    if form >= 3:
        for (x, y) in ((12, 2), (14, 4)):
            img.putpixel((x, y), orange + (255,))
    if form >= 4:
        for (x, y) in ((9, 1), (14, 7), (6, 4), (11, 5), (13, 1)):
            img.putpixel((x, y), (255, 190, 60, 255))
        for (x, y) in ((10, 0), (15, 3)):
            img.putpixel((x, y), red + (255,))
    return img


def build_items():
    out = os.path.join(RES, "textures/item")
    os.makedirs(out, exist_ok=True)
    for f in range(1, 5):
        draw_icon(f).save(os.path.join(out, f"scary_form{f}.png"))


if __name__ == "__main__":
    gear, raptor, rex = build_gear(), build_raptor(), build_rex()
    for S, tex, vis in ((gear, "dino_gear.png", True), (raptor, "dino_raptor.png", False), (rex, "dino_rex.png", False)):
        pack(S)
        build_texture(S, tex)
        emit_java(S, vis)
        print(f"{S.key}: {len(S.parts)} parts")
    build_items()
