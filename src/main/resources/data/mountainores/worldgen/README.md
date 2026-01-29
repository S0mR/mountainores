# MountainOres worldgen (JSON)

## Folder layout

- `configured_feature/`
  - *What* gets generated (targets, discard chance, lode weights, etc.).
  - Configured feature ID is the filename:
    - `configured_feature/iron__main__cfg.json` ⇒ `mountainores:iron__main__cfg`

- `placed_feature/`
  - *How often* / *where* it runs (count, rarity, height distribution, biome filter).
  - Placed feature ID is the filename:
    - `placed_feature/iron__main__y60_200__placed.json` ⇒ `mountainores:iron__main__y60_200__placed`

## Where is the code?

- Java features are registered in `src/main/java/de/kin/mountainores/FeatureRegistrar.java`
- Biome injection (what gets added to the Overworld) happens in `src/main/java/de/kin/mountainores/WorldGenRegistrar.java`

## What is safe to tweak?

 - Band balancing (recommended): edit `placed_feature/*__placed.json`
  - `minecraft:count`, `minecraft:rarity_filter`, `minecraft:height_range`
- Ore targets / air exposure: edit `configured_feature/*__cfg.json`
  - `targets`, `discard_chance_on_air_exposure`
- Lodes (jackpot deposits):
  - Frequency: `placed_feature/lode__selector__placed.json` (`rarity_filter.chance`)
  - Weights/Y/size: `configured_feature/lode__selector__cfg.json`

## Avoid breaking IDs

If you rename JSON filenames or change a configured_feature `type`, you must update the corresponding Java strings/registrations as well.
Most worldgen “it doesn’t spawn anymore” issues are caused by an **ID mismatch**.

More detailed guide:
- `docs/worldgen-tuning.md`
- `docs/height-bands.md`
