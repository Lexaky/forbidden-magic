"""Generates the datapack content and language files of the Forbidden Magic port.

Everything Thaumaturge models as data (aspects, item/entity aspect maps, research, recipes) plus FM's enchantments,
tags and lang files is produced here from tables that mirror the original mod (fox.spiteful.forbidden.*).

Usage:
    python -I tools/gen_data.py <thaumaturge-jar-extract> <aspect_index.json> <fm-lang-dir> <resources-dir>

* thaumaturge-jar-extract: directory with the unpacked Thaumaturge 1.1.0 jar (for its entity aspect data map)
* aspect_index.json: run-server/config/thaumaturge/aspect_index.json written by Thaumaturge on server start
  (effective aspects of vanilla items, used when FM adds an aspect to them)
* fm-lang-dir: assets/forbidden/lang of the original Forbidden Magic (WTFPL), source of research texts
* resources-dir: src/main/resources of this project
"""
import json
import re
import shutil
import sys
from pathlib import Path

TT_JAR, INDEX, FM_LANG, RES = (Path(a) for a in sys.argv[1:5])
DATA = RES / "data"
ASSETS = RES / "assets/forbidden"
NS = "forbidden"
TT = "thaumaturge"

# Remove previously generated data so renamed entries do not linger.
for sub in ["forbidden/thaumaturge", "forbidden/recipe", "forbidden/enchantment", "forbidden/tags", "forbidden/loot_table", "forbidden/worldgen",
            "forbidden/villager_trade", "forbidden/trade_set"]:
    shutil.rmtree(DATA / sub, ignore_errors=True)


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def asp(name: str) -> str:
    return name if ":" in name else (f"{NS}:{name}" if name in FM_ASPECTS else f"{TT}:{name}")


def alist(pairs):
    return [{"aspect": asp(a), "amount": n} for a, n in pairs]


# --------------------------------------------------------------------------------------------------------------------
# Aspects (DarkAspects). TC4 components mapped to Thaumaturge: telum->aversio, fames->desiderium, corpus->bestia.
# --------------------------------------------------------------------------------------------------------------------
FM_ASPECTS = {
    "infernus": (0xFF0000, ["ignis", "praecantatio"], "4", 771),
    "ira": (0x870404, ["aversio", "ignis"], "4", 771),
    "invidia": (0x00BA00, ["sensus", "desiderium"], "2", 1),
    "gula": (0xD59C46, ["desiderium", "vacuos"], "6", 1),
    "superbia": (0x9639FF, ["volatus", "vacuos"], "5", 1),
    "luxuria": (0xFFC1CE, ["bestia", "desiderium"], "d", 1),
    "desidia": (0x6E6E6E, ["vinculum", "spiritus"], "8", 771),
}
for tag, (color, comps, chat, blend) in FM_ASPECTS.items():
    data = {"tag": tag, "color": color, "components": [f"{TT}:{c}" for c in comps], "chat_color": chat,
            "texture": f"{NS}:textures/aspects/{tag}.png"}
    if blend != 1:
        data["blend"] = blend
    write(DATA / NS / TT / "aspect" / f"{tag}.json", data)

# --------------------------------------------------------------------------------------------------------------------
# Item aspects. FM's TC4 amounts are doubled to sit closer to Thaumaturge's scale (TC6-like, ~2-5x TC4).
# --------------------------------------------------------------------------------------------------------------------
SCALE = 2
FM_ITEM_ASPECTS = {
    "wrath_shard": [("infernus", 1), ("ira", 2), ("vitreus", 1)],
    "envy_shard": [("infernus", 1), ("invidia", 2), ("vitreus", 1)],
    "taint_shard": [("vitium", 3), ("vitreus", 1)],
    "pride_shard": [("infernus", 1), ("superbia", 2), ("vitreus", 1)],
    "lust_shard": [("infernus", 1), ("luxuria", 2), ("vitreus", 1)],
    "sloth_shard": [("infernus", 1), ("desidia", 2), ("vitreus", 1)],
    "greed_shard": [("infernus", 1), ("desiderium", 2), ("vitreus", 1)],
    "gluttony_shard": [("infernus", 1), ("gula", 2), ("vitreus", 1)],
    "emerald_nugget": [("vitreus", 1)],
    "black_ink": [("tenebrae", 1)],
    "riding_crop": [("herba", 2), ("fabrico", 2), ("bestia", 1), ("luxuria", 1), ("aversio", 1)],
    "taint_fruit": [("vitium", 2), ("mortuus", 1)],
    "taint_coal": [("vitium", 2), ("perditio", 2)],
    "umbral_rose": [("herba", 1), ("victus", 1), ("sensus", 1), ("tenebrae", 1)],
    "taint_leaves": [("vitium", 1), ("herba", 1)],
    "taint_log": [("vitium", 1), ("herba", 3)],
    "taint_planks": [("herba", 1)],
    "taint_stone_bricks": [("terra", 1)],
}
# Aspects FM adds on top of an item's existing ones.
VANILLA_ADDITIONS = {
    "minecraft:netherrack": [("infernus", 1)],
    "minecraft:nether_quartz_ore": [("infernus", 2)],
    "minecraft:nether_star": [("infernus", 8), ("superbia", 8)],
    "minecraft:nether_wart": [("infernus", 1)],
    "minecraft:nether_brick": [("infernus", 1)],
    "minecraft:nether_bricks": [("infernus", 2)],
    "minecraft:ghast_tear": [("ira", 4)],
    "minecraft:wither_skeleton_skull": [("infernus", 4)],
    "minecraft:creeper_head": [("ira", 2)],
    "minecraft:tnt": [("ira", 2)],
    "minecraft:golden_helmet": [("superbia", 2)],
    "minecraft:golden_chestplate": [("superbia", 1)],
    "minecraft:golden_leggings": [("superbia", 1)],
    "minecraft:golden_boots": [("superbia", 1)],
    "minecraft:golden_sword": [("superbia", 2)],
    "minecraft:ender_eye": [("invidia", 4)],
    "minecraft:comparator": [("invidia", 2)],
    "minecraft:cake": [("gula", 7)],
    "minecraft:cookie": [("gula", 1)],
    "minecraft:fire_charge": [("ira", 1)],
    "minecraft:saddle": [("luxuria", 2)],
    "minecraft:lead": [("luxuria", 2)],
}
BEDS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple",
        "blue", "brown", "green", "red", "black"]
for color in BEDS:
    VANILLA_ADDITIONS[f"minecraft:{color}_bed"] = [("desidia", 4)]

index = json.loads(INDEX.read_text(encoding="utf-8"))["aspects"]


def merged(existing, additions):
    # The index may already contain this addon's aspects from an earlier run; drop them so regeneration is idempotent.
    out = {e["aspect"]: e.get("amount", 1) for e in existing if not e["aspect"].startswith(f"{NS}:")}
    for a, n in additions:
        out[asp(a)] = out.get(asp(a), 0) + n * SCALE
    return [{"aspect": k, "amount": v} for k, v in out.items()]


base = {f"{NS}:{item}": [{"aspect": asp(a), "amount": n * SCALE} for a, n in pairs] for item, pairs in FM_ITEM_ASPECTS.items()}
for item, additions in VANILLA_ADDITIONS.items():
    base[item] = merged(index.get(item, []), additions)
write(DATA / TT / "data_maps/item/base_aspects.json", {"values": base})

