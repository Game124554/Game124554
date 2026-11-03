#!/usr/bin/env python3
"""
Generates the built-in "realistic_terrain" data pack from vanilla 1.20.1 worldgen JSON.

Vanilla data: git clone --branch 1.20.1-data-json https://github.com/misode/mcmeta
Usage: python3 tools/gen_terrain.py <mcmeta>/data/minecraft/worldgen

Changes compared to vanilla:
  * continents / erosion noise sampled 2x larger  -> wide continents, long coasts, big plains and highlands
  * ridges sampled 1.4x larger                     -> longer mountain ranges and fewer, wider river valleys
  * terrain height (offset) x HEIGHT_UP / HEIGHT_DOWN -> taller mountains, deeper oceans and valleys
  * peak jaggedness x JAGGED                        -> sharper alpine peaks
  * top slide moved from 240-256 up to 280-300      -> tall peaks are not cut flat
"""
import copy, json, os, sys

def env(name, default):
    return float(os.environ.get(name, default))


CONTINENT_SCALE = env('RT_CONTINENT', 0.125)  # vanilla 0.25 (smaller = bigger continents)
EROSION_SCALE = env('RT_EROSION', 0.125)      # vanilla 0.25
RIDGE_SCALE = env('RT_RIDGE', 0.18)           # vanilla 0.25
HEIGHT_UP = env('RT_HEIGHT_UP', 1.25)         # land above sea level x this
HEIGHT_DOWN = env('RT_HEIGHT_DOWN', 1.25)     # sea floor below sea level x this
JAGGED = env('RT_JAGGED', 1.2)                # peak sharpness
TOP_SLIDE = (280, 300)                        # vanilla (240, 256)

src = sys.argv[1]
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources',
                   'resourcepacks', 'realistic_terrain', 'data', 'minecraft', 'worldgen')


def load(rel):
    with open(os.path.join(src, rel)) as f:
        return json.load(f)


def save(rel, data):
    p = os.path.join(out, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w') as f:
        json.dump(data, f, indent=2)
    print('wrote', rel)


def scale_spline(node, up, down=None):
    """Wrap every spline under 'node': positive values x up, negative values x down."""
    count = 0
    if isinstance(node, dict):
        for key, val in list(node.items()):
            if isinstance(val, dict) and val.get('type') == 'minecraft:spline':
                if down is None or down == up:
                    node[key] = {'type': 'minecraft:mul', 'argument1': up, 'argument2': val}
                else:
                    node[key] = {'type': 'minecraft:add',
                                 'argument1': {'type': 'minecraft:mul', 'argument1': up,
                                               'argument2': {'type': 'minecraft:max', 'argument1': 0.0, 'argument2': val}},
                                 'argument2': {'type': 'minecraft:mul', 'argument1': down,
                                               'argument2': {'type': 'minecraft:min', 'argument1': 0.0, 'argument2': copy.deepcopy(val)}}}
                count += 1
            else:
                count += scale_spline(val, up, down)
    elif isinstance(node, list):
        for v in node:
            count += scale_spline(v, up, down)
    return count


def move_top_slide(node):
    count = 0
    if isinstance(node, dict):
        if node.get('type') == 'minecraft:y_clamped_gradient' and node.get('from_y') == 240 and node.get('to_y') == 256:
            node['from_y'], node['to_y'] = TOP_SLIDE
            count += 1
        for v in node.values():
            count += move_top_slide(v)
    elif isinstance(node, list):
        for v in node:
            count += move_top_slide(v)
    return count


for name, scale in (('continents', CONTINENT_SCALE), ('erosion', EROSION_SCALE), ('ridges', RIDGE_SCALE)):
    d = load(f'density_function/overworld/{name}.json')
    d['argument']['xz_scale'] = scale
    save(f'density_function/overworld/{name}.json', d)

d = load('density_function/overworld/offset.json')
assert scale_spline(d, HEIGHT_UP, HEIGHT_DOWN) == 1
save('density_function/overworld/offset.json', d)

d = load('density_function/overworld/jaggedness.json')
assert scale_spline(d, JAGGED) == 1
save('density_function/overworld/jaggedness.json', d)

d = load('noise_settings/overworld.json')
n = move_top_slide(d)
assert n >= 2, n
save('noise_settings/overworld.json', d)
print('top slide moved in', n, 'places')
print('settings:', dict(continent=CONTINENT_SCALE, erosion=EROSION_SCALE, ridge=RIDGE_SCALE,
                        height_up=HEIGHT_UP, height_down=HEIGHT_DOWN, jagged=JAGGED))
