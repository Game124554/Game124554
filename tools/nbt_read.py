import gzip, struct
def read(path):
    data = gzip.open(path).read(); pos = [0]
    def rd(fmt):
        v = struct.unpack_from(fmt, data, pos[0]); pos[0] += struct.calcsize(fmt); return v[0]
    def payload(t):
        if t == 1: return rd(">b")
        if t == 2: return rd(">h")
        if t == 3: return rd(">i")
        if t == 4: return rd(">q")
        if t == 5: return rd(">f")
        if t == 6: return rd(">d")
        if t == 8:
            n = rd(">H"); s = data[pos[0]:pos[0]+n].decode(); pos[0] += n; return s
        if t == 9:
            et = rd(">b"); n = rd(">i"); return [payload(et) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                tt = rd(">b")
                if tt == 0: return d
                n = rd(">H"); k = data[pos[0]:pos[0]+n].decode(); pos[0] += n
                d[k] = payload(tt)
        raise ValueError(t)
    assert rd(">b") == 10; rd(">H")
    root = payload(10); assert pos[0] == len(data); return root
