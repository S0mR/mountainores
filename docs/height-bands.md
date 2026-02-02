# Height bands (Y>=64) for MountainOres (tall worlds up to 2032)

Goal: a clear, stable set of height-band names so tuning is easy later, without mixing naming with the “two overlap placed-features” technique.

See also: `docs/worldgen-tuning.md` for “what to change vs what to avoid”.

## Recommended 6-band model (fine-tuning friendly)

These bands describe only the custom area (Y>=64). By default, vanilla generation remains in control for Y<=63.

Optional: you can enable an **Ore Override** mode (see `config/mountainores.json`, `overrideVanillaOres`) which removes selected vanilla `minecraft:ore_*` placed-features ("real" ores only; terrain patches like tuff/granite remain) and replaces them with MountainOres-managed underground (**Band 0**) generation for Y<=63.

Terminology note: the band names below ("Lower/Upper/High") are human-facing height labels. The JSON IDs use role segments like `__mid_altitude__` (instead of the older `upper`) and `__underground__`.

Example `config/mountainores.json`:

```json
{
	"overrideVanillaOres": true,
	"logVanillaOreOverride": false
}
```

This specific split keeps the existing major cut lines (256 / 1024 / 1536) intact, while adding two extra “fine-tuning” splits.

These six bands are a **conceptual model** (stable naming and rough cut lines). The actual worldgen uses overlapping windows around these cut lines; treat the implemented windows below as the **source of truth**.

1. **Band 1 (Lower Foothills):** Y=64–159
2. **Band 2 (Upper Foothills):** Y=160–255
3. **Band 3 (Lower Mountains):** Y=256–639
4. **Band 4 (Upper Mountains):** Y=640–1023
5. **Band 5 (High Mountains):** Y=1024–1535
6. **Band 6 (Peak Zone):** Y=1536–2032

Notes:
- A band name should describe a **height interval**, not the distribution shape.
- “Vanilla-like curves” can be built *within a band* by combining multiple placed-features (e.g. a `*__low__y...__placed` + a `*__tail__y...__placed`). This approach is used in a few places (e.g. high tails) to extend a band smoothly without changing the main window.

## Current implementation

The current worldgen JSONs implement the 6-band model using **overlapping placed-features** to avoid visible “hard cuts”.

Mixing rule (current): **3–5 blocks below and above each band boundary** are used as an overlap zone.

Practical (implemented) band windows used by the placed-features right now (this is what the JSONs actually encode):
- Band 1: 60–200
- Band 2: 115–300
- Band 3: 215–700
- Band 4: 595–1100
- Band 5: 975–1600
- Band 6: 1475–2031

Note on naming: in JSON IDs you may see role segments like `__mid_altitude__` (previously called “upper”) and `__underground__` (Y<=63 override features).

If `overrideVanillaOres` is enabled, an additional underground **Band 0** is active (Y<=63):
- Band 0a: -64–0 (deepslate underground)
- Band 0b: 1–68 (stone underground + small overlap into Band 1)

## Ore overview (current)

This table is derived from the placed-feature JSONs in `src/main/resources/data/mountainores/worldgen/placed_feature/`.
If you change heights/counts there, update this table too.

This uses the same column layout and ore order as the vanilla reference table below.
All values are approximate (especially when multiple placed-features contribute).
Assumption for this table: `overrideVanillaOres: true`.

| Ore | Best Y-level (approx.) | Y-level range (approx.) | Biome notes |
|---|---:|---|---|
| Coal | ~190 and ~957 and ~35 | -64–68; 120–1360 | All Overworld biomes; underground Y=-64..0 (count 10) + Y=1..68 (count 12); high-altitude extends to 1360 |
| Copper | ~50 and ~130 | -16–68; 60–1100 | All Overworld biomes; underground peak Y=32..68 (count 7); dripstone caves extra (count 16) |
| Iron | ~16 and ~460 and ~1412 | -64–68; 60–1730 | All Overworld biomes; deep small veins Y=-64..0 (size 4, count 10); large veins Y=-24..56; high-alt extends to 1730 |
| Gold | ~-16 and ~1288 | -64–32; 975–2031 | All Overworld biomes; underground trapezoid Y=-64..32 (count 4); badlands extra Y=32–top (count 50); high-alt rare (1/18 and 1/24) |
| Diamond | ~-59 | -64–16 | All Overworld biomes; best Y=-64..-54 (count 5); tail Y=-54..16 (count 3); large veins (1/5 chance) |
| Redstone | ~-59 | -64–16 | All Overworld biomes; best Y=-64..-54 (count 8); tail Y=-54..16 (count 4) |
| Lapis Lazuli | ~0 | -64–64 | All Overworld biomes; buried Y=-64..64 (count 4); open trapezoid Y=-32..32 (count 2) |
| Emerald | ~1288 | 975–2031 | All Overworld biomes; high-alt only; rare (1/24 and 1/32) |

### Detailed placed-feature breakdown

**Coal (6 features):**
- `coal__underground__y-32_0`: Y=-64..0, uniform, count 10
- `coal__underground__y1_68`: Y=1..68, trapezoid (plateau 40), count 12
- `coal__main__y120_260`: Y=120..260, trapezoid (plateau 72), count 8
- `coal__mid_altitude__y175_360`: Y=175..360, trapezoid (plateau 96), count 6
- `coal__high__y275_560`: Y=275..560, trapezoid (plateau 140), count 4
- `coal__high_tail__y555_1360`: Y=555..1360, trapezoid (plateau 260), count 2

