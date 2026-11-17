"""Generates the client assets of the Forbidden Magic port from the original FM textures (WTFPL)."""
import json
import shutil
import sys
from pathlib import Path

FM = Path(sys.argv[1])          # original assets/forbidden
OUT = Path(sys.argv[2])         # src/main/resources/assets/forbidden

def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def copy(src: Path, dst: Path):
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(src, dst)
    meta = src.with_name(src.name + ".mcmeta")
    if meta.exists():
        shutil.copyfile(meta, dst.with_name(dst.name + ".mcmeta"))

# item id -> (FM texture name, handheld?)
SIMPLE = {
    "wrath_shard": ("wrathshard", False),
    "envy_shard": ("envyshard", False),
    "taint_shard": ("taintshard", False),
    "pride_shard": ("prideshard", False),
    "lust_shard": ("lustshard", False),
    "sloth_shard": ("slothshard", False),
    "greed_shard": ("greedshard", False),
    "gluttony_shard": ("gluttonyshard", False),
    "emerald_nugget": ("emerald_nugget", False),
    "black_ink": ("dye_powder_black", False),
    "taint_fruit": ("taint_fruit", False),
    "taint_coal": ("taint_charcoal", False),
    "skull_axe": ("skullaxe", True),
    "diabolist_fork": ("fork", True),
    "taint_shovel": ("taintshovel", True),
    "taint_pickaxe": ("taintpickaxe", True),
    "riding_crop": ("crop", True),
    "wand_rod_tainted": ("wand_rod_tainted", True),
    "wand_rod_infernal": ("wand_rod_infernal", True),
    "wand_rod_profane": ("wand_rod_profane", True),
    "ring_nutrition": ("ring_nutrition", False),
    "sub_collar": ("collar", False),
    "crystalwell": ("crystalwell", False),
    "primewell": ("primewell", False),
}
# morphic tools: base texture + tinted overlay (phase colour from custom_model_data colors[0])
MORPH = {
    "morph_pickaxe": ("chameleonpick", "eyepick"),
    "morph_sword": ("chameleonsword", "eyesword"),
    "morph_shovel": ("chameleonshovel", "eyeshovel"),
    "morph_axe": ("chameleonaxe", "eyeaxe"),
}

for item, (tex, handheld) in SIMPLE.items():
    copy(FM / "textures/items" / f"{tex}.png", OUT / "textures/item" / f"{item}.png")
    write(OUT / "models/item" / f"{item}.json", {
        "parent": "minecraft:item/handheld" if handheld else "minecraft:item/generated",
        "textures": {"layer0": f"forbidden:item/{item}"},
    })
    write(OUT / "items" / f"{item}.json", {"model": {"type": "minecraft:model", "model": f"forbidden:item/{item}"}})

for item, (base, overlay) in MORPH.items():
    copy(FM / "textures/items" / f"{base}.png", OUT / "textures/item" / f"{item}.png")
    copy(FM / "textures/items" / f"{overlay}.png", OUT / "textures/item" / f"{item}_overlay.png")
    write(OUT / "models/item" / f"{item}.json", {
        "parent": "minecraft:item/handheld",
        "textures": {"layer0": f"forbidden:item/{item}", "layer1": f"forbidden:item/{item}_overlay"},
    })
    write(OUT / "items" / f"{item}.json", {"model": {
        "type": "minecraft:model",
        "model": f"forbidden:item/{item}",
        "tints": [
            {"type": "minecraft:constant", "value": -1},
            {"type": "minecraft:custom_model_data", "index": 0, "default": 0x980000},
        ],
    }})

# wand rod model textures used by Thaumaturge's wand renderer
for rod in ["tainted", "infernal", "profane", "profaned"]:
    copy(FM / "textures/models" / f"wand_rod_{rod}.png", OUT / "textures/models" / f"wand_rod_{rod}.png")