# Entity aspects (DarkAspects#addAspects scan tags), on top of Thaumaturge's own values.
tt_entities = json.loads((TT_JAR / "data/thaumaturge/data_maps/entity_type/entity_aspects.json").read_text(encoding="utf-8"))["values"]
ENTITY_ADDITIONS = {
    "thaumaturge:brainy_zombie": [("ira", 3)],
    "thaumaturge:giant_brainy_zombie": [("ira", 4)],
    "thaumaturge:firebat": [("ira", 1), ("infernus", 2)],
    "minecraft:wither_skeleton": [("infernus", 3)],
    "minecraft:creeper": [("ira", 2)],
    "minecraft:wither": [("infernus", 7), ("ira", 7)],
    "minecraft:ocelot": [("desidia", 3)],
    "minecraft:enderman": [("superbia", 4), ("invidia", 4)],
    "minecraft:ghast": [("infernus", 3), ("ira", 3)],
    "minecraft:zombified_piglin": [("ira", 6)],
    "minecraft:ender_dragon": [("superbia", 10)],
    "thaumaturge:taintacle": [("luxuria", 3)],
    "minecraft:pig": [("gula", 3)],
    "minecraft:magma_cube": [("infernus", 1)],
}
entities = {}
for entity, additions in ENTITY_ADDITIONS.items():
    existing = tt_entities.get(entity, [])
    if isinstance(existing, dict):
        existing = existing.get("value", [])
    entities[entity] = merged(existing, additions)
write(DATA / TT / "data_maps/entity_type/entity_aspects.json", {"values": entities})

# --------------------------------------------------------------------------------------------------------------------
# Recipes
# --------------------------------------------------------------------------------------------------------------------
def crystal(aspect):
    return {"neoforge:ingredient_type": "neoforge:components", "components": {f"{TT}:crystal_aspect": {"aspect": f"{TT}:{aspect}"}},
            "items": f"{TT}:essentia_crystal"}


def research(entry):
    return {"entry": f"{NS}:{entry}"}


RECIPES = {}


def infusion(name, entry, result, catalyst, instability, aspects, components):
    RECIPES[f"infusion/{name}"] = {"type": f"{TT}:infusion", "aspects": alist(aspects), "catalyst": catalyst,
                                   "components": components, "instability": instability, "research": research(entry),
                                   "result": result if isinstance(result, dict) else {"id": result}}


def crucible(name, entry, result, catalyst, aspects, count=1):
    res = {"id": result}
    if count > 1:
        res["count"] = count
    RECIPES[f"crucible/{name}"] = {"type": f"{TT}:crucible", "aspects": alist(aspects), "catalyst": catalyst,
                                   "research": research(entry), "result": res}


def book(enchantment, level=1):
    return {"id": "minecraft:enchanted_book", "components": {"minecraft:stored_enchantments": {f"{NS}:{enchantment}": level}}}


SALIS = f"{TT}:salis_mundus"
SHARD = lambda s: f"{NS}:{s}_shard"

infusion("taint_shovel", "taint_shovel", f"{NS}:taint_shovel", f"{TT}:thaumium_shovel", 5,
         [("victus", 8), ("vitreus", 24), ("instrumentum", 12)],
         [crystal("ordo"), crystal("ordo"), "minecraft:diamond", f"#{TT}:silverwood_logs"])
infusion("taint_pickaxe", "taint_pickaxe", f"{NS}:taint_pickaxe", f"{TT}:thaumium_pickaxe", 1,
         [("perditio", 8), ("instrumentum", 8), ("vitium", 12)],
         [crystal("perditio"), crystal("perditio"), "minecraft:diamond", f"#{TT}:greatwood_logs"])
infusion("wand_rod_tainted", "rod_tainted", f"{NS}:wand_rod_tainted", f"{TT}:taint_tendril", 5,
         [("vitium", 24), ("praecantatio", 12), ("perditio", 12)],
         [SALIS] + [SHARD("taint")] * 6)
infusion("skull_axe", "skull_axe", f"{NS}:skull_axe", f"{TT}:thaumium_axe", 1,
         [("ira", 8), ("aversio", 8), ("infernus", 8)],
         [SHARD("wrath"), SHARD("wrath"), "minecraft:diamond", "minecraft:wither_skeleton_skull"])
for tool, base_tool, third in [("pickaxe", "pickaxe", "instrumentum"), ("sword", "sword", "aversio"),
                               ("shovel", "shovel", "instrumentum"), ("axe", "axe", "instrumentum")]:
    infusion(f"morph_{tool}", "morph_tools", f"{NS}:morph_{tool}", f"{TT}:thaumium_{base_tool}", 6,
             [("permutatio", 32), ("invidia", 16), (third, 16)],
             [SHARD("envy"), SHARD("envy"), "minecraft:diamond", f"{TT}:quicksilver", f"#{TT}:silverwood_logs"])
infusion("wand_rod_infernal", "rod_infernal", f"{NS}:wand_rod_infernal", "minecraft:blaze_rod", 5,
         [("infernus", 32), ("praecantatio", 12), ("superbia", 12)],
         [SALIS, SHARD("pride"), SHARD("pride"), "minecraft:soul_sand", "minecraft:wither_skeleton_skull", "minecraft:quartz",
          "minecraft:blaze_powder"])
infusion("diabolist_fork", "diabolist_fork", f"{NS}:diabolist_fork", f"{TT}:thaumium_sword", 1,
         [("infernus", 8), ("machina", 8), ("potentia", 8)],
         ["minecraft:quartz", "minecraft:quartz", "minecraft:quartz", "minecraft:redstone"])

# Infusion enchanting (TC4 addInfusionEnchantmentRecipe). Thaumaturge's infusion enchantments are a closed enum, so FM's
# enchantments are infused into a book that is then applied on an anvil.
infusion("wrath_book", "wrath", book("wrath"), "minecraft:book", 8, [("ira", 16), ("aversio", 16), ("infernus", 8)],
         [SALIS, "minecraft:diamond_sword", SHARD("wrath"), SHARD("wrath"), SHARD("wrath")])
infusion("capitalist_book", "capitalist", book("capitalist"), "minecraft:book", 4, [("infernus", 16), ("aversio", 8), ("desiderium", 16)],
         ["minecraft:golden_sword", "minecraft:diamond", SHARD("greed"), SHARD("greed"), SALIS])
infusion("consuming_book", "consuming", book("consuming"), "minecraft:book", 3, [("vacuos", 8), ("instrumentum", 8), ("desiderium", 8)],
         ["minecraft:iron_pickaxe", "minecraft:lava_bucket", SALIS])
infusion("educational_book", "educational", book("educational"), "minecraft:book", 3, [("praecantatio", 4), ("aversio", 4), ("cognitio", 8)],
         [f"{TT}:brain", "minecraft:book", SALIS])
infusion("corrupting_book", "corrupting", book("corrupting"), "minecraft:book", 4, [("infernus", 16), ("permutatio", 16), ("vitreus", 8)],
         ["minecraft:nether_wart", "minecraft:soul_sand", SALIS])
infusion("impact_book", "impact", book("impact"), "minecraft:book", 4, [("perditio", 16), ("instrumentum", 16)],
         [SALIS, SHARD("envy"), SHARD("envy"), f"{TT}:elemental_shovel"])
infusion("voidtouched_book", "voidtouched", book("voidtouched"), "minecraft:book", 8,
         [("vacuos", 16), ("tenebrae", 16), ("invidia", 24), ("alienis", 16)],
         [SALIS, f"{TT}:ingot_void", f"{TT}:ingot_void", f"{TT}:ingot_void", SHARD("envy"), SHARD("envy"), SHARD("envy")])

infusion("fiery_core_book", "fiery_core", book("fiery_core"), "minecraft:book", 3, [("ignis", 4), ("metallum", 4), ("desiderium", 4)],
         [f"{TT}:elemental_pickaxe", SALIS])
crucible("emerald_transmutation", "emerald_transmutation", f"{NS}:emerald_nugget", "#c:nuggets/emerald",
         [("vitreus", 2), ("desiderium", 2)], count=4)

RECIPES["emerald_from_nuggets"] = {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["###", "###", "###"],
                                   "key": {"#": f"{NS}:emerald_nugget"}, "result": {"id": "minecraft:emerald"}}
