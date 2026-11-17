"""
3D gun models for Changed: Thunder Science Expansion.

Writes, for the pistol, assault rifle and shotgun:
  assets/changed_tsc/models/item/<gun>.json   (cuboid "elements" model + display transforms)
  assets/changed_tsc/textures/item/gun_palette.png  (one shared 16x16 colour palette)
and, with a second argument, a preview picture of the three guns.

Usage:  python gen_gun_models.py <project dir> [preview.png]

Every gun is built pointing NORTH (-Z) with +Y up, which is how Minecraft expects a held 3D item
when the arm is in an aiming pose. The grip is moved to the model centre (8, 8, 8) so it sits in
the hand. To change a gun, edit its box list below and run the script again, or open the .json in
Blockbench.
"""
import json, math, os, sys
import numpy as np
from PIL import Image

MOD = "changed_tsc"

# ----------------------------------------------------------------------------- palette (4x4 swatches of 4px)
PALETTE = ["gunmetal_dark", "gunmetal", "steel", "black",
           "rubber", "wood", "wood_dark", "yellow",
           "muzzle", "white", "red", "brass",
           "polymer", "mag", "blue_steel", "highlight"]
COLORS = {"gunmetal_dark": (0x2B, 0x2E, 0x33), "gunmetal": (0x3C, 0x40, 0x47), "steel": (0x6E, 0x74, 0x7D),
          "black": (0x15, 0x16, 0x1A), "rubber": (0x1E, 0x1E, 0x22), "wood": (0x7A, 0x4E, 0x2A),
          "wood_dark": (0x5A, 0x37, 0x19), "yellow": (0xF2, 0xC2, 0x1A), "muzzle": (0x0A, 0x0A, 0x0C),
          "white": (0xE8, 0xE8, 0xE8), "red": (0xD1, 0x30, 0x30), "brass": (0xB8, 0x86, 0x2B),
          "polymer": (0x4A, 0x4F, 0x57), "mag": (0x24, 0x26, 0x2B), "blue_steel": (0x2F, 0x3A, 0x4A),
          "highlight": (0x92, 0x99, 0xA3)}


