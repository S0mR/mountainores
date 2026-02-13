# MountainOres (MC >=1.21.10 Fabric)

MountainOres adjusts ore generation for **very tall worlds (up to Y=2032)** while keeping vanilla generation intact for **Y<=63**.

Optionally, MountainOres can **override vanilla ore placement** to prevent datapacks from changing ore spawn via `minecraft:ore_*` placed features.

## Features

- Band-based ore distribution for Y>=64 (coal/iron/copper/gold/emerald)
- Very rare “jackpot” deposits (lodes): one roll per ~1000 chunks, then weighted ore selection
- Vanilla-like behavior where possible (targets, air-exposure discard behavior)

## Tuning / balancing

Worldgen tuning is intentionally JSON-driven:

- `src/main/resources/data/mountainores/worldgen/placed_feature/` (frequency + height distribution)
- `src/main/resources/data/mountainores/worldgen/configured_feature/` (targets, discard chance, lode weights)

Guides:
- `docs/worldgen-tuning.md`
- `docs/height-bands.md`

## Optional: Ore Override mode (recommended if you use ore-altering datapacks)

If you run datapacks that modify vanilla ores (e.g. by overriding `minecraft:worldgen/placed_feature/ore_*.json`), you can enable MountainOres **Ore Override** mode.

What it does:
- Removes selected vanilla **real ore** placed-features from all Overworld biomes (coal/iron/copper/gold/redstone/lapis/diamond/emerald).
- Keeps terrain patches untouched (tuff/granite/diorite/andesite/dirt/gravel are NOT removed).
- Adds MountainOres-managed **Band 0** replacement ores for **Y<=63** (vanilla-like, ~8–15% reduced).

How to enable:
- Start the game/server once to generate `config/mountainores.json`.
- Set `overrideVanillaOres` to `true`.

Example `config/mountainores.json`:

```json
{
	"overrideVanillaOres": true,
	"logVanillaOreOverride": false
}
```

Note: Emerald is still handled by MountainOres (no new underground (Y<=63) emerald).

## Build

- `./gradlew build`
- Output JAR: `build/libs/mountainores-<version>.jar`

## Install

- Requires Minecraft `1.21.10`, Fabric Loader, and Fabric API.
- Drop the built JAR into your instance’s `mods/` folder.

## License

MIT (see `LICENSE`).
