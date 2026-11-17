"""Checks the client assets: every registered item/block has an item model definition, every model and blockstate
reference resolves, and every referenced texture exists as a valid PNG (with sane animation metadata).

Usage: python -I tools/check_assets.py <project-dir>
Exit code 1 on any problem.
"""
import json
import re
import struct
import sys
from pathlib import Path

ROOT = Path(sys.argv[1])
ASSETS = ROOT / "src/main/resources/assets/forbidden"
SRC = ROOT / "src/main/java/fox/spiteful/forbidden/registry"
DATA = ROOT / "src/main/resources/data/forbidden"
problems = []


def ns_path(ref, folder, ext):
    ns, _, path = ref.partition(":") if ":" in ref else ("minecraft", "", ref)
    return ns, path, ASSETS.parent / ns / folder / f"{path}{ext}"


def png_size(path):
    data = path.read_bytes()[:24]
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        return None
    return struct.unpack(">II", data[16:24])


def check_texture(ref, where):
    ns, path, file = ns_path(ref, "textures", ".png")
    if ns != "forbidden":
        return
    if not file.exists():
        problems.append(f"{where}: missing texture {ref}")
        return
    size = png_size(file)
    if size is None:
        problems.append(f"{where}: {ref} is not a PNG")
        return
    w, h = size
    meta = file.with_name(file.name + ".mcmeta")
    if meta.exists():
        json.loads(meta.read_text(encoding="utf-8"))
        if h % w:
            problems.append(f"{where}: animated {ref} is {w}x{h}, height not a multiple of width")
    elif w != h and "textures/models" not in str(file) and "entity" not in str(file):
        problems.append(f"{where}: {ref} is {w}x{h} but not square and not animated")


checked_models = set()


def check_model(ref, where):
    ns, path, file = ns_path(ref, "models", ".json")
    if ns != "forbidden" or ref in checked_models:
        return
    checked_models.add(ref)
    if not file.exists():
        problems.append(f"{where}: missing model {ref}")
        return
    model = json.loads(file.read_text(encoding="utf-8"))
    if "parent" in model:
        check_model(model["parent"], ref)
    for key, tex in model.get("textures", {}).items():
        if not tex.startswith("#"):
            check_texture(tex, f"{ref}#{key}")


def walk_item_model(node, where):
    if isinstance(node, dict):
        if node.get("type") == "minecraft:model":
            check_model(node["model"], where)
        for value in node.values():
            walk_item_model(value, where)
    elif isinstance(node, list):
        for value in node:
            walk_item_model(value, where)


items, blocks = set(), set()
for java in SRC.glob("*.java"):
    text = java.read_text(encoding="utf-8")
    items |= set(re.findall(r'ITEMS\.register(?:Simple)?Item\("([a-z_]+)"', text))
    blocks |= set(re.findall(r'BLOCKS\.registerBlock\("([a-z_]+)"', text))
    for block in re.findall(r'registerSimpleBlockItem\(([A-Z_]+)', text):
        pass
items |= blocks  # every block has a block item

for item in sorted(items):
    file = ASSETS / "items" / f"{item}.json"
    if not file.exists():
        problems.append(f"item {item}: no items/{item}.json")
        continue
    walk_item_model(json.loads(file.read_text(encoding="utf-8")), f"items/{item}")

for block in sorted(blocks):
    file = ASSETS / "blockstates" / f"{block}.json"
    if not file.exists():
        problems.append(f"block {block}: no blockstate")
        continue
    state = json.loads(file.read_text(encoding="utf-8"))
    for variant in state.get("variants", {}).values():
        for v in variant if isinstance(variant, list) else [variant]:
            check_model(v["model"], f"blockstates/{block}")

# Textures referenced from data: wand rods, aspects, research icons and backgrounds, spell parts, villager.
for ref in ["forbidden:models/wand_rod_tainted", "forbidden:models/wand_rod_infernal", "forbidden:models/wand_rod_profane",
            "forbidden:models/wand_rod_profaned", "forbidden:entity/villager/profession/heretic"]:
    check_texture(ref, "code")
for f in (DATA / "thaumaturge").rglob("*.json"):
    for ref in re.findall(r'"forbidden:textures/([a-z0-9_/]+)\.png"', f.read_text(encoding="utf-8")):
        check_texture(f"forbidden:{ref}", f.relative_to(DATA).as_posix())

print(f"checked {len(items)} items, {len(blocks)} blocks, {len(checked_models)} models")
for p in problems:
    print("PROBLEM", p)
sys.exit(1 if problems else 0)