def swatch_uv(name):
    i = PALETTE.index(name)
    u, v = (i % 4) * 4, (i // 4) * 4
    return [u + 0.5, v + 0.5, u + 3.5, v + 3.5]


def write_palette(path):
    img = Image.new("RGBA", (16, 16))
    rng = np.random.default_rng(7)
    for i, name in enumerate(PALETTE):
        r, g, b = COLORS[name]
        for py in range(4):
            for px in range(4):
                d = int(rng.integers(-5, 6))
                img.putpixel(((i % 4) * 4 + px, (i // 4) * 4 + py),
                             (max(0, min(255, r + d)), max(0, min(255, g + d)), max(0, min(255, b + d)), 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


# ----------------------------------------------------------------------------- the guns
# box = (name, colour, [x0, y0, z0], [x1, y1, z1], optional (angle, origin) rotation about the X axis)
# Muzzle is at low Z. "grip" is the point that goes into the hand.

PISTOL = dict(
    grip=(8, 6.4, 9.8),
    boxes=[
        ("slide", "gunmetal", [7, 9.5, 2.5], [9, 11.5, 11]),
        ("slide_serrations", "gunmetal_dark", [6.95, 9.8, 9], [9.05, 11.2, 10.4]),
        ("frame", "gunmetal_dark", [7.1, 8.5, 3.6], [8.9, 9.5, 11]),
        ("muzzle", "muzzle", [7.5, 10, 2.3], [8.5, 11, 2.6]),
        ("tsc_stripe", "yellow", [6.95, 10.2, 4.5], [9.05, 10.7, 8]),
        ("grip", "rubber", [7.1, 4.5, 8.6], [8.9, 8.6, 10.9], (-22.5, [8, 8.5, 9.6])),
        ("magazine_base", "mag", [7, 4.1, 8.5], [9, 4.6, 11.2], (-22.5, [8, 8.5, 9.6])),
        ("guard_bottom", "gunmetal_dark", [7.6, 7.1, 6.2], [8.4, 7.5, 8.4]),
        ("guard_front", "gunmetal_dark", [7.6, 7.1, 6.2], [8.4, 8.5, 6.6]),
        ("trigger", "steel", [7.8, 7.5, 7.5], [8.2, 8.5, 7.8]),
        ("rear_sight", "black", [7.3, 11.5, 10], [8.7, 11.9, 10.6]),
        ("front_sight", "white", [7.8, 11.5, 3], [8.2, 11.9, 3.5]),
        ("hammer", "steel", [7.7, 10.4, 11], [8.3, 11.2, 11.4]),
    ],
    third=dict(translation=[0, -1.5, -1], scale=0.85),
    first=dict(rotation=[0, 5, 0], translation=[-1.5, 2.5, -1], scale=1.0),
)

RIFLE = dict(
    grip=(8, 6.6, 14.9),
    boxes=[
        ("barrel", "blue_steel", [7.5, 10, 0], [8.5, 11, 6]),
        ("muzzle_brake", "black", [7.3, 9.8, -0.8], [8.7, 11.2, 0.6]),
        ("handguard", "polymer", [6.9, 9.3, 3], [9.1, 11.5, 9.5]),
        ("tsc_stripe", "yellow", [6.85, 10.1, 4], [9.15, 10.6, 8.5]),
        ("front_sight", "black", [7.8, 11.5, 3.5], [8.2, 12.6, 4.2]),
        ("receiver_upper", "gunmetal", [7, 9.5, 9.5], [9, 12, 15.5]),
        ("receiver_lower", "gunmetal_dark", [7.1, 8.3, 10], [8.9, 9.5, 15.5]),
        ("ejection_port", "steel", [8.95, 10.4, 11.5], [9.1, 11.2, 13.5]),
        ("magazine", "mag", [7.3, 4.2, 10.3], [8.7, 8.4, 12.7]),
        ("magazine_base", "black", [7.2, 3.9, 10.2], [8.8, 4.3, 12.8]),
        ("grip", "rubber", [7.2, 4.9, 14], [8.8, 8.4, 15.8], (-22.5, [8, 8.3, 14.9])),
        ("guard", "gunmetal_dark", [7.6, 7.3, 12.8], [8.4, 7.6, 14.2]),
        ("trigger", "steel", [7.8, 7.6, 13.5], [8.2, 8.3, 13.8]),
        ("stock_tube", "gunmetal_dark", [7.4, 9.8, 15.5], [8.6, 11.2, 19]),
        ("stock", "polymer", [7.1, 8, 18.5], [8.9, 12, 21]),
        ("stock_pad", "rubber", [7, 7.9, 21], [9, 12.1, 21.5]),
        ("rail", "black", [7.4, 12, 9.8], [8.6, 12.4, 15.2]),
        ("optic", "gunmetal", [7.3, 12.4, 11], [8.7, 13.8, 14]),
        ("optic_lens", "red", [7.5, 12.6, 10.85], [8.5, 13.6, 11.05]),
        ("optic_rear", "black", [7.5, 12.6, 13.95], [8.5, 13.6, 14.15]),
    ],
    third=dict(translation=[0, -1.5, -1], scale=0.8),
    first=dict(rotation=[0, 4, 0], translation=[-1.5, 2.5, 0.5], scale=0.9),
)

SHOTGUN = dict(
    grip=(8, 8.6, 17.2),
    boxes=[
        ("barrel", "blue_steel", [7.4, 10.4, 0], [8.6, 11.6, 12]),
        ("muzzle", "muzzle", [7.6, 10.6, -0.15], [8.4, 11.4, 0.05]),
        ("magazine_tube", "gunmetal_dark", [7.5, 9.2, 1.5], [8.5, 10.4, 12]),
        ("bead_sight", "brass", [7.8, 11.6, 0.4], [8.2, 12, 0.9]),
        ("pump", "wood", [6.9, 8.7, 5], [9.1, 10.5, 9.5]),
        ("pump_groove_1", "wood_dark", [6.85, 8.9, 5.9], [9.15, 10.3, 6.3]),
        ("pump_groove_2", "wood_dark", [6.85, 8.9, 7.1], [9.15, 10.3, 7.5]),
        ("pump_groove_3", "wood_dark", [6.85, 8.9, 8.3], [9.15, 10.3, 8.7]),
        ("receiver", "gunmetal", [7, 8.6, 12], [9, 11.8, 16]),
        ("tsc_tag", "yellow", [6.95, 10.5, 13], [9.05, 11, 15]),
        ("ejection_port", "black", [8.95, 9.4, 12.8], [9.1, 10.3, 14.6]),
        ("guard", "gunmetal_dark", [7.6, 7.6, 13.2], [8.4, 7.9, 15]),
        ("trigger", "steel", [7.8, 7.9, 14.2], [8.2, 8.6, 14.5]),
        ("stock_wrist", "wood", [7.2, 7.9, 16], [8.8, 10.4, 18.4]),
        ("stock", "wood", [7.2, 6.4, 18], [8.8, 10.2, 22]),
        ("stock_stripe", "wood_dark", [7.15, 8.1, 18.6], [8.85, 8.5, 21.6]),
        ("butt_pad", "black", [7.1, 6.3, 22], [8.9, 10.3, 22.6]),
    ],
    third=dict(translation=[0, -1.5, -1], scale=0.8),
    first=dict(rotation=[0, 4, 0], translation=[-1.5, 2, 0.5], scale=0.9),
)

GUNS = {"tsc_pistol": PISTOL, "tsc_assault_rifle": RIFLE, "tsc_shotgun": SHOTGUN}
FACES = ["north", "south", "east", "west", "up", "down"]


# ----------------------------------------------------------------------------- maths shared by the model writer and the preview
def rot(axis, deg):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    if axis == "x":
        return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])
    if axis == "y":
        return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def display_matrix(rotation):
    """Minecraft applies a display rotation as X * Y * Z."""
    rx, ry, rz = rotation
    return rot("x", rx) @ rot("y", ry) @ rot("z", rz)


def placed_boxes(gun):
    """Boxes moved so the grip sits at the model centre (8, 8, 8)."""
    off = np.array([8.0, 8.0, 8.0]) - np.array(gun["grip"], dtype=float)
    out = []
    for box in gun["boxes"]:
        name, colour, a, b = box[:4]
        rotation = box[4] if len(box) > 4 else None
        a = np.round(np.array(a, dtype=float) + off, 3)
        b = np.round(np.array(b, dtype=float) + off, 3)
        if rotation:
            rotation = (rotation[0], np.round(np.array(rotation[1], dtype=float) + off, 3))
        out.append((name, colour, a, b, rotation))
    return out


def box_corners(a, b, rotation):
    pts = np.array([[x, y, z] for x in (a[0], b[0]) for y in (a[1], b[1]) for z in (a[2], b[2])])
    if rotation:
        angle, origin = rotation
        pts = (pts - origin) @ rot("x", angle).T + origin
    return pts


def all_points(boxes):
    return np.vstack([box_corners(a, b, r) for _, _, a, b, r in boxes])


def centred(boxes, rotation, fit):
    """Display transform that shows the whole gun centred in a 16x16 slot (inventory, item frame)."""
    R = display_matrix(rotation)
    pts = (all_points(boxes) - 8.0) @ R.T
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    scale = min(1.3, fit / max(hi[0] - lo[0], hi[1] - lo[1]))
    centre = (lo + hi) / 2.0 * scale
    return dict(rotation=list(rotation), translation=[round(-centre[0], 2), round(-centre[1], 2), 0],
                scale=[round(scale, 3)] * 3)


def model_json(gun):
    boxes = placed_boxes(gun)
    elements = []
    for name, colour, a, b, rotation in boxes:
        assert all(a[i] < b[i] for i in range(3)), name
        assert a.min() >= -16 and b.max() <= 32, name
        el = {"name": name, "from": a.tolist(), "to": b.tolist()}
        if rotation:
            el["rotation"] = {"angle": rotation[0], "axis": "x", "origin": rotation[1].tolist()}
        el["faces"] = {f: {"uv": swatch_uv(colour), "texture": "#0"} for f in FACES}
        elements.append(el)
    third, first = gun["third"], gun["first"]
    display = {
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": third["translation"], "scale": [third["scale"]] * 3},
        "firstperson_righthand": {"rotation": first["rotation"], "translation": first["translation"], "scale": [first["scale"]] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
        "gui": centred(boxes, (15, -115, 0), 14.5),
        "fixed": centred(boxes, (0, 90, 0), 15.0),
    }
    return {
        "credit": "Thunder Science Company - generated by tools/gen_gun_models.py",
        "texture_size": [16, 16],
        "textures": {"0": f"{MOD}:item/gun_palette", "particle": f"{MOD}:item/gun_palette"},
        "gui_light": "side",
        "elements": elements,
        "display": display,
    }


# ----------------------------------------------------------------------------- preview (small z-buffer renderer)
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "east": 0.6, "west": 0.6}
QUADS = {"west": [0, 1, 3, 2], "east": [4, 6, 7, 5], "down": [0, 4, 5, 1], "up": [2, 3, 7, 6], "north": [0, 2, 6, 4], "south": [1, 5, 7, 3]}


def render(gun, rotation, px_per_unit, size):
    w, h = size
    colour = np.zeros((h, w, 4), dtype=np.uint8)
    depth = np.full((h, w), -1e9)
    R = display_matrix(rotation)
    boxes = placed_boxes(gun)
    pts_all = (all_points(boxes) - 8.0) @ R.T
    mid = (pts_all.min(axis=0) + pts_all.max(axis=0)) / 2.0
    ys, xs = np.mgrid[0:h, 0:w]
    for name, col, a, b, rotn in boxes:
        corners = (box_corners(a, b, rotn) - 8.0) @ R.T - mid
        sx = corners[:, 0] * px_per_unit + w / 2.0
        sy = -corners[:, 1] * px_per_unit + h / 2.0
        sz = corners[:, 2]
        for face, idx in QUADS.items():
            rgb = tuple(int(c * SHADE[face]) for c in COLORS[col])
            for tri in ((idx[0], idx[1], idx[2]), (idx[0], idx[2], idx[3])):
                x0, y0, x1, y1, x2, y2 = sx[tri[0]], sy[tri[0]], sx[tri[1]], sy[tri[1]], sx[tri[2]], sy[tri[2]]
                den = (y1 - y2) * (x0 - x2) + (x2 - x1) * (y0 - y2)
                if abs(den) < 1e-9:
                    continue
                minx, maxx = max(int(min(x0, x1, x2)), 0), min(int(max(x0, x1, x2)) + 1, w - 1)
                miny, maxy = max(int(min(y0, y1, y2)), 0), min(int(max(y0, y1, y2)) + 1, h - 1)
                if minx > maxx or miny > maxy:
                    continue
                gx = xs[miny:maxy + 1, minx:maxx + 1] + 0.5
                gy = ys[miny:maxy + 1, minx:maxx + 1] + 0.5
                l0 = ((y1 - y2) * (gx - x2) + (x2 - x1) * (gy - y2)) / den
                l1 = ((y2 - y0) * (gx - x2) + (x0 - x2) * (gy - y2)) / den
                l2 = 1.0 - l0 - l1
                z = l0 * sz[tri[0]] + l1 * sz[tri[1]] + l2 * sz[tri[2]]
                sub = depth[miny:maxy + 1, minx:maxx + 1]
                hit = (l0 >= 0) & (l1 >= 0) & (l2 >= 0) & (z > sub)
                sub[hit] = z[hit]
                colour[miny:maxy + 1, minx:maxx + 1][hit] = rgb + (255,)
    return Image.fromarray(colour, "RGBA")


def preview(path):
    from PIL import ImageDraw, ImageFont
    try:
        font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 14)
    except Exception:
        font = ImageFont.load_default()
    views = [("side", (0, -90, 0)), ("inventory icon", (15, -115, 0)), ("three-quarter", (25, -140, 0)), ("from behind", (20, 25, 0))]
    cell_w, cell_h = 330, 190
    sheet = Image.new("RGBA", (cell_w * len(views) + 20, cell_h * len(GUNS) + 40), (58, 62, 70, 255))
    d = ImageDraw.Draw(sheet)
    for col, (label, _) in enumerate(views):
        d.text((20 + col * cell_w, 8), label, fill=(210, 210, 210), font=font)
    for row, (name, gun) in enumerate(GUNS.items()):
        for col, (_, rotation) in enumerate(views):
            img = render(gun, rotation, 13, (cell_w - 10, cell_h - 10))
            sheet.alpha_composite(img, (10 + col * cell_w, 32 + row * cell_h))
        d.text((20, 32 + row * cell_h + cell_h - 28), name, fill=(240, 200, 60), font=font)
    sheet.convert("RGB").save(path)


def main():
    root = sys.argv[1] if len(sys.argv) > 1 else ".."
    assets = os.path.join(root, "src/main/resources/assets", MOD)
    write_palette(os.path.join(assets, "textures/item/gun_palette.png"))
    for name, gun in GUNS.items():
        path = os.path.join(assets, "models/item", name + ".json")
        with open(path, "w") as f:
            json.dump(model_json(gun), f, indent=1)
        print("wrote", path, len(gun["boxes"]), "boxes")
    if len(sys.argv) > 2:
        preview(sys.argv[2])
        print("preview", sys.argv[2])


if __name__ == "__main__":
    main()
