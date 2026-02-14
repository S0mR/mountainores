package de.kin.mountainores;

import de.kin.mountainores.feature.LodeSelectorFeature;
import de.kin.mountainores.feature.VariableOreFeature;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("null")
public class FeatureRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger("mountainores");

    public static void registerFeatures() {
        // Tuning guide: docs/worldgen-tuning.md
        // Height bands: docs/height-bands.md (Y>=64 only)
        //
        // SAFE TO CHANGE here:
        // - min/max vein size ranges (the int arguments)
        //
        // AVOID changing here unless you also update JSON + biome injection:
        // - the feature registration names ("*_ore" / "lode__selector")

        // Coal (Y>63 custom distribution; capped/extended via placement)
        registerFeature("coal__main__ore", 13, 21);
        registerFeature("coal__mid_altitude__ore", 11, 20);
        registerFeature("coal__high__ore", 10, 17);

        // Underground replacements (used when overrideVanillaOres=true)
        // Keep these reasonably vanilla-like; the exact per-height distribution is defined in placed_feature JSON.
        // Vanilla coal size: 17
        registerFeature("coal__underground__ore", 12, 17);

        // Iron (Y>63)
        registerFeature("iron__main__ore", 10, 18);
        registerFeature("iron__mid_altitude__ore", 10, 18);
        registerFeature("iron__high__ore", 12, 20);
        registerFeature("iron__mountain_tail__ore", 10, 18);

        // Underground iron (normal + small)
        registerFeature("iron__underground__ore", 7, 12);
        registerFeature("iron__underground_small__ore", 4, 7);

        // Copper (Y>63; weak tail in mountain tail placement)
        registerFeature("copper__main__ore", 12, 22);
        registerFeature("copper__mid_altitude__ore", 12, 22);
        registerFeature("copper__high__ore", 10, 20);
        registerFeature("copper__mountain_tail__ore", 8, 16);

        // Underground copper
        registerFeature("copper__underground__ore", 8, 14);

        // Vanilla-like dripstone caves copper (matches vanilla ore_copper_large vein size ~20)
        registerFeature("copper__dripstone_caves__large__ore", 20, 20);

        // Gold (mountain main + mountain tail)
        registerFeature("gold__mountain_main__ore", 6, 12);
        registerFeature("gold__mountain_tail__ore", 5, 10);

        // Underground gold / redstone / lapis / diamond (vanilla-like vein sizes)
        // Vanilla sizes: gold=9, redstone=8, lapis=7, diamond=4/8/12
        registerFeature("gold__underground__ore", 7, 11);

        // Badlands extra gold (vanilla-like: ore_gold_extra spawns Y 32-256 with 50 attempts)
        registerFeature("gold__badlands_extra__ore", 7, 11);
        registerFeature("redstone__underground__ore", 6, 10);
        registerFeature("lapis__underground__ore", 5, 9);
        registerFeature("diamond__underground__ore", 3, 5);
        registerFeature("diamond__underground_large__ore", 8, 12);

        // Emerald (mountain main + mountain tail)
        registerFeature("emerald__mountain_main__ore", 2, 5);
        registerFeature("emerald__mountain_tail__ore", 2, 5);

        // Rare large deposits ("lodes")
        // Implemented as a single weighted selector feature with exactly one roll per chunk (via placement rarity).
        // Entries and weights are configured in the lode selector configured_feature JSON.
        registerLodeSelectorFeature();

        LOGGER.info("[mountainores] Registered ore features");
    }

    private static void registerFeature(String name, int minSize, int maxSize) {
        // Note: the registered ID is "mountainores:<name>".
        // Worldgen JSONs reference these IDs via configured_feature "type".
        LOGGER.info("[mountainores] Register Feature '{}' (vein blocks: {}-{})", name, minSize, maxSize);
        Registry.register(
                Registries.FEATURE,
                Identifier.of("mountainores", name),
                new VariableOreFeature(OreFeatureConfig.CODEC, minSize, maxSize)
        );
    }

    private static void registerLodeSelectorFeature() {
        String name = "lode__selector";
        LOGGER.info("[mountainores] Register Feature '{}' (weighted lode selector)", name);
        Registry.register(
                Registries.FEATURE,
                Identifier.of("mountainores", name),
                new LodeSelectorFeature(LodeSelectorFeature.Config.CODEC)
        );
    }
}
