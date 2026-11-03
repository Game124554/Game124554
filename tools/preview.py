import sys, os
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(__file__))
from spec import FORMS, SCIENTISTS
T = "src/main/resources/assets/latexdrone/textures/entity/"
def crop(img, x, y, w, h): return img.crop((x, y, x + w, y + h))
def figure(path):
    s = Image.open(path).convert("RGBA")
    fig = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    layers = [((8, 8, 8, 8), (4, 0)), ((20, 20, 8, 12), (4, 8)), ((44, 20, 4, 12), (0, 8)), ((36, 52, 4, 12), (12, 8)),
              ((4, 20, 4, 12), (4, 20)), ((20, 52, 4, 12), (8, 20)),
              ((40, 8, 8, 8), (4, 0)), ((20, 36, 8, 12), (4, 8)), ((44, 36, 4, 12), (0, 8)), ((52, 52, 4, 12), (12, 8)),
              ((4, 36, 4, 12), (4, 20)), ((4, 52, 4, 12), (8, 20))]
    for (x, y, w, h), pos in layers:
        part = crop(s, x, y, w, h)
        fig.alpha_composite(part, pos)
    return fig.resize((64, 128), Image.NEAREST)
items = [(T + "latex/" + f["id"] + ".png", f["id"]) for f in FORMS] + [(T + "scientist/" + s["id"] + ".png", s["id"]) for s in SCIENTISTS]
cols = 14
rows = (len(items) + cols - 1) // cols
sheet = Image.new("RGBA", (cols * 72, rows * 136), (120, 130, 140, 255))
for i, (p, n) in enumerate(items):
    sheet.alpha_composite(figure(p), ((i % cols) * 72 + 4, (i // cols) * 136 + 4))
sheet.save("/home/claude/preview/skins.png")
