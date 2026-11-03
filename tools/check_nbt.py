"""Reads back every generated structure .nbt and validates it (independent parser)."""
import glob
import gzip
import os
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def read(buf, pos, t):
    if t == 1: return struct.unpack_from(">b", buf, pos)[0], pos + 1
    if t == 2: return struct.unpack_from(">h", buf, pos)[0], pos + 2
    if t == 3: return struct.unpack_from(">i", buf, pos)[0], pos + 4
    if t == 4: return struct.unpack_from(">q", buf, pos)[0], pos + 8
    if t == 5: return struct.unpack_from(">f", buf, pos)[0], pos + 4
    if t == 6: return struct.unpack_from(">d", buf, pos)[0], pos + 8
    if t == 8:
        n = struct.unpack_from(">H", buf, pos)[0]
        return buf[pos + 2:pos + 2 + n].decode("utf-8"), pos + 2 + n
    if t == 9:
        et, n = struct.unpack_from(">bi", buf, pos)
        pos += 5
        out = []
        for _ in range(n):
            v, pos = read(buf, pos, et)
            out.append(v)
        return out, pos
    if t == 10:
        d = {}
        while True:
            ct = buf[pos]; pos += 1
            if ct == 0: return d, pos
            name, pos = read(buf, pos, 8)
            d[name], pos = read(buf, pos, ct)
    raise ValueError(t)


def load(path):
    buf = gzip.open(path).read()
    assert buf[0] == 10
    _, pos = read(buf, 1, 8)
    root, pos = read(buf, pos, 10)
    assert pos == len(buf)
    return root


if __name__ == "__main__":
    files = sorted(glob.glob(os.path.join(ROOT, "src/main/resources/data/scpfoundation/structures/*.nbt")))
    blockids = set()
    ents = {}
    for f in files:
        r = load(f)
        sx, sy, sz = r["size"]
        pal = r["palette"]
        for b in r["blocks"]:
            x, y, z = b["pos"]
            assert 0 <= x < sx and 0 <= y < sy and 0 <= z < sz, f
            assert 0 <= b["state"] < len(pal)
        for p in pal:
            blockids.add(p["Name"])
        for e in r["entities"]:
            ents[e["nbt"]["id"]] = ents.get(e["nbt"]["id"], 0) + 1
        assert r["DataVersion"] == 3465
    print(len(files), "files OK")
    print("entities:", ents)
    print("block ids:", len(blockids))
    print(sorted(b for b in blockids))
