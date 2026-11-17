"""Checks that every translation key the mod uses exists in en_us and ru_ru, and lists ru_ru entries still in English.

Usage: python -I tools/check_lang.py <project-dir>
Exit code 1 when a key is missing.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(sys.argv[1])
SRC = ROOT / "src/main/java"
RES = ROOT / "src/main/resources"
LANG = RES / "assets/forbidden/lang"

keys = set()
# Literal keys in code: Component.translatable("...") and string constants that look like keys.
for java in SRC.rglob("*.java"):
    text = java.read_text(encoding="utf-8")
    keys |= set(re.findall(r'translatable\("([a-z0-9_.]+)"', text))
    keys |= set(re.findall(r'"((?:message|tooltip|entity|item|block)\.forbidden\.[a-z0-9_.]+)"', text))
# Registered items and blocks.
for java in (SRC / "fox/spiteful/forbidden/registry").glob("*.java"):
    text = java.read_text(encoding="utf-8")
    for item in re.findall(r'ITEMS\.register(?:Simple)?Item\("([a-z_]+)"', text):
        keys.add(f"item.forbidden.{item}")
    for block in re.findall(r'BLOCKS\.registerBlock\("([a-z_]+)"', text):
        keys.add(f"block.forbidden.{block}")
keys.discard("item.forbidden.umbral_bush")  # uses the block's name (useBlockDescriptionPrefix)
# Data: research names/stages, enchantment descriptions, spell parts, aspects, the research category.
data = RES / "data/forbidden"
for f in (data / "thaumaturge/research_entry").glob("*.json"):
    entry = json.loads(f.read_text(encoding="utf-8"))
    keys.add(entry["name"])
    keys |= {stage["text"] for stage in entry["stages"]}
for f in (data / "enchantment").glob("*.json"):
    keys.add(json.loads(f.read_text(encoding="utf-8"))["description"]["translate"])
for f in (data / "thaumaturge/spell_part").glob("*.json"):
    keys |= {f"spell.forbidden.{f.stem}.name", f"spell.forbidden.{f.stem}.desc"}
for f in (data / "thaumaturge/aspect").glob("*.json"):
    keys |= {f"aspect.forbidden.{f.stem}", f"aspect.forbidden.{f.stem}.desc"}
for f in (data / "thaumaturge/research_category").glob("*.json"):
    keys.add(f"research_category.forbidden.{f.stem}")
keys.add("itemGroup.forbidden")
keys |= {f"wand.thaumaturge.rod.{r}" for r in ["tainted", "infernal", "profane", "profaned"]}

failed = False
langs = {code: json.loads((LANG / f"{code}.json").read_text(encoding="utf-8")) for code in ["en_us", "ru_ru"]}
for code, lang in langs.items():
    missing = sorted(k for k in keys if k not in lang and not k.endswith("."))
    if missing:
        failed = True
        print(f"{code}: {len(missing)} missing")
        for k in missing:
            print("   ", k)
    else:
        print(f"{code}: all {len(keys)} used keys present")
untranslated = sorted(k for k, v in langs["ru_ru"].items() if langs["en_us"].get(k) == v and re.search(r"[A-Za-z]{4}", v) and not k.startswith("aspect.forbidden.") )
print(f"ru_ru entries identical to en_us: {len(untranslated)}")
for k in untranslated:
    print("   ", k)
sys.exit(1 if failed else 0)