# aspect icons
for aspect in ["infernus", "ira", "invidia", "gula", "superbia", "luxuria", "desidia"]:
    copy(FM / "textures/aspects" / f"{aspect}.png", OUT / "textures/aspects" / f"{aspect}.png")

# research icons and backgrounds
for misc in ["forbidden", "runecircle", "consuming", "corrupting", "educational", "greedy", "hellfire", "impact",
             "lucrative", "pandemonium", "voidtouched", "wrath"]:
    copy(FM / "textures/misc" / f"{misc}.png", OUT / "textures/research" / f"{misc}.png")

print("assets generated")

# ---------------------------------------------------------------------------------------------------------------------
# Blocks
# ---------------------------------------------------------------------------------------------------------------------
BLOCK_TEX = {
    "taint_log_bottom": "taint_log_bottom", "taint_log_side": "taint_log_side", "taint_log_side_gross": "taint_log_side_gross",
    "taint_log_side_gross2": "taint_log_side_gross2", "taint_planks": "taint_planks", "taint_leaves": "taint_leaves",
    "taint_sapling": "sapling_taint", "taint_stone": "taint_stone_smooth", "taint_stone_bricks": "taint_stone_brick",
    "nether_star_block": "starblock", "umbral_rose": "flower_black", "umbral_bush_top": "umbral_rose_top",
    "umbral_bush_bottom": "umbral_rose_bottom", "arcane_cake_top": "cake_top", "arcane_cake_side": "cake_side",
    "arcane_cake_inner": "cake_inner", "arcane_cake_bottom": "cake_bottom",
}
for name, tex in BLOCK_TEX.items():
    copy(FM / "textures/blocks" / f"{tex}.png", OUT / "textures/block" / f"{name}.png")

def block_model(name, parent, textures):
    write(OUT / "models/block" / f"{name}.json", {"parent": parent, "textures": textures})

def simple_state(name, model=None):
    write(OUT / "blockstates" / f"{name}.json", {"variants": {"": {"model": f"forbidden:block/{model or name}"}}})

