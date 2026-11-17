"""Tiny NBT writer + structure-template builder for Minecraft 1.20.1 (DataVersion 3465)."""
import gzip
import struct


class B(int):
    """Byte tag."""


class F(float):
    """Float tag."""


def _w_str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _tid(v):
    if isinstance(v, (B, bool)):
        return 1
    if isinstance(v, int):
        return 3
    if isinstance(v, F):
        return 5
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(type(v))


def _payload(v):
    t = _tid(v)
    if t == 1:
        return struct.pack(">b", int(v))
    if t == 3:
        return struct.pack(">i", v)
    if t == 5:
        return struct.pack(">f", v)
    if t == 6:
        return struct.pack(">d", v)
    if t == 8:
        return _w_str(v)
    if t == 9:
        et = _tid(v[0]) if v else 0
        for x in v:
            assert _tid(x) == et, "mixed list"
        return struct.pack(">b", et) + struct.pack(">i", len(v)) + b"".join(_payload(x) for x in v)
    if t == 10:
        out = b""
        for k, val in v.items():
            out += struct.pack(">b", _tid(val)) + _w_str(k) + _payload(val)
        return out + b"\x00"


def write_nbt(path, root):
    data = b"\x0a" + _w_str("") + _payload(root)
    with open(path, "wb") as f:
        f.write(gzip.compress(data))


# ---------------------------------------------------------------- reader (for verification)

def read_nbt(path):
    data = gzip.decompress(open(path, "rb").read())
    pos = [0]

    def rd(fmt):
        n = struct.calcsize(fmt)
        v = struct.unpack(fmt, data[pos[0]:pos[0] + n])
        pos[0] += n
        return v[0]

    def rs():
        n = rd(">H")
        s = data[pos[0]:pos[0] + n].decode("utf-8")
        pos[0] += n
        return s

    def rp(t):
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 8: return rs()
        if t == 9:
            et = rd(">b"); n = rd(">i")
            return [rp(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = rd(">b")
                if tt == 0: return d
                k = rs(); d[k] = rp(tt)
        if t == 11:
            n = rd(">i"); return [rd(">i") for _ in range(n)]
        raise ValueError(t)

    assert rd(">b") == 10
    rs()
    return rp(10)


# ---------------------------------------------------------------- template builder

DATA_VERSION = 3465


class Template:
    def __init__(self, sx, sy, sz):
        self.sx, self.sy, self.sz = sx, sy, sz
        self.blocks = {}

    def inb(self, x, y, z):
        return 0 <= x < self.sx and 0 <= y < self.sy and 0 <= z < self.sz

    def set(self, x, y, z, name, props=None, nbt=None):
        if not self.inb(x, y, z):
            return
        if ":" not in name:
            name = "minecraft:" + name
        key = (name, tuple(sorted((props or {}).items())))
        self.blocks[(x, y, z)] = (key, nbt)

    def get(self, x, y, z):
        v = self.blocks.get((x, y, z))
        return v[0][0] if v else None

    def remove(self, x, y, z):
        self.blocks.pop((x, y, z), None)

    def fill(self, x1, y1, z1, x2, y2, z2, name, props=None):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for z in range(min(z1, z2), max(z1, z2) + 1):
                    self.set(x, y, z, name, props)

    def to_nbt(self):
        palette, index, blocks = [], {}, []
        for (x, y, z), (key, nbt) in sorted(self.blocks.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            if key not in index:
                index[key] = len(palette)
                entry = {"Name": key[0]}
                if key[1]:
                    entry["Properties"] = {k: str(v) for k, v in key[1]}
                palette.append(entry)
            b = {"state": index[key], "pos": [x, y, z]}
            if nbt is not None:
                b["nbt"] = nbt
            blocks.append(b)
        return {
            "DataVersion": DATA_VERSION,
            "size": [self.sx, self.sy, self.sz],
            "palette": palette,
            "blocks": blocks,
            "entities": [],
        }

    def save(self, path):
        write_nbt(path, self.to_nbt())