RECIPES["emerald_nugget"] = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ["minecraft:emerald"],
                             "result": {"id": f"{NS}:emerald_nugget", "count": 9}}
RECIPES["riding_crop"] = {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["X", "#", "#"],
                          "key": {"X": "minecraft:leather", "#": "minecraft:stick"}, "result": {"id": f"{NS}:riding_crop"}}
infusion("arcane_cake", "arcane_cake", f"{NS}:arcane_cake", "minecraft:cake", 3, [("gula", 12), ("desiderium", 24), ("fabrico", 24)],
         [SALIS, "minecraft:egg", "minecraft:milk_bucket", "minecraft:egg", SHARD("gluttony"), SHARD("gluttony")])
crucible("umbral_bush", "black_flower", f"{NS}:umbral_bush", "minecraft:rose_bush", [("tenebrae", 8), ("victus", 5)])
crucible("taint_sapling", "taint_tree", f"{NS}:taint_sapling", "minecraft:oak_sapling", [("vitium", 10), ("mortuus", 4)])
RECIPES["black_ink"] = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [f"{NS}:umbral_rose"],
                        "result": {"id": f"{NS}:black_ink", "count": 2}}
RECIPES["taint_planks"] = {"type": "minecraft:crafting_shapeless", "category": "building", "group": "planks",
                           "ingredients": [f"{NS}:taint_log"], "result": {"id": f"{NS}:taint_planks", "count": 4}}
RECIPES["taint_coal"] = {"type": "minecraft:smelting", "category": "misc", "cookingtime": 200, "experience": 0.15,
                         "ingredient": f"{NS}:taint_log", "result": {"id": f"{NS}:taint_coal"}}
RECIPES["arcane/taint_stone"] = {"type": f"{TT}:arcane_workbench_shaped", "crystals": [{"aspect": f"{TT}:perditio", "amount": 2},
                                 {"aspect": f"{TT}:ordo", "amount": 1}], "key": {"S": "minecraft:stone", "X": f"#{NS}:taint_resources"},
                                 "pattern": ["SSS", "SXS", "SSS"], "research": research("taint_stone"),
                                 "result": {"id": f"{NS}:taint_stone", "count": 9}, "vis": 10}
RECIPES["taint_stone_bricks"] = {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["##", "##"],
                                 "key": {"#": f"{NS}:taint_stone"}, "result": {"id": f"{NS}:taint_stone_bricks", "count": 4}}
RECIPES["nether_star_block"] = {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["###", "###", "###"],
                                "key": {"#": "minecraft:nether_star"}, "result": {"id": f"{NS}:nether_star_block"}}
RECIPES["nether_star_from_block"] = {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [f"{NS}:nether_star_block"],
                                     "result": {"id": "minecraft:nether_star", "count": 9}}
infusion("wrath_cage", "wrath_cage", f"{NS}:wrath_cage", f"{TT}:metal_thaumium", 10,
         [("ira", 32), ("praecantatio", 32), ("bestia", 32), ("machina", 16)],
         [SHARD("wrath")] * 4 + ["minecraft:diamond"] * 4 + [f"{TT}:jar_normal"] * 3)
crucible("mob_crystal", "wrath_cage", f"{NS}:mob_crystal", "minecraft:diamond", [("cognitio", 10), ("potentia", 10)])
infusion("sub_collar", "sub_collar", f"{NS}:sub_collar", f"{TT}:amulet_vis_crafted", 1,
         [("luxuria", 8), ("vinculum", 8), ("infernus", 4), ("bestia", 4)],
         [SHARD("lust")] * 3 + ["minecraft:lead", f"{TT}:girdle_mundane"])
# TC4 arcane cost Perditio 10 / Aqua 20 / Terra 20: total as vis, one crystal of each primal used.
RECIPES["arcane/ring_nutrition"] = {"type": f"{TT}:arcane_workbench_shaped", "crystals": [{"aspect": f"{TT}:perditio", "amount": 1},
                                    {"aspect": f"{TT}:aqua", "amount": 1}, {"aspect": f"{TT}:terra", "amount": 1}],
                                    "key": {"D": "minecraft:diamond", "S": SHARD("gluttony"), "X": f"{TT}:ring_mundane"},
                                    "pattern": [" D ", "SXS", " S "], "research": research("ring_nutrition"),
                                    "result": {"id": f"{NS}:ring_nutrition"}, "vis": 50}
# Crystalwell: inkwell + black dye + two shards (TC4 shards -> Thaumaturge essentia crystals of any aspect).
RECIPES["arcane/crystalwell"] = {"type": f"{TT}:arcane_workbench_shapeless", "crystals": [{"aspect": f"{TT}:aqua", "amount": 1},
                                 {"aspect": f"{TT}:ordo", "amount": 1}],
                                 "ingredients": [f"{TT}:scribing_tools", "#c:dyes/black", f"{TT}:essentia_crystal", f"{TT}:essentia_crystal"],
                                 "research": research("crystalwell"), "result": {"id": f"{NS}:crystalwell"}, "vis": 10}
# Primewell: TC4 cost 50 of every primal. Thaumaturge's primordial pearl survives crafting on its own (it loses one use).
RECIPES["arcane/primewell"] = {"type": f"{TT}:arcane_workbench_shapeless",
                               "crystals": [{"aspect": f"{TT}:{a}", "amount": 1} for a in ["aer", "terra", "ignis", "aqua", "ordo", "perditio"]],
                               "ingredients": ["minecraft:feather", f"{TT}:primordial_pearl", "minecraft:glass_bottle"],
                               "research": research("primewell"), "result": {"id": f"{NS}:primewell"}, "vis": 300}
for name, recipe in RECIPES.items():
    write(DATA / NS / "recipe" / f"{name}.json", recipe)

# Wrath Cage mobs (FM Config.spawnilify), TC4 aspects mapped: corpus->bestia, pannus->fabrico, limus->aqua, venenum->mortuus,
# lucrum->desiderium, eldritch->alienis, magic->praecantatio, aura->auram, taint->vitium, mind->cognitio.
CAGE_MOBS = {
    "minecraft:zombie": "bestia", "minecraft:skeleton": "mortuus", "minecraft:creeper": "ignis", "minecraft:horse": "bestia",
    "minecraft:pig": "bestia", "minecraft:sheep": "fabrico", "minecraft:cow": "bestia", "minecraft:mooshroom": "herba",
    "minecraft:ocelot": "bestia", "minecraft:chicken": "volatus", "minecraft:squid": "sensus", "minecraft:wolf": "bestia",
    "minecraft:bat": "volatus", "minecraft:spider": "fabrico", "minecraft:slime": "aqua", "minecraft:ghast": "infernus",
    "minecraft:zombified_piglin": "desiderium", "minecraft:enderman": "alienis", "minecraft:cave_spider": "mortuus",
    "minecraft:silverfish": "desiderium", "minecraft:blaze": "ignis", "minecraft:magma_cube": "ignis", "minecraft:witch": "praecantatio",
    "minecraft:villager": "desiderium", "thaumaturge:firebat": "ignis", "thaumaturge:wisp": "auram", "thaumaturge:thaumic_slime": "vitium",
    "thaumaturge:brainy_zombie": "cognitio", "thaumaturge:giant_brainy_zombie": "cognitio", "thaumaturge:taint_crawler": "vitium",
    "thaumaturge:taint_swarm": "vitium", "thaumaturge:cultist_knight": "alienis", "thaumaturge:cultist_cleric": "alienis",
    "thaumaturge:eldritch_crab": "alienis", "thaumaturge:inhabited_zombie": "alienis", "thaumaturge:pech": "desiderium",
    "thaumaturge:eldritch_guardian": "alienis",
}
write(DATA / NS / "data_maps/entity_type/wrath_cage_mobs.json", {"values": {
    mob: asp(a) for mob, a in CAGE_MOBS.items()}})

