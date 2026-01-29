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
| Coal | ~190 and ~957 and ~35 | -32–68; 120–1360 | All Overworld biomes; overlap zones avoid hard cuts |
| Copper | ~130 and ~50 | -16–68; 60–1100 | All Overworld biomes; 32–68 is intentionally emphasized |
| Iron | ~460 and ~1412 and ~16 | -64–68; 60–1730 | All Overworld biomes; underground (Y<=63) low peak is vanilla-like; includes extra “small iron” hits |
| Gold | ~1288 and ~-16 | -64–32; 975–2031 | All Overworld biomes; underground (Y<=63) is vanilla-like (no badlands extra); high-alt gold is rare (rarity filter) |
| Diamond | ~-59 | -64–16 | All Overworld biomes; underground (Y<=63) replacement |
| Redstone | ~-59 | -64–16 | All Overworld biomes; underground (Y<=63) replacement |
| Lapis Lazuli | ~0 | -64–64 | All Overworld biomes; vanilla-like (lapis + buried lapis) |
| Emerald | ~1288 | 975–2031 | All Overworld biomes; high-alt emerald is rare (rarity filter) |

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
