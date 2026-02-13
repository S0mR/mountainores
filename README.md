# MountainOres v2.0 (MC 1.21.11 · Fabric)

MountainOres adds ore generation **above sea level (Y ≥ 64)** for any world height — vanilla or modded — while keeping underground generation (Y ≤ 63) intact.

It **automatically detects** the actual world height at runtime and scales both ore Y-positions and spawn counts to match. No manual configuration needed.

## Features

- **Automatic height scaling** — reads the dimension's real height at runtime; no manual config needed
- **Band-based ore distribution** for Y ≥ 64 (coal, iron, copper, gold, emerald)
- **Scaled spawn counts** — ore density stays consistent regardless of world height
- **Lode deposits** — very rare "jackpot" veins (one roll per ~1000 chunks, weighted ore selection)
- **Vanilla ore override** — optionally replaces vanilla ore placement to prevent datapack conflicts
- Vanilla-like behavior where possible (targets, air-exposure discard)

## Height Scaling

All above-ground ore placement (Y ≥ 64) is internally authored for a reference height of 2032. At runtime, the mod detects the actual world height and linearly scales both Y-positions and spawn counts to fit. Examples:

| World setup        | Detected height | Scaling factor |
|--------------------|-----------------|----------------|
| Vanilla            | 320             | 0.130          |
| Lithosphere        | 512             | 0.228          |
| JJThunder          | 2032            | 1.000 (no scaling) |

The scaling is handled by two custom placement modifiers:

- `mountainores:scaled_height_range` — scales Y positions
- `mountainores:scaled_count` — scales spawn counts proportionally

Underground ores (Y < 64) are **never** scaled.

## Datapack Compatibility

MountainOres is tested with:

- **Lithosphere** (world height 576, Y -64 to 512)
- **JJThunderToTheMax** (world height 2032, Y -64 to 2032) 

MountainOres uses its own namespace (`mountainores:`) and the Fabric Biome Modification API, so it does not conflict with datapacks that modify vanilla ore features.

## Configuration

On first launch, `config/mountainores.toml` is generated with full comments:

```toml
# Replace vanilla ore generation with MountainOres equivalents.
overrideVanillaOres = true

# Log which vanilla ore features were replaced (useful for debugging).
logVanillaOreOverride = false

# When true (default), reads the actual world height from the dimension.
autoDetectWorldHeight = true

# Manual override (only used when autoDetectWorldHeight = false).
maxWorldHeight = 2032
```

### Config options

| Option                   | Default | Description |
|--------------------------|---------|-------------|
| `overrideVanillaOres`    | `true`  | Remove vanilla ore placed-features and replace with MountainOres underground equivalents (Y ≤ 63) |
| `logVanillaOreOverride`  | `false` | Log which vanilla ores were replaced (debug) |
| `autoDetectWorldHeight`  | `true`  | Auto-detect world height from the dimension at runtime |
| `maxWorldHeight`         | `2032`  | Manual world height override (only when `autoDetectWorldHeight = false`) |

## Ore Override Mode

When `overrideVanillaOres = true`:

- Removes selected vanilla **real ore** placed-features from all Overworld biomes (coal, iron, copper, gold, redstone, lapis, diamond, emerald)
- Keeps terrain patches untouched (tuff/granite/diorite/andesite/dirt/gravel are NOT removed)
- Adds MountainOres-managed **Band 0** replacement ores for Y ≤ 63 (vanilla-like, ~8–15% reduced)

Note: Emerald is handled by MountainOres only above ground (no underground emerald replacement).

## Tuning / Balancing

Worldgen tuning is JSON-driven:

- `src/main/resources/data/mountainores/worldgen/placed_feature/` — frequency + height distribution
- `src/main/resources/data/mountainores/worldgen/configured_feature/` — targets, discard chance, lode weights

Guides:

- [docs/worldgen-tuning.md](docs/worldgen-tuning.md)
- [docs/height-bands.md](docs/height-bands.md)

## Build

```sh
./gradlew build
```

Output JAR: `build/libs/mountainores-2.0.0.jar`

## Install

- Requires **Minecraft 1.21.11**, Fabric Loader, and Fabric API
- Drop the built JAR into your instance's `mods/` folder
- The TOML library (toml4j) is bundled inside the JAR — no extra dependencies needed

## License

MIT (see [LICENSE](LICENSE)).