# --------------------------------------------------------------------------------------------------------------------
# Heretic villager (VillagerHereticManager). FM's random amounts become uniform number providers.
# --------------------------------------------------------------------------------------------------------------------
def uniform(lo, hi):
    return {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}


def trade(name, wants, gives, gives_count=None, extra=None, max_uses=12, xp=2):
    data = {"wants": wants, "gives": gives, "max_uses": float(max_uses), "reputation_discount": 0.05, "xp": float(xp)}
    if gives_count:
        data["given_item_modifiers"] = [{"function": "minecraft:set_count", "count": gives_count}]
    if extra:
        data["additional_wants"] = extra
    write(DATA / NS / "villager_trade/heretic" / f"{name}.json", data)
    return f"{NS}:heretic/{name}"


PROFANE_WAND = {"id": f"{TT}:wand", "components": {
    f"{TT}:wand_parts": {"cap": f"{TT}:iron", "rod": f"{NS}:profane"},
    f"{TT}:wand_vis": {f"{TT}:{a}": 5000 for a in ["aer", "terra", "ignis", "aqua", "ordo", "perditio"]}}}
HERETIC = {
    1: [trade("greed_shards", {"id": "minecraft:emerald", "count": uniform(1, 2)}, {"id": f"{NS}:greed_shard"}, uniform(3, 4)),
        trade("buy_lust_shards", {"id": f"{NS}:lust_shard", "count": uniform(2, 5)}, {"id": "minecraft:emerald"}),
        trade("buy_wrath_shards", {"id": f"{NS}:wrath_shard", "count": uniform(4, 9)}, {"id": "minecraft:emerald"}),
        trade("buy_gluttony_shards", {"id": f"{NS}:gluttony_shard", "count": uniform(4, 9)}, {"id": "minecraft:emerald"})],
    2: [trade("skeleton_skull", {"id": "minecraft:emerald", "count": uniform(1, 3)}, {"id": "minecraft:skeleton_skull"}, xp=5),
        trade("ghast_tear", {"id": "minecraft:emerald", "count": uniform(4, 7)}, {"id": "minecraft:ghast_tear"}, xp=5),
        trade("glowstone", {"id": "minecraft:emerald"}, {"id": "minecraft:glowstone_dust"}, uniform(8, 16), xp=5)],
    # FM lowered the profane wand's uses by 6 (one sale).
    3: [trade("profane_wand", {"id": "minecraft:emerald", "count": uniform(8, 12)}, PROFANE_WAND, extra={"id": "minecraft:stick"}, max_uses=1, xp=10)],
}
for level, trades in HERETIC.items():
    write(DATA / NS / "tags/villager_trade/heretic" / f"level_{level}.json", {"values": trades})
    write(DATA / NS / "trade_set/heretic" / f"level_{level}.json", {"amount": float(min(2, len(trades))), "random_sequence": f"{NS}:trade_set/heretic/level_{level}",
                                                                  "trades": f"#{NS}:heretic/level_{level}"})
write(DATA / "minecraft/tags/point_of_interest_type/acquirable_job_site.json", {"replace": False, "values": [f"{NS}:heretic"]})

# Blink upgrades as spell parts (ItemFocusBlink hellfire / pandemonium upgrades)
for part, aspect, color in [("hellfire", "infernus", 0xFF4400), ("pandemonium", "desidia", 0x6E6E6E)]:
    write(DATA / NS / TT / "spell_part" / f"{part}.json", {"aspect": asp(aspect), "behavior": {"type": f"{NS}:{part}"}, "color": color,
                                                           "complexity": 5, "icon": f"{NS}:textures/foci/{part}.png", "max_per_spell": 1,
                                                           "research": research("blink_upgrades")})

# --------------------------------------------------------------------------------------------------------------------
# Blocks: loot tables and the tainted tree (WorldGenTaintedTree: oak-shaped, 4-6 tall)
# --------------------------------------------------------------------------------------------------------------------
def self_drop(block, extra_conditions=None):
    entry = {"type": "minecraft:item", "name": f"{NS}:{block}"}
    if extra_conditions:
        entry["conditions"] = extra_conditions
    return {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [entry]}], "random_sequence": f"{NS}:blocks/{block}"}


for block in ["wrath_cage", "taint_log", "taint_planks", "taint_sapling", "taint_stone", "taint_stone_bricks", "nether_star_block", "umbral_rose"]:
    write(DATA / NS / "loot_table/blocks" / f"{block}.json", self_drop(block))
write(DATA / NS / "loot_table/blocks/umbral_bush.json", self_drop("umbral_bush", [
    {"condition": "minecraft:block_state_property", "block": f"{NS}:umbral_bush", "properties": {"half": "lower"}}]))
SHEARS_OR_SILK = {"condition": "minecraft:any_of", "terms": [
    {"condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}},
    {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}]}
write(DATA / NS / "loot_table/blocks/taint_leaves.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/taint_leaves", "pools": [
    {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": f"{NS}:taint_leaves", "conditions": [SHEARS_OR_SILK]},
        {"type": "minecraft:item", "name": f"{NS}:taint_sapling", "conditions": [{"condition": "minecraft:survives_explosion"},
            {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [1 / 60, 1 / 45, 1 / 30, 1 / 20]}]}]}]},
    {"rolls": 1.0, "bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:inverted", "term": SHEARS_OR_SILK}],
     "entries": [{"type": "minecraft:item", "name": f"{NS}:taint_fruit", "conditions": [{"condition": "minecraft:survives_explosion"},
         {"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune", "chances": [0.05, 0.0625, 0.083333336, 0.1]}]}]}]})
