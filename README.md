# Forbidden Magic for Thaumaturge

An unofficial, standalone NeoForge port of SpitefulFox's [Forbidden Magic](https://github.com/SpitefulFox/ForbiddenMagic)
(WTFPL) to [Thaumaturge](https://github.com/Leclowndu93150/Thaumaturge) 1.1.0 on Minecraft 26.1.2.

The analysis, the Thaumcraft 4 → Thaumaturge mapping, every deviation from the original and the verified status of each
milestone are in [PORTING_PLAN.md](PORTING_PLAN.md) (Russian).

## Content

- **Aspects**: Infernus, Ira, Invidia, Gula, Superbia, Luxuria, Desidia, plus FM's aspect additions to vanilla items and mobs.
- **Deadly shards**: wrath, envy, pride, lust, sloth, greed, gluttony and taint, dropped by FM's Nether rules.
- **Tools**: Axe of the Skulltaker, Diabolist Fork, Shovel of the Purifier, Pickaxe of Distortion, the four Chameleon
  tools with their three switchable phases, Riding Crop.
- **Wand rods**: tainted, infernal and profane (with its demonic contract). Thaumaturge generates their wand and sceptre
  assembly recipes.
- **Enchantments**: Wrath, Capitalist, Consuming, Educational, Corrupting, Fiery Core, Impact, Voidtouched, infused into
  books and applied on an anvil.
- **Blocks**: Thaumic Cake, Umbral Rose and Rose Bush, Nether Star Block, the tainted tree, tainted stone.
- **Wrath Cage** with Imprinting Crystals, fuelled by essentia; spawnable mobs are the `forbidden:wrath_cage_mobs` data map.
- **Curios**: Ring of Nutrition, Collar of Pain.
- **Scribing tools**: Crystal and Primal Scribing Tools.
- **Hellfire** wand trigger, the **Heretic** villager, and FM's Blink upgrades as the spell parts Hellfire and Pandemonium.
- A Thaumonomicon category with 30 research entries; English and Russian translations.

Cross-mod features of the original (Blood Magic, Botania, Ars Magica 2, EE3, ...) are not ported: those mods do not
exist for this Minecraft version.

## Building

Requires JDK 25.

```bash
./gradlew build
```

Thaumaturge 1.1.0 is pulled from CurseMaven (CurseForge file 9428045); Curios and Lithostitched are runtime-only.

## Checking

```bash
./gradlew runGameTestServer
```

```bash
./gradlew runServer
```

```bash
python -I tools/check_lang.py .
```

```bash
python -I tools/check_assets.py .
```

Visual check: `runClientCheck` joins the world `run-check/saves/fmcheck`, builds a showcase of every block and item,
opens the research book and saves screenshots to `run-check/screenshots/fmcheck_*.png`, then quits.

```bash
./gradlew runClientCheck
```

## Regenerating data

Most content is data. `tools/gen_data.py` writes aspects, item/entity aspect maps, research, recipes, enchantments, tags,
loot tables, trades and the en_us/ru_ru lang files; `tools/gen_assets.py` copies FM's textures and writes models and
blockstates. See the docstring at the top of each script for its arguments.

## Skills for future porting work

`.claude/skills/` holds agent skills distilled from this port (loaded automatically by Claude Code in this project):
`legacy-mod-porting` (workflow), `neoforge-26-1-api` (old API → 26.1.2), `thaumcraft-to-thaumaturge` (TC4 → Thaumaturge
mapping and data formats) and `mc-port-verification` (GameTests, client screenshots, generic asset/lang linters).