**Copper (8 features):**
- `copper__underground__y-16_0`: Y=-16..0, uniform, count 4
- `copper__underground__low__y1_48`: Y=1..48, trapezoid, count 5
- `copper__underground__peak__y32_68`: Y=32..68, trapezoid (plateau 16), count 7
- `copper__main__y60_200`: Y=60..200, trapezoid (plateau 72), count 3
- `copper__mid_altitude__y115_300`: Y=115..300, trapezoid (plateau 96), count 4
- `copper__high__y215_700`: Y=215..700, trapezoid (plateau 200), count 2
- `copper__mountain_tail__y595_1100`: Y=595..1100, trapezoid (plateau 200), count 1
- `copper__dripstone_caves__large`: all heights, trapezoid, count 16 (dripstone caves only)

**Iron (11 features):**
- `iron__underground_small__y-64_0`: Y=-64..0, uniform, count 10, size 4 (vanilla-like)
- `iron__underground__y-64_0`: Y=-64..0, uniform, count 4, size 9
- `iron__underground__main__y-24_56`: Y=-24..56, trapezoid, count 7
- `iron__underground__low__y1_32`: Y=1..32, trapezoid (plateau 8), count 6
- `iron__underground__tail__y24_68`: Y=24..68, trapezoid, count 2
- `iron__underground_small__y1_68`: Y=1..68, uniform, count 2
- `iron__main__y60_200`: Y=60..200, trapezoid (plateau 72), count 5
- `iron__mid_altitude__y115_300`: Y=115..300, trapezoid (plateau 96), count 5
- `iron__high__y215_700`: Y=215..700, trapezoid (plateau 200), count 8
- `iron__mountain_tail__y595_1100`: Y=595..1100, trapezoid (plateau 200), count 3
- `iron__high_tail__y1095_1730`: Y=1095..1730, trapezoid (plateau 200), count 2

**Gold (5 features):**
- `gold__underground__deep__y-64_-48`: Y=-64..-48, uniform, count 0-1
- `gold__underground__main__y-64_32`: Y=-64..32, trapezoid, count 4
- `gold__badlands_extra__y32_all_heights`: Y=32..top, uniform, count 50 (badlands only)
- `gold__mountain_main__y975_1600`: Y=975..1600, trapezoid (plateau 240), count 1, rarity 1/18
- `gold__mountain_tail__y1475_2031`: Y=1475..2031, trapezoid (plateau 200), count 1, rarity 1/24

**Diamond (3 features):**
- `diamond__underground__best__y-64_-54`: Y=-64..-54, trapezoid, count 5
- `diamond__underground__tail__y-54_16`: Y=-54..16, trapezoid, count 3
- `diamond__underground_large__y-64_-54`: Y=-64..-54, trapezoid, count 1, rarity 1/5

**Redstone (2 features):**
- `redstone__underground__best__y-64_-54`: Y=-64..-54, trapezoid, count 8
- `redstone__underground__tail__y-54_16`: Y=-54..16, trapezoid, count 4

**Lapis Lazuli (2 features):**
- `lapis__underground__buried__y-64_64`: Y=-64..64, uniform, count 4
- `lapis__underground__open__y-32_32`: Y=-32..32, trapezoid, count 2

**Emerald (2 features):**
- `emerald__mountain_main__y975_1600`: Y=975..1600, trapezoid (plateau 240), count 1, rarity 1/24
- `emerald__mountain_tail__y1475_2031`: Y=1475..2031, trapezoid (plateau 200), count 1, rarity 1/32

## Vanilla reference (Minecraft 1.21.10)

This is a **vanilla 1.21.10** reference table (Overworld), meant as a quick comparison point.
Values are derived from the vanilla `placed_feature` JSONs and are therefore **approximate** (especially when multiple placed-features contribute to the same ore).

| Ore | Best Y-level (approx.) | Y-level range (approx.) | Biome notes |
|---|---:|---|---|
| Coal | ~96 (plus a separate high-alt layer) | 0–top (~320); strong at 0–192 and again 136–top | Not biome-locked; common across most biomes |
| Copper | 48 | (all heights) | Extra common in dripstone caves (uses additional copper feature) |
| Iron | 16 and ~232 | -64–384 | Strong at low Y and again in high mountains |
| Gold | -16 | -64–32 (most biomes); up to 256 in badlands (extra gold) | Abundant in badlands up to Y=256 |
| Diamond | ~-59 | -64–16 | Mostly deep slate levels |
| Redstone | ~-59 | -64–15 | Always deep underground |
| Lapis Lazuli | 0 | -64–64 | Best around Y=0 |
| Emerald | ~232 | -16–480 | Primarily in mountain biomes (windswept/meadow/peaks) |

## Next step (optional)

The 6-band model is already implemented. Optional next steps if you want even finer control:

- Split a band into multiple placed-features (e.g. `*__low__y...__placed` + `*__tail__y...__placed`) to shape “vanilla-like” curves more precisely.
- Keep IDs stable where possible; renaming JSON filenames/IDs requires changing `WorldGenRegistrar` and can break worldgen if anything stops matching.