write(DATA / NS / "worldgen/configured_feature/tainted_tree.json", {"type": "minecraft:tree", "config": {
    "below_trunk_provider": {"type": "minecraft:rule_based_state_provider", "rules": [{"if_true": {"type": "minecraft:not", "predicate": {
        "type": "minecraft:matching_block_tag", "tag": "minecraft:cannot_replace_below_tree_trunk"}},
        "then": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:dirt"}}}]},
    "decorators": [],
    "foliage_placer": {"type": "minecraft:blob_foliage_placer", "height": 3, "offset": 0, "radius": 2},
    "foliage_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:taint_leaves",
        "Properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}}},
    "ignore_vines": True,
    "minimum_size": {"type": "minecraft:two_layers_feature_size", "limit": 1, "lower_size": 0, "upper_size": 1},
    "trunk_placer": {"type": "minecraft:straight_trunk_placer", "base_height": 4, "height_rand_a": 2, "height_rand_b": 0},
    "trunk_provider": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:taint_log", "Properties": {"axis": "y"}}}}})

# --------------------------------------------------------------------------------------------------------------------
# Enchantments (DarkEnchantments). Never offered by the enchanting table: they are not in #in_enchanting_table.
# Only Wrath's effect is expressible in data; the others are implemented in fox.spiteful.forbidden.event.
# --------------------------------------------------------------------------------------------------------------------
def enchantment(name, max_level, supported, exclusive=None, effects=None, min_base=20, anvil=4, weight=1, primary=None):
    data = {"anvil_cost": anvil, "description": {"translate": f"enchantment.{NS}.{name}"}, "effects": effects or {},
            "max_cost": {"base": min_base + 50, "per_level_above_first": 11}, "max_level": max_level,
            "min_cost": {"base": min_base, "per_level_above_first": 11}, "slots": ["mainhand"],
            "supported_items": supported, "weight": weight}
    if primary:
        data["primary_items"] = primary
    if exclusive:
        data["exclusive_set"] = exclusive
    write(DATA / NS / "enchantment" / f"{name}.json", data)


enchantment("wrath", 5, "#minecraft:enchantable/sharp_weapon", min_base=5, anvil=2, weight=1, effects={
    "minecraft:damage": [{"effect": {"type": "minecraft:add", "value": {"type": "minecraft:linear", "base": 1.25, "per_level_above_first": 1.25}}}]})
enchantment("capitalist", 1, "#minecraft:enchantable/weapon", exclusive=["minecraft:looting", f"{NS}:educational"])
enchantment("educational", 5, "#minecraft:enchantable/weapon", exclusive=["minecraft:looting", f"{NS}:capitalist"], min_base=5)
enchantment("consuming", 1, f"#{NS}:enchantable/consuming", exclusive=["minecraft:silk_touch"], min_base=5)
enchantment("corrupting", 1, f"#{NS}:enchantable/corrupting")
enchantment("impact", 1, f"#{NS}:enchantable/impact", min_base=30)
enchantment("fiery_core", 4, f"#{NS}:enchantable/fiery_core", exclusive=["minecraft:fortune"], min_base=5)
enchantment("voidtouched", 1, f"#{NS}:enchantable/morphic", min_base=30)

# --------------------------------------------------------------------------------------------------------------------
# Tags
# --------------------------------------------------------------------------------------------------------------------
def tag(path, values):
    write(DATA / path, {"replace": False, "values": values})


MORPH = [f"{NS}:morph_{t}" for t in ["pickaxe", "sword", "shovel", "axe"]]
tag(f"{NS}/tags/item/repairs/morphic.json", [f"{NS}:envy_shard"])
tag(f"{NS}/tags/item/consuming_garbage.json", ["minecraft:dirt", "#c:sands", "minecraft:gravel", "#c:cobblestones", "minecraft:netherrack"])
tag(f"{NS}/tags/item/enchantable/consuming.json", ["#minecraft:pickaxes", "#minecraft:shovels", {"id": f"{TT}:primal_crusher", "required": False}])
tag(f"{NS}/tags/item/enchantable/corrupting.json", ["#minecraft:pickaxes", {"id": f"{TT}:primal_crusher", "required": False}])
tag(f"{NS}/tags/item/enchantable/impact.json", [f"{NS}:morph_pickaxe", f"{NS}:morph_shovel"])
tag(f"{NS}/tags/item/enchantable/morphic.json", MORPH)
tag(f"{NS}/tags/item/enchantable/fiery_core.json", [f"{NS}:morph_pickaxe"])
tag("minecraft/tags/item/swords.json", [f"{NS}:skull_axe", f"{NS}:diabolist_fork", f"{NS}:morph_sword", f"{NS}:riding_crop"])
tag("minecraft/tags/item/pickaxes.json", [f"{NS}:taint_pickaxe", f"{NS}:morph_pickaxe"])
tag("minecraft/tags/item/shovels.json", [f"{NS}:taint_shovel", f"{NS}:morph_shovel"])
tag("minecraft/tags/item/axes.json", [f"{NS}:morph_axe"])
tag("c/tags/item/nuggets/emerald.json", [f"{NS}:emerald_nugget"])
tag("c/tags/item/nuggets.json", [f"{NS}:emerald_nugget"])
tag("c/tags/item/dyes/black.json", [f"{NS}:black_ink"])
tag("c/tags/item/dyes.json", [f"{NS}:black_ink"])
tag(f"{TT}/tags/item/taint_tools.json", [f"{NS}:taint_shovel"])
tag("curios/tags/item/ring.json", [f"{NS}:ring_nutrition"])
tag(f"{TT}/tags/item/scribing_tools.json", [f"{NS}:crystalwell", f"{NS}:primewell"])
tag("curios/tags/item/necklace.json", [f"{NS}:sub_collar"])
tag(f"{TT}/tags/item/runic_shieldable.json", [f"{NS}:ring_nutrition", f"{NS}:sub_collar"])
tag(f"{NS}/tags/item/taint_resources.json", [f"{NS}:taint_shard", {"id": f"{TT}:tainted_goo", "required": False}])
for kind in ["block", "item"]:
    tag(f"minecraft/tags/{kind}/logs.json", [f"{NS}:taint_log"])
    tag(f"minecraft/tags/{kind}/planks.json", [f"{NS}:taint_planks"])
    tag(f"minecraft/tags/{kind}/leaves.json", [f"{NS}:taint_leaves"])
    tag(f"minecraft/tags/{kind}/saplings.json", [f"{NS}:taint_sapling"])
    tag(f"minecraft/tags/{kind}/small_flowers.json", [f"{NS}:umbral_rose"])
    tag(f"c/tags/{kind}/storage_blocks/nether_star.json", [f"{NS}:nether_star_block"])
    tag(f"c/tags/{kind}/storage_blocks.json", [f"{NS}:nether_star_block"])
tag("minecraft/tags/block/mineable/axe.json", [f"{NS}:taint_log", f"{NS}:taint_planks"])
tag("minecraft/tags/block/mineable/hoe.json", [f"{NS}:taint_leaves"])
tag("minecraft/tags/block/mineable/pickaxe.json", [f"{NS}:wrath_cage", f"{NS}:taint_stone", f"{NS}:taint_stone_bricks", f"{NS}:nether_star_block"])
tag("minecraft/tags/block/beacon_base_blocks.json", [f"{NS}:nether_star_block"])
tag(f"{TT}/tags/item/wand_rods.json", [f"{NS}:wand_rod_tainted", f"{NS}:wand_rod_infernal", f"{NS}:wand_rod_profane"])

# --------------------------------------------------------------------------------------------------------------------
# Research (ForbiddenResearch). FM column/row are shifted (+5, +3) to centre the tree. TC4 research aspects become the
# note aspects of the Thaumaturge research note; the TC4 complexity (0-4) is clamped to Thaumaturge's 1-3.
# --------------------------------------------------------------------------------------------------------------------
CATEGORY = f"{NS}:{NS}"
write(DATA / NS / TT / "research_category" / f"{NS}.json", {
    "background": f"{NS}:textures/research/runecircle.png",
    "formula": alist([("infernus", 20), ("ira", 10), ("invidia", 10), ("superbia", 10), ("desidia", 5), ("gula", 5), ("vitium", 5)]),
    "icon": f"{NS}:textures/research/forbidden.png",
    "index": 10,
})

# key: (FM key, column, row, complexity, note aspects, parents, meta, icon, recipes, warp)
RESEARCH = {
    "nether_shards": ("NETHERSHARDS", -8, -5, 0, [], [], ["autounlock", "round"], f"{NS}:wrath_shard", [], 0),
    "rod_profane": ("ROD_profane", -9, -8, 0, [], [f"{NS}:nether_shards"], ["hidden", "round"], f"{NS}:wand_rod_profane", [], 0),
    "skull_axe": ("SKULLAXE", -11, -7, 2, [("aversio", 3), ("ira", 4), ("infernus", 1)],
                  [f"{NS}:nether_shards", f"{TT}:metallurgy", f"{TT}:infusion"], [], f"{NS}:skull_axe", ["infusion/skull_axe"], 1),
    "morph_tools": ("MORPHTOOLS", -9, -3, 3, [("instrumentum", 2), ("invidia", 3), ("permutatio", 2)],
                    [f"{NS}:nether_shards", f"~{TT}:metallurgy", f"~{TT}:infusion_enchantment"], [], f"{NS}:morph_pickaxe",
                    ["infusion/morph_pickaxe", "infusion/morph_sword", "infusion/morph_shovel", "infusion/morph_axe"], 4),
    "rod_infernal": ("ROD_infernal", -11, -4, 3, [("infernus", 4), ("ignis", 3), ("instrumentum", 1)],
                     [f"{TT}:rod_silverwood", f"{TT}:infusion", f"{NS}:nether_shards"], [], f"{NS}:wand_rod_infernal",
                     ["infusion/wand_rod_infernal"], 2),
    "wrath": ("WRATH", -6, -3, 3, [("ira", 16), ("aversio", 20), ("praecantatio", 10)],
              [f"{NS}:nether_shards", f"{TT}:infusion_enchantment"], [], f"{NS}:textures/research/wrath.png", ["infusion/wrath_book"], 3),
    "capitalist": ("GREEDY", -6, -5, 2, [("praecantatio", 2), ("aversio", 1), ("desiderium", 3)],
                   [f"{NS}:nether_shards", f"{TT}:infusion_enchantment"], [], f"{NS}:textures/research/greedy.png", ["infusion/capitalist_book"], 0),
    "corrupting": ("CORRUPTING", -6, -4, 1, [("infernus", 5), ("vitreus", 2), ("permutatio", 1)],
                   [f"{NS}:nether_shards", f"{TT}:infusion_enchantment"], [], f"{NS}:textures/research/corrupting.png", ["infusion/corrupting_book"], 0),
    "impact": ("IMPACT", -9, -1, 3, [("perditio", 8), ("instrumentum", 10), ("invidia", 10)],
               [f"{NS}:morph_tools", f"{TT}:elemental_tools"], [], f"{NS}:textures/research/impact.png", ["infusion/impact_book"], 0),
    "fiery_core": ("CLUSTER", -10, -1, 3, [("metallum", 1), ("ignis", 4), ("invidia", 3)],
                   [f"{NS}:morph_tools", f"{TT}:elemental_tools"], [], f"{NS}:textures/research/lucrative.png", ["infusion/fiery_core_book"], 0),
    "voidtouched": ("VOIDTOUCHED", -8, -1, 3, [("alienis", 16), ("instrumentum", 10), ("fabrico", 8), ("invidia", 32)],
                    [f"{NS}:morph_tools", f"{TT}:base_eldritch"], [], f"{NS}:textures/research/voidtouched.png", ["infusion/voidtouched_book"], 3),
    "diabolist_fork": ("FORK", -6, -7, 1, [("infernus", 3), ("machina", 1), ("instrumentum", 1)],
                       [f"{TT}:infusion", f"{NS}:nether_shards", f"{TT}:metallurgy"], [], f"{NS}:diabolist_fork", ["infusion/diabolist_fork"], 0),
    "taint_shovel": ("TAINTSHOVEL", -8, 1, 2, [("vitreus", 3), ("vitium", 2), ("instrumentum", 1)],
                     [f"{TT}:metallurgy", f"{TT}:infusion", f"{TT}:ethereal_bloom"], [], f"{NS}:taint_shovel", ["infusion/taint_shovel"], 0),
    "taint_pickaxe": ("TAINTPICK", -10, 1, 2, [("instrumentum", 2), ("vitium", 4), ("perditio", 3)],
                      [f"{TT}:metallurgy", f"{TT}:infusion"], [], f"{NS}:taint_pickaxe", ["infusion/taint_pickaxe"], 0),
    "rod_tainted": ("ROD_tainted", -8, 3, 3, [("praecantatio", 4), ("vitium", 5), ("instrumentum", 2)],
                    [f"{TT}:rod_silverwood", f"{NS}:taint_shovel", f"{TT}:infusion"], [], f"{NS}:wand_rod_tainted",
                    ["infusion/wand_rod_tainted"], 2),
    "consuming": ("CONSUMING", -4, -8, 1, [("vacuos", 4), ("perditio", 3), ("praecantatio", 2)],
                  [f"{TT}:infusion_enchantment"], [], f"{NS}:textures/research/consuming.png", ["infusion/consuming_book"], 0),
    "educational": ("EDUCATIONAL", -4, -7, 2, [("cognitio", 5), ("aversio", 1), ("praecantatio", 3)],
                    [f"{TT}:infusion_enchantment"], [], f"{NS}:textures/research/educational.png", ["infusion/educational_book"], 0),
    "emerald_transmutation": ("TRANSEMERALD", 0, -6, 3, [("vitreus", 2), ("permutatio", 5), ("desiderium", 4)],
                              [f"{TT}:metal_purification"], [], f"{NS}:emerald_nugget", ["crucible/emerald_transmutation"], 0),
    "arcane_cake": ("ARCANECAKE", -12, -6, 2, [("gula", 6), ("desiderium", 3), ("fabrico", 2)],
                    [f"{NS}:nether_shards", f"{TT}:infusion"], [], f"{NS}:arcane_cake", ["infusion/arcane_cake"], 0),
    "black_flower": ("BLACKFLOWER", -2, -6, 1, [("herba", 3), ("sensus", 2), ("tenebrae", 4)], [], [], f"{NS}:umbral_rose",
                     ["crucible/umbral_bush", "black_ink"], 0),
    "taint_tree": ("TAINTTREE", -11, 3, 3, [("herba", 7), ("vitium", 6), ("mortuus", 2)],
                   [f"{TT}:metallurgy", f"{TT}:infusion", f"{TT}:ethereal_bloom"], [], f"{NS}:taint_sapling",
                   ["crucible/taint_sapling", "taint_planks", "taint_coal"], 1),
    "taint_stone": ("TAINTSTONE", -10, 3, 2, [("terra", 4), ("vitium", 6), ("ordo", 2)],
                    [f"{TT}:metallurgy", f"{TT}:infusion", f"{TT}:ethereal_bloom"], [], f"{NS}:taint_stone",
                    ["arcane/taint_stone", "taint_stone_bricks"], 0),
    "wrath_cage": ("WRATHCAGE", -6, -9, 3, [("ira", 5), ("machina", 3), ("bestia", 2)],
                   [f"{NS}:diabolist_fork", f"~{TT}:infusion", f"~{TT}:metallurgy", f"~{TT}:essentia_smelter"], [], f"{NS}:wrath_cage",
                   ["infusion/wrath_cage", "crucible/mob_crystal"], 3),
    "ring_nutrition": ("RINGFOOD", -11, -6, 2, [("gula", 4), ("desiderium", 3), ("victus", 2)], [f"{NS}:nether_shards"], [],
                       f"{NS}:ring_nutrition", ["arcane/ring_nutrition"], 0),
    "sub_collar": ("SUBCOLLAR", -11, -5, 2, [("auram", 3), ("luxuria", 8), ("praecantatio", 3), ("vinculum", 6), ("bestia", 4)],
                   [f"{NS}:nether_shards", f"{TT}:vis_amulet", f"{TT}:infusion"], [], f"{NS}:sub_collar", ["infusion/sub_collar"], 0),
    "crystalwell": ("CRYSTALWELL", -2, -8, 1, [("cognitio", 3), ("vitreus", 2), ("praecantatio", 1)], [f"{TT}:research"], [],
                    f"{NS}:crystalwell", ["arcane/crystalwell"], 0),
    "primewell": ("PRIMEWELL", 2, -8, 1, [("cognitio", 3), ("alienis", 6), ("fabrico", 1)], [f"{TT}:primordial_pearl"], [],
                  f"{NS}:primewell", ["arcane/primewell"], 0),
    "hellfire": ("HELLFIRE", -8, -8, 1, [("ignis", 3), ("infernus", 4), ("motus", 2)], [f"{NS}:nether_shards"], ["round"],
                 "minecraft:fire_charge", [], 0),
    "blink_upgrades": ("FOCUSBLINK", -11, -3, 2, [("motus", 3), ("perditio", 3), ("infernus", 3), ("desidia", 6)],
                       [f"{NS}:nether_shards", f"{TT}:focus_blink"], [], f"focus:{NS}:hellfire", [], 0),
    "riding_crop": ("RIDINGCROP", 0, -8, 0, [], [], ["autounlock", "round"], f"{NS}:riding_crop", ["riding_crop"], 0),
}


def parse_lang(path: Path):
    out = {}
    if not path.exists():
        return out
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            out[k.strip()] = v.strip()
    return out


fm_en = parse_lang(FM_LANG / "en_US.lang")
fm_ru = parse_lang(FM_LANG / "ru_RU.lang")


def pages(lang, fm_key):
    keys = sorted((k for k in lang if k.startswith(f"forbidden.research_page.{fm_key}.") and re.fullmatch(r".*\.\d+$", k)),
                  key=lambda k: int(k.rsplit(".", 1)[1]))
    return [lang[k] for k in keys]


LANG_EN, LANG_RU = {}, {}
for key, (fm_key, col, row, complexity, note, parents, meta, icon, recipes, warp) in RESEARCH.items():
    first = {"text": f"research.{NS}.{key}.stage_0"}
    stages = [first]
    if "autounlock" not in meta and "hidden" not in meta:
        first["required_knowledge"] = [{"type": "theory" if complexity >= 2 else "observation", "category": CATEGORY, "amount": 1}]
        stages.append({"text": f"research.{NS}.{key}.stage_1"})
    if recipes:
        stages[-1]["recipes"] = [f"{NS}:{r}" for r in recipes]
    if warp:
        stages[-1]["warp"] = warp
    entry = {"category": CATEGORY, "name": f"research.{NS}.{key}.title", "column": col + 5, "row": row + 3, "icons": [icon],
             "stages": stages, "complexity": max(1, min(3, complexity))}
    if parents:
        entry["parents"] = parents
    if meta:
        entry["meta"] = meta
    if note:
        entry["note_aspects"] = alist(note)
    write(DATA / NS / TT / "research_entry" / f"{key}.json", entry)
    for lang, store in [(fm_en, LANG_EN), (fm_ru, LANG_RU)]:
        texts = pages(lang, fm_key) or pages(fm_en, fm_key)
        title = lang.get(f"forbidden.research_name.{fm_key}") or fm_en.get(f"forbidden.research_name.{fm_key}", key)
        store[f"research.{NS}.{key}.title"] = title
        store[f"research.{NS}.{key}.stage_0"] = texts[0] if texts else title
        if len(stages) > 1:
            store[f"research.{NS}.{key}.stage_1"] = "<BR><BR>".join(texts) if texts else title

# --------------------------------------------------------------------------------------------------------------------
# Lang
# --------------------------------------------------------------------------------------------------------------------
ITEM_NAMES = {
    "wrath_shard": "item.NetherShard.wrath.name", "envy_shard": "item.NetherShard.envy.name", "taint_shard": "item.NetherShard.taint.name",
    "pride_shard": "item.NetherShard.pride.name", "lust_shard": "item.NetherShard.lust.name", "sloth_shard": "item.NetherShard.sloth.name",
    "greed_shard": "item.NetherShard.greed.name", "gluttony_shard": "item.GluttonyShard.name", "emerald_nugget": "item.FMResource.0.name",
    "black_ink": "item.FMResource.1.name", "taint_fruit": "item.TaintFruit.name", "taint_coal": "item.TaintCoal.name",
    "skull_axe": "item.SkullAxe.name", "diabolist_fork": "item.DiabolistFork.name", "taint_shovel": "item.TaintShovel.name",
    "taint_pickaxe": "item.TaintPickaxe.name", "morph_pickaxe": "item.MorphPickaxe.name", "morph_sword": "item.MorphSword.name",
    "morph_shovel": "item.MorphShovel.name", "morph_axe": "item.MorphAxe.name", "riding_crop": "item.RidingCrop.name",
    "wand_rod_tainted": "item.WandCores.tainted.name", "wand_rod_infernal": "item.WandCores.infernal.name",
    "wand_rod_profane": "item.WandCores.profane.name", "mob_crystal": "item.MobCrystalEmpty.name",
    "ring_nutrition": "item.RingNutrition.name", "sub_collar": "item.SubCollar.name",
    "crystalwell": "item.Crystalwell.name", "primewell": "item.Primewell.name",
}
ENCH_NAMES = {"wrath": "enchantment.wrath", "capitalist": "enchantment.greedy", "consuming": "enchantment.consuming",
              "educational": "enchantment.educational", "corrupting": "enchantment.corrupting", "impact": "enchantment.impact",
              "voidtouched": "enchantment.voidtouched", "fiery_core": "enchantment.cluster"}
ROD_NAMES = {"tainted": "item.Wand.tainted.rod", "infernal": "item.Wand.infernal.rod", "profane": "item.Wand.profane.rod",
             "profaned": "item.Wand.profaned.rod"}
ASPECT_HELP_EN = {"infernus": "nether", "ira": "wrath", "invidia": "envy", "gula": "gluttony", "superbia": "pride",
                  "luxuria": "lust", "desidia": "sloth"}
EXTRA = {
    "en": {"itemGroup.forbidden": "Forbidden Magic", f"research_category.{NS}.{NS}": "Forbidden Magic",
           "message.forbidden.taint_fruit": "Your stomach churns and something deep inside you begins to grow...",
           "message.forbidden.wrath_cage.mode.mob": "The cage now hungers for the essence of its captive",
           "message.forbidden.wrath_cage.mode.ira": "The cage now hungers for Ira", "message.forbidden.wrath_cage.mode.desidia": "The cage now hungers for Desidia"},
    "ru": {"itemGroup.forbidden": "Запретная магия", f"research_category.{NS}.{NS}": "Запретная магия",
           "message.forbidden.taint_fruit": "Ваш желудок сводит, и что-то глубоко внутри начинает расти...",
           "message.forbidden.wrath_cage.mode.mob": "Клетка жаждет сущности своего пленника",
           "message.forbidden.wrath_cage.mode.ira": "Клетка жаждет Ira", "message.forbidden.wrath_cage.mode.desidia": "Клетка жаждет Desidia"},
}

BLOCK_NAMES = {"arcane_cake": "tile.ArcaneCake.name", "umbral_rose": "tile.InkFlower.name", "umbral_bush": "tile.blackRoseBush.name",
               "nether_star_block": "tile.StarBlock.name", "taint_log": "tile.TaintLog.name", "taint_planks": "tile.TaintPlank.name",
               "taint_leaves": "tile.TaintLeaves.name", "taint_sapling": "tile.TaintSapling.name", "taint_stone": "tile.TaintStone_Smooth.name",
               "taint_stone_bricks": "tile.TaintStone_Brick.name", "wrath_cage": "tile.WrathCage.name"}

# Texts FM never translated (or that describe Thaumaturge-specific behaviour).
LANG_OVERRIDES = {'en': {'research.forbidden.blink_upgrades.title': 'Blink Upgrades', 'research.forbidden.blink_upgrades.stage_1': "Thaumaturge's Blink already carries you through the aether. Two infernal refinements can be woven into the same spell.<BR><BR>Hellfire unleashes a burst of fire on everything around the spot where the spell lands. Weave it after Blink to scorch whatever waits at your destination.<BR><BR>Pandemonium drags every monster near the spell's target to where you stand. Weave it before Blink and the horde is left behind where you started."}, 'ru': {'block.forbidden.taint_leaves': 'Осквернённая листва', 'block.forbidden.taint_log': 'Осквернённое бревно', 'block.forbidden.taint_planks': 'Осквернённые доски', 'block.forbidden.taint_sapling': 'Осквернённый саженец', 'block.forbidden.taint_stone': 'Осквернённый камень', 'block.forbidden.taint_stone_bricks': 'Осквернённые каменные кирпичи', 'entity.forbidden.villager.heretic': 'Еретик', 'item.forbidden.primewell': 'Первородные письменные принадлежности', 'item.forbidden.taint_coal': 'Проклятый уголь', 'item.forbidden.taint_fruit': 'Осквернённый плод', 'item.forbidden.wand_rod_profane': 'Нечестивый стержень', 'wand.thaumaturge.rod.profane': 'Нечестивый', 'wand.thaumaturge.rod.profaned': 'Нечестивый', 'spell.forbidden.hellfire.name': 'Адское пламя', 'spell.forbidden.hellfire.desc': 'Обрушивает огненный шквал на всё вокруг места, куда попало заклинание.', 'spell.forbidden.pandemonium.name': 'Пандемониум', 'spell.forbidden.pandemonium.desc': 'Переносит чудовищ рядом с целью туда, где стоит заклинатель.', 'research.forbidden.blink_upgrades.title': 'Улучшения скачка', 'research.forbidden.blink_upgrades.stage_0': 'Ох.<BR><BR>Хуже, чем вставать, только ходить куда-то пешком. Столько раааботы. Какой смысл быть великим магом, если всё равно приходится переставлять ноги, как все остальные?', 'research.forbidden.blink_upgrades.stage_1': 'Скачок Thaumaturge и так переносит вас сквозь эфир. В то же заклинание можно вплести два адских усовершенствования.<BR><BR>«Адское пламя» обрушивает огненный шквал на всё вокруг места, куда попало заклинание. Вплетите его после Скачка, чтобы опалить тех, кто ждёт вас в точке прибытия.<BR><BR>«Пандемониум» переносит всех чудовищ рядом с целью заклинания туда, где стоите вы. Вплетите его перед Скачком — и орда останется там, откуда вы ушли.', 'research.forbidden.primewell.title': 'Первородные письменные принадлежности', 'research.forbidden.primewell.stage_0': 'Вы узнали то, что людям знать не положено, видели то, чего видеть не положено, и бывали там, куда ходить не положено.<BR><BR>Всё это научило вас одному: как наполнить чернильницу.<BR><BR>Используя странные свойства Первородной жемчужины, можно создать письменные принадлежности, в которых никогда не кончатся чернила. Даже на холодном и тёмном краю времён, когда угаснет свет миллионов звёзд, Первородные письменные принадлежности будут готовы писать дальше.', 'research.forbidden.primewell.stage_1': 'Вы узнали то, что людям знать не положено, видели то, чего видеть не положено, и бывали там, куда ходить не положено.<BR><BR>Всё это научило вас одному: как наполнить чернильницу.<BR><BR>Используя странные свойства Первородной жемчужины, можно создать письменные принадлежности, в которых никогда не кончатся чернила. Даже на холодном и тёмном краю времён, когда угаснет свет миллионов звёзд, Первородные письменные принадлежности будут готовы писать дальше.', 'research.forbidden.rod_profane.title': 'Нечестивый жезл', 'research.forbidden.rod_profane.stage_0': 'Странные жители порой добывают эти деревянные жезлы, покрытые богохульными знаками. Запас вис у нечестивых жезлов скромный, но они очень быстро наполняются из незримого источника.<BR><BR>Руны на жезле иногда сдвигаются и кровоточат, а сам жезл время от времени шепчет о демонических договорах и мерзких деяниях.<BR><BR>Долгое использование не рекомендуется.', 'research.forbidden.taint_stone.title': 'Осквернённый камень', 'research.forbidden.taint_stone.stage_0': 'Осквернённые земли — не лучшее место для прогулок. Волокнистая скверна жадно расползается по любым поверхностям, окутывая их и делая опасными. Однако самим осквернённым существам и материалам волокна, похоже, не вредят.<BR><BR>Смешав скверну с обычным камнем, вы получили магический материал — прочный, но неподвластный волокнистой скверне. Из осквернённого камня можно строить в Осквернённых землях безопасные дорожки.<BR><BR>Хотя строиться в Осквернённых землях всё же не советуем...', 'research.forbidden.taint_stone.stage_1': 'Осквернённые земли — не лучшее место для прогулок. Волокнистая скверна жадно расползается по любым поверхностям, окутывая их и делая опасными. Однако самим осквернённым существам и материалам волокна, похоже, не вредят.<BR><BR>Смешав скверну с обычным камнем, вы получили магический материал — прочный, но неподвластный волокнистой скверне. Из осквернённого камня можно строить в Осквернённых землях безопасные дорожки.<BR><BR>Хотя строиться в Осквернённых землях всё же не советуем...', 'research.forbidden.taint_tree.title': 'Осквернённое дерево', 'research.forbidden.taint_tree.stage_0': 'Больные, искажённые отходы магических опытов порой обретают собственную странную жизнь. Большинство растений не выживает в Осквернённых землях, но вы нашли способ вырастить то, что выживет. Вымочив саженец в скверне, вы получите дерево, которое процветает среди скверны.<BR><BR>Из его древесины выходят доски, устойчивые к волокнистой скверне: из них можно строить, не боясь её распространения.<BR><BR>Дерево даже плодоносит, но есть эти плоды не рекомендуется...', 'research.forbidden.taint_tree.stage_1': 'Больные, искажённые отходы магических опытов порой обретают собственную странную жизнь. Большинство растений не выживает в Осквернённых землях, но вы нашли способ вырастить то, что выживет. Вымочив саженец в скверне, вы получите дерево, которое процветает среди скверны.<BR><BR>Из его древесины выходят доски, устойчивые к волокнистой скверне: из них можно строить, не боясь её распространения.<BR><BR>Дерево даже плодоносит, но есть эти плоды не рекомендуется...'}}

for code, fm_lang, store in [("en_us", fm_en, LANG_EN), ("ru_ru", fm_ru, LANG_RU)]:
    short = code[:2]
    out = dict(EXTRA[short])
    for block, fm_key in BLOCK_NAMES.items():
        out[f"block.{NS}.{block}"] = fm_lang.get(fm_key) or fm_en.get(fm_key, block)
    for item, fm_key in ITEM_NAMES.items():
        out[f"item.{NS}.{item}"] = fm_lang.get(fm_key) or fm_en.get(fm_key, item)
    for ench, fm_key in ENCH_NAMES.items():
        out[f"enchantment.{NS}.{ench}"] = fm_lang.get(fm_key) or fm_en.get(fm_key, ench)
    for rod, fm_key in ROD_NAMES.items():
        out[f"wand.{TT}.rod.{rod}"] = fm_lang.get(fm_key) or fm_en.get(fm_key, rod)
    for aspect in FM_ASPECTS:
        out[f"aspect.{NS}.{aspect}"] = aspect.capitalize()
        desc = fm_lang.get(f"tc.aspect.{aspect}") or fm_en.get(f"tc.aspect.{aspect}", "")
        out[f"aspect.{NS}.{aspect}.desc"] = desc
        out[f"aspect.{NS}.{aspect}.help"] = desc.split(",")[0].strip().lower() if desc else ASPECT_HELP_EN[aspect]
    imprinted = fm_lang.get("item.MobCrystal.name") or fm_en["item.MobCrystal.name"]
    out[f"item.{NS}.mob_crystal.imprinted"] = imprinted
    out[f"entity.{NS}.villager.heretic"] = fm_lang.get("description.villager.profession.forbidden.heretic") or fm_en["description.villager.profession.forbidden.heretic"]
    for part in ["hellfire", "pandemonium"]:
        out[f"spell.{NS}.{part}.name"] = fm_lang.get(f"forbidden.upgrade.{part}.name") or fm_en[f"forbidden.upgrade.{part}.name"]
        out[f"spell.{NS}.{part}.desc"] = fm_lang.get(f"forbidden.upgrade.{part}.text") or fm_en[f"forbidden.upgrade.{part}.text"]
    for key in ["message.collar.alreadywearing", "message.collar.placescollar", "message.collar.youplacecollar"]:
        out[key.replace("message.", f"message.{NS}.")] = fm_lang.get(key) or fm_en[key]
    out[f"tooltip.{NS}.collar.owner"] = (fm_lang.get("tooltip.collar.owner") or fm_en["tooltip.collar.owner"]) + " %s"
    out[f"tooltip.{NS}.capacity"] = "Capacity: %s" if short == "en" else "Ёмкость: %s"
    out.update(store)
    out.update(LANG_OVERRIDES[short])
    write(ASSETS / "lang" / f"{code}.json", dict(sorted(out.items())))

print(f"generated {len(RECIPES)} recipes, {len(RESEARCH)} research entries, {len(base)} item aspect entries")
