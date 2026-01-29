# Worldgen tuning guide (MountainOres)

This mod intentionally splits **data-tuning** (JSON worldgen) from **code** (custom features + registration).

If you only want to rebalance ores, you should almost always edit **JSON** under:

- `src/main/resources/data/mountainores/worldgen/placed_feature/`
- `src/main/resources/data/mountainores/worldgen/configured_feature/`

If you enabled Ore Override (`config/mountainores.json` → `overrideVanillaOres: true`), there is an additional "Band 0" (Y<=63) set of placed/configured features you can tune:

- `.../placed_feature/*__underground__*__placed.json`
- `.../configured_feature/*__underground__cfg.json`

## Quick reference

| You want to change… | Edit this | Safe knobs | Avoid touching |
|---|---|---|---|
| Band ore spawn rate / height curve | `.../placed_feature/*__y*__placed.json` | `count`, `rarity_filter`, `height_range` | Filename/ID (used by `WorldGenRegistrar`) |
| Band ore targets + air exposure discard | `.../configured_feature/*__cfg.json` | `targets`, `discard_chance_on_air_exposure` | `type` (must match Java feature registry) |
| Vein size range per ore-band | `src/main/java/.../FeatureRegistrar.java` | `minSize`, `maxSize` ints | Registry names (strings) |
| Lode global frequency | `.../placed_feature/lode__selector__placed.json` | `rarity_filter.chance` | Feature ID string |
| Lode weights / allowed Y / size | `.../configured_feature/lode__selector__cfg.json` | `entries[]`, `exposure_shrink_factor` | Entry field names/types |

If worldgen “stops working” after edits, it’s almost always an **ID mismatch** (renamed JSON filename or changed `type`).

## Naming conventions (IDs)

Worldgen IDs are designed to be readable and consistent:

- `*__<band_or_role>__y<min>_<max>__placed` = a placed feature that runs in that Y window.
- `*__<band_or_role>__cfg` = a configured feature referenced by the placed feature.
- `*__<band_or_role>__ore` = the Java Feature `type` used by the configured feature.

Common role segments:

- `underground` = replacement generation for **Y<=63** (used only when `overrideVanillaOres` is enabled)
- `mid_altitude` = the mid-height layer (renamed from the older `upper` label)

If you rename any of these IDs, update JSON references and `WorldGenRegistrar` together.

## Mental model (how it works)

There are three layers:

1. **Feature type (Java)**
  - Registered in code (Feature registry): `mountainores:*__ore` and `mountainores:lode__selector`.
   - This is the implementation (how blocks get placed).

2. **Configured feature (JSON)**
   - Files in `.../configured_feature/*.json`.
   - These define *what* a feature does (targets, discard chance, lode weights, etc).
  - The **configured feature ID** is the filename, e.g. `configured_feature/iron__main__cfg.json` → `mountainores:iron__main__cfg`.

3. **Placed feature (JSON)**
   - Files in `.../placed_feature/*_placed.json`.
   - These define *how often* and *where* it runs (count/rarity/height range/biome).
  - The **placed feature ID** is the filename, e.g. `placed_feature/iron__main__y60_200__placed.json` → `mountainores:iron__main__y60_200__placed`.

At runtime, `WorldGenRegistrar` injects the **placed features** into Overworld biomes.

## Safe knobs (recommended edits)

### Band ore frequency (most common tuning)

Edit the placed features in `.../placed_feature/*__placed.json`:

- `minecraft:count` = attempts per chunk
- `minecraft:rarity_filter` = extra 1/N gate (only present for some ores)
- `minecraft:height_range` = vertical distribution (triangle/trapezoid)

Rule of thumb:

- Effective average attempts per chunk is roughly:
  - `count` if there is no rarity filter
  - `count / chance` if there is `rarity_filter` with `chance = N`

### Band ore “feel” (vein size / exposure discard)

- Vein size for band ores is randomized in Java (`FeatureRegistrar`) per ore+band.
  - See: `FeatureRegistrar.registerFeature(name, minSize, maxSize)`
- “Discard on air exposure” is set per configured feature JSON:
  - `discard_chance_on_air_exposure`

### Lodes (jackpot deposits)

Files:

- Config: `.../configured_feature/lode__selector__cfg.json`
- Placement: `.../placed_feature/lode__selector__placed.json`

What to tune safely in `lode__selector__cfg.json`:

- `exposure_shrink_factor` (0..1)
- `entries[]`:
  - `weight` (relative chance among entries)
  - `min_y`, `max_y` (where this ore-lode is allowed)
  - `min_size`, `max_size`
  - `ore.targets[]` (stone/deepslate targets)

The global frequency is controlled in `lode__selector__placed.json`:

- `minecraft:rarity_filter` with `chance: 1000` means ~1 lode-roll per 1000 chunks on average.

Important: the feature chooses the final Y itself from each entry’s `min_y..max_y`.

## Things to avoid (unless you know why)

### Don’t rename IDs casually

Avoid renaming:

- Java feature IDs registered in `FeatureRegistrar` (`mountainores:*__ore`, `mountainores:lode__selector`)
- JSON filenames for configured/placed features
- The strings referenced by `WorldGenRegistrar.addOreFeatureToBiomes("...")`

Renaming any of these requires updating multiple places (code + JSON) and will silently break worldgen if IDs stop matching.

### Don’t change configured_feature `type` unless you also change Java registration

Example:

- `configured_feature/iron__main__cfg.json` has `"type": "mountainores:iron__main__ore"`
- That `type` must match a Java-registered Feature in `FeatureRegistrar`.

## Where to edit what

- **Add/remove a placed feature from the world**: `WorldGenRegistrar.registerAll()`
- **Change min/max vein size per ore-band**: `FeatureRegistrar.registerFeatures()`
- **Change ore target blocks / discard chance**: `configured_feature/*__cfg.json`
- **Change frequency/height distribution**: `placed_feature/*_placed.json`
- **Change lode weights / size / allowed Y**: `configured_feature/lode__selector__cfg.json`
- **Change lode global rarity**: `placed_feature/lode__selector__placed.json`

## Quick sanity checklist after tuning

- Run `./gradlew build`.
- Ensure every `placed_feature/*.json` references an existing configured feature ID.
- Ensure every `configured_feature/*.json` uses a `type` that exists in Java.
- If worldgen seems “missing”, check you didn’t break an ID match.