def block_item(name, model=None):
    write(OUT / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": model or f"forbidden:block/{name}"}})

def flat_item(name, texture):
    write(OUT / "models/item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture}})
    block_item(name, f"forbidden:item/{name}")

for cube in ["taint_planks", "taint_stone", "taint_stone_bricks", "nether_star_block"]:
    block_model(cube, "minecraft:block/cube_all", {"all": f"forbidden:block/{cube}"})
    simple_state(cube)
    block_item(cube)

block_model("taint_leaves", "minecraft:block/leaves", {"all": "forbidden:block/taint_leaves"})
simple_state("taint_leaves")
block_item("taint_leaves")

block_model("taint_sapling", "minecraft:block/cross", {"cross": "forbidden:block/taint_sapling"})
simple_state("taint_sapling")
flat_item("taint_sapling", "forbidden:block/taint_sapling")

block_model("umbral_rose", "minecraft:block/cross", {"cross": "forbidden:block/umbral_rose"})
simple_state("umbral_rose")
flat_item("umbral_rose", "forbidden:block/umbral_rose")

for half in ["top", "bottom"]:
    block_model(f"umbral_bush_{half}", "minecraft:block/cross", {"cross": f"forbidden:block/umbral_bush_{half}"})
write(OUT / "blockstates/umbral_bush.json", {"variants": {
    "half=lower": {"model": "forbidden:block/umbral_bush_bottom"}, "half=upper": {"model": "forbidden:block/umbral_bush_top"}}})
flat_item("umbral_bush", "forbidden:block/umbral_bush_top")

# Tainted log: FM randomly shows "gross" side textures on 25% of positions; weighted blockstate variants reproduce it.
for suffix, side in [("", "taint_log_side"), ("_gross", "taint_log_side_gross"), ("_gross2", "taint_log_side_gross2")]:
    block_model(f"taint_log{suffix}", "minecraft:block/cube_column", {"end": "forbidden:block/taint_log_bottom", "side": f"forbidden:block/{side}"})
    block_model(f"taint_log{suffix}_horizontal", "minecraft:block/cube_column_horizontal",
                {"end": "forbidden:block/taint_log_bottom", "side": f"forbidden:block/{side}"})
def log_variants(rot):
    out = []
    for suffix, weight in [("", 75), ("_gross", 12), ("_gross2", 13)]:
        horizontal = "_horizontal" if rot else ""
        v = {"model": f"forbidden:block/taint_log{suffix}{horizontal}", "weight": weight}
        v.update(rot)
        out.append(v)
    return out
write(OUT / "blockstates/taint_log.json", {"variants": {
    "axis=y": log_variants({}), "axis=z": log_variants({"x": 90}), "axis=x": log_variants({"x": 90, "y": 90})}})
block_item("taint_log")

# Thaumic cake: 12 even slices across the 14px top (FM used (1 + eaten) / 12, which leaves the last slice zero-width).
for eaten in range(12):
    x0 = round(1 + 14 * eaten / 12, 4)
    faces = {
        "down": {"texture": "#bottom", "uv": [x0, 1, 15, 15], "cullface": "down"},
        "up": {"texture": "#top", "uv": [x0, 1, 15, 15]},
        "north": {"texture": "#side", "uv": [1, 8, 16 - x0, 16]},
        "south": {"texture": "#side", "uv": [x0, 8, 15, 16]},
        "west": {"texture": "#inner" if eaten else "#side", "uv": [1, 8, 15, 16]},
        "east": {"texture": "#side", "uv": [1, 8, 15, 16]},
    }
    write(OUT / "models/block" / f"arcane_cake_slice{eaten}.json", {
        "textures": {"particle": "forbidden:block/arcane_cake_side", "bottom": "forbidden:block/arcane_cake_bottom",
                     "top": "forbidden:block/arcane_cake_top", "side": "forbidden:block/arcane_cake_side",
                     "inner": "forbidden:block/arcane_cake_inner"},
        "elements": [{"from": [x0, 0, 1], "to": [15, 8, 15], "faces": faces}]})
write(OUT / "blockstates/arcane_cake.json", {"variants": {f"eaten={i}": {"model": f"forbidden:block/arcane_cake_slice{i}"} for i in range(12)}})
copy(FM / "textures/items/cake.png", OUT / "textures/item/arcane_cake.png")
flat_item("arcane_cake", "forbidden:item/arcane_cake")
# Wrath Cage (FM texture "spirit_box", see-through)
copy(FM / "textures/blocks/spirit_box.png", OUT / "textures/block/wrath_cage.png")
write(OUT / "models/block/wrath_cage.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
                                             "textures": {"all": "forbidden:block/wrath_cage"}})
simple_state("wrath_cage")
block_item("wrath_cage")

# Imprinting crystal: blank / imprinted textures, switched on the forbidden:mob component
copy(FM / "textures/items/emptycrystal.png", OUT / "textures/item/mob_crystal_empty.png")
copy(FM / "textures/items/mobcrystal.png", OUT / "textures/item/mob_crystal.png")
for name in ["mob_crystal", "mob_crystal_empty"]:
    write(OUT / "models/item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"forbidden:item/{name}"}})
write(OUT / "items/mob_crystal.json", {"model": {"type": "minecraft:condition", "property": "minecraft:has_component", "component": "forbidden:mob",
    "on_true": {"type": "minecraft:model", "model": "forbidden:item/mob_crystal"},
    "on_false": {"type": "minecraft:model", "model": "forbidden:item/mob_crystal_empty"}}})
# Blink upgrade spell part icons and the Heretic villager
for icon in ["hellfire", "pandemonium"]:
    copy(FM / "textures/misc" / f"{icon}.png", OUT / "textures/foci" / f"{icon}.png")
copy(FM / "textures/models/heretic.png", OUT / "textures/entity/villager/profession/heretic.png")
print("block assets generated")
