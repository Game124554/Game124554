"""Minimal big-endian NBT writer (gzip) for Minecraft structure templates."""
import gzip
import struct

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY = range(12)


class Tag:
    __slots__ = ("t", "v", "et")

    def __init__(self, t, v, et=None):
        self.t, self.v, self.et = t, v, et


def Byte(v): return Tag(BYTE, int(v))
def Short(v): return Tag(SHORT, int(v))
def Int(v): return Tag(INT, int(v))
def Float(v): return Tag(FLOAT, float(v))
def Double(v): return Tag(DOUBLE, float(v))
def Str(v): return Tag(STRING, str(v))
def List(et, items): return Tag(LIST, list(items), et)
def Compound(d): return Tag(COMPOUND, dict(d))


def _w_str(out, s):
    b = s.encode("utf-8")
    out += struct.pack(">H", len(b)) + b


def _w_payload(out, tag):
    t, v = tag.t, tag.v
    if t == BYTE: out += struct.pack(">b", v)
    elif t == SHORT: out += struct.pack(">h", v)
    elif t == INT: out += struct.pack(">i", v)
    elif t == LONG: out += struct.pack(">q", v)
    elif t == FLOAT: out += struct.pack(">f", v)
    elif t == DOUBLE: out += struct.pack(">d", v)
    elif t == STRING: _w_str(out, v)
    elif t == LIST:
        et = tag.et if v else (tag.et or END)
        out += struct.pack(">bi", et if v else (et if et else END), len(v))
        for item in v:
            assert item.t == et, (item.t, et)
            _w_payload(out, item)
    elif t == COMPOUND:
        for k, child in v.items():
            out += struct.pack(">b", child.t)
            _w_str(out, k)
            _w_payload(out, child)
        out += struct.pack(">b", END)
    elif t == INT_ARRAY:
        out += struct.pack(">i", len(v))
        for i in v: out += struct.pack(">i", i)
    else:
        raise ValueError(t)


def write_gz(path, root):
    out = bytearray()
    out += struct.pack(">b", COMPOUND)
    _w_str(out, "")
    _w_payload(out, root)
    with gzip.open(path, "wb") as f:
        f.write(bytes(out))


def to_tag(obj):
    """Convert plain python values to tags (dict->Compound, str->String, bool->Byte, int->Int, float->Double)."""
    if isinstance(obj, Tag): return obj
    if isinstance(obj, bool): return Byte(1 if obj else 0)
    if isinstance(obj, int): return Int(obj)
    if isinstance(obj, float): return Double(obj)
    if isinstance(obj, str): return Str(obj)
    if isinstance(obj, dict): return Compound({k: to_tag(v) for k, v in obj.items()})
    if isinstance(obj, (list, tuple)):
        items = [to_tag(x) for x in obj]
        return List(items[0].t if items else END, items)
    raise TypeError(type(obj))
