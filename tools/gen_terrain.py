#!/usr/bin/env python3
"""
Generates the built-in "realistic_terrain" data pack from vanilla 1.20.1 worldgen JSON.

Vanilla data: git clone --branch 1.20.1-data-json https://github.com/misode/mcmeta
Usage: python3 tools/gen_terrain.py <mcmeta>/data/minecraft/worldgen

Changes compared to vanilla:
  * continents / erosion noise sampled 2x larger  -> wide continents, long coasts, big plains and highlands
  * ridges sampled 1.4x larger                     -> longer mountain ranges and fewer, wider river valleys
  * terrain height (offset) x HEIGHT                -> taller mountains, deeper oceans and valleys
  * peak jaggedness x JAGGED                        -> sharper alpine peaks
  * top slide moved from 240-256 up to 280-300      -> tall peaks are not cut flat
"""
import copy, json, os, sys

CONTINENT_SCALE = 0.125   # vanilla 0.25
EROSION_SCALE = 0.125     # vanilla 0.25
RIDGE_SCALE = 0.18        # vanilla 0.25
HEIGHT = 1.25
JAGGED = 1.2
TOP_SLIDE = (280, 300)    # vanilla (240, 256)

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


def scale_spline(node, k):
    """Wrap every spline found under 'node' in mul(k, spline)."""
    count = 0
    if isinstance(node, dict):
        for key, val in list(node.items()):
            if isinstance(val, dict) and val.get('type') == 'minecraft:spline':
                node[key] = {'type': 'minecraft:mul', 'argument1': k, 'argument2': val}
                count += 1
            else:
                count += scale_spline(val, k)
    elif isinstance(node, list):
        for v in node:
            count += scale_spline(v, k)
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
assert scale_spline(d, HEIGHT) == 1
save('density_function/overworld/offset.json', d)

d = load('density_function/overworld/jaggedness.json')
assert scale_spline(d, JAGGED) == 1
save('density_function/overworld/jaggedness.json', d)

d = load('noise_settings/overworld.json')
n = move_top_slide(d)
assert n >= 2, n
save('noise_settings/overworld.json', d)
print('top slide moved in', n, 'places')
