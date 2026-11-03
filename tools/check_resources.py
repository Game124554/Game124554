"""Cross-checks registry ids in Java against models, blockstates, textures, lang, loot and structures."""
import json, os, re, glob, sys
R = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = f"{R}/src/main/resources/assets/scpfoundation"
D = f"{R}/src/main/resources/data/scpfoundation"
java = "".join(open(f).read() for f in glob.glob(f"{R}/src/main/java/**/*.java", recursive=True))
blocks = re.findall(r'register\("([a-z0-9_]+)",\s*\n?\s*new (?:Block|GlassBlock|Scp\w+Block|KeycardReaderBlock)', java) + [f"keycard_reader_{i}" for i in range(1, 6)]
items = re.findall(r'register\("([a-z0-9_]+)", new ', open(f"{R}/src/main/java/net/scpfoundation/registry/ModItems.java").read())
eggs = [m + "_spawn_egg" for m in re.findall(r'egg\("([a-z0-9_]+)"', java)]
entities = re.findall(r'register\("([a-z0-9_]+)",\s*\n\s*EntityType', java)
lang = json.load(open(f"{A}/lang/en_us.json"))
errs = []
def tex_exists(ref):
    ns, path = ref.split(":") if ":" in ref else ("minecraft", ref)
    return ns != "scpfoundation" or os.path.exists(f"{A}/textures/{path}.png")
for b in set(blocks):
    bs = f"{A}/blockstates/{b}.json"
    if not os.path.exists(bs): errs.append("blockstate " + b); continue
    for v in json.load(open(bs))["variants"].values():
        m = v["model"].split(":")[1]
        mf = f"{A}/models/{m}.json"
        if not os.path.exists(mf): errs.append("model " + m); continue
        for t in json.load(open(mf)).get("textures", {}).values():
            if not t.startswith("#") and not tex_exists(t): errs.append(f"texture {t} in {m}")
    if f"block.scpfoundation.{b}" not in lang: errs.append("lang block " + b)
    if not os.path.exists(f"{D}/loot_tables/blocks/{b}.json"): errs.append("loot " + b)
    if not os.path.exists(f"{A}/models/item/{b}.json"): errs.append("item model " + b)
for i in items + eggs:
    mf = f"{A}/models/item/{i}.json"
    if not os.path.exists(mf): errs.append("item model " + i); continue
    for t in json.load(open(mf)).get("textures", {}).values():
        if not tex_exists(t): errs.append(f"texture {t}")
    if f"item.scpfoundation.{i}" not in lang: errs.append("lang item " + i)
for e in entities:
    if f"entity.scpfoundation.{e}" not in lang: errs.append("lang entity " + e)
# translation keys used in Java
for k in set(re.findall(r'"((?:message|tooltip|database|command|itemGroup|scp914|drink|item)\.scpfoundation[.a-z0-9_]*|scp914\.setting\.)', java)):
    if k.endswith(".") : continue
    if k not in lang and not any(x.startswith(k) for x in lang): errs.append("lang key " + k)
# all ids referenced in loot tables & structures exist
known = {f"scpfoundation:{x}" for x in set(blocks) | set(items) | set(eggs)}
for f in glob.glob(f"{D}/loot_tables/**/*.json", recursive=True):
    for n in re.findall(r'"name": "(scpfoundation:[a-z0-9_]+)"', open(f).read()):
        if n not in known: errs.append(f"loot ref {n} in {f}")
sys.path.insert(0, f"{R}/tools")
from check_nbt import load
lt = {os.path.relpath(f, f"{D}/loot_tables")[:-5] for f in glob.glob(f"{D}/loot_tables/**/*.json", recursive=True)}
for f in glob.glob(f"{D}/structures/*.nbt"):
    r = load(f)
    for pz in r["palette"]:
        if pz["Name"].startswith("scpfoundation:") and pz["Name"] not in known: errs.append(f"structure block {pz['Name']}")
    for b in r["blocks"]:
        if "nbt" in b and "LootTable" in b["nbt"]:
            t = b["nbt"]["LootTable"].split(":")[1]
            if t not in lt: errs.append(f"structure loot {t} in {os.path.basename(f)}")
    for e in r["entities"]:
        eid = e["nbt"]["id"]
        if eid.startswith("scpfoundation:") and eid.split(":")[1] not in entities: errs.append("structure entity " + eid)
for f in glob.glob(f"{R}/src/main/resources/**/*.json", recursive=True):
    json.load(open(f))
print("blocks", len(set(blocks)), "items", len(items), "eggs", len(eggs), "entities", len(entities))
print("ERRORS:", errs if errs else "none")
