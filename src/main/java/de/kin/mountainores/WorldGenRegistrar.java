package de.kin.mountainores;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

public class WorldGenRegistrar {

    private static final Logger LOGGER = LoggerFactory.getLogger("mountainores");

    public static void registerAll() {

        MountainOresConfig config = MountainOres.CONFIG != null ? MountainOres.CONFIG : new MountainOresConfig();
        if (config.overrideVanillaOres) {
            overrideVanillaOresInOverworld(config.logVanillaOreOverride);
            addVanillaLikeUndergroundOres();
            LOGGER.info("[mountainores] Vanilla ore override enabled (remove minecraft:ore_* + add underground replacements)");
        }

        // Tuning guide: docs/worldgen-tuning.md
        //
        // SAFE TO CHANGE for balancing:
        // - the JSON contents under data/mountainores/worldgen/**
        //
        // AVOID changing casually:
        // - the string IDs passed to addOreFeatureToBiomes("...")
        //   These must match placed_feature filenames ("<id>.json" => "mountainores:<id>").
        // Band ores: Y > 63 custom distribution (Y<=63 uses vanilla)
        // Lodes: very rare jackpot deposits across all heights (height-limited per ore via placed_feature)

        // Band-based placed features (see docs/height-bands.md)

        // Coal (bands 1-3 only; extended high tail via placement)
        addOreFeatureToBiomes("coal__main__y120_260__placed");
        addOreFeatureToBiomes("coal__mid_altitude__y175_360__placed");
        addOreFeatureToBiomes("coal__high__y275_560__placed");
        addOreFeatureToBiomes("coal__high_tail__y555_1360__placed");

        // Iron (bands 1-4)
        addOreFeatureToBiomes("iron__main__y60_200__placed");
        addOreFeatureToBiomes("iron__mid_altitude__y115_300__placed");
        addOreFeatureToBiomes("iron__high__y215_700__placed");
        addOreFeatureToBiomes("iron__mountain_tail__y595_1100__placed");
        addOreFeatureToBiomes("iron__high_tail__y1095_1730__placed");

        // Copper (bands 1-4; weak tail in band 4)
        addOreFeatureToBiomes("copper__main__y60_200__placed");
        addOreFeatureToBiomes("copper__mid_altitude__y115_300__placed");
        addOreFeatureToBiomes("copper__high__y215_700__placed");
        addOreFeatureToBiomes("copper__mountain_tail__y595_1100__placed");

        // Gold + Emerald (band 5 + modest tail in band 6)
        addOreFeatureToBiomes("gold__mountain_main__y975_1600__placed");
        addOreFeatureToBiomes("emerald__mountain_main__y975_1600__placed");
        addOreFeatureToBiomes("gold__mountain_tail__y1475_2031__placed");
        addOreFeatureToBiomes("emerald__mountain_tail__y1475_2031__placed");

        // Rare large deposits ("lodes") across all heights.
        // Exactly one roll per chunk (via rarity), then weighted ore selection inside the feature.
        addOreFeatureToBiomes("lode__selector__placed");

        LOGGER.info("[mountainores] Registered biome modifications for placed features (Y>63)");
    }

    private static void overrideVanillaOresInOverworld(boolean logEach) {
        // Only "real" ores: leave terrain patches (tuff/granite/diorite/andesite/dirt/gravel/...) untouched.
        String[] vanillaPlacedFeaturesToRemove = new String[] {
                // Coal
                "ore_coal_upper",
                "ore_coal_lower",
                // Iron
                "ore_iron_upper",
                "ore_iron_middle",
                "ore_iron_small",
                // Copper
                "ore_copper",
                "ore_copper_large",
                // Gold
                "ore_gold",
                "ore_gold_lower",
                "ore_gold_extra",
                // Redstone
                "ore_redstone",
                "ore_redstone_lower",
                // Lapis
                "ore_lapis",
                "ore_lapis_buried",
                // Diamond
                "ore_diamond",
                "ore_diamond_medium",
                "ore_diamond_large",
                "ore_diamond_buried",
                // Emerald (we keep MountainOres emerald; remove vanilla emerald so datapacks can't override it)
                "ore_emerald"
        };

        for (String vanillaIdPath : vanillaPlacedFeaturesToRemove) {
            removeVanillaPlacedFeatureFromBiomes(vanillaIdPath, logEach);
        }
    }

    private static void removeVanillaPlacedFeatureFromBiomes(String vanillaPlacedFeaturePath, boolean logEach) {
        Identifier featureId = Identifier.of("minecraft", vanillaPlacedFeaturePath);
        RegistryKey<PlacedFeature> featureKey = RegistryKey.of(RegistryKeys.PLACED_FEATURE, featureId);

        BiomeModifications.create(Identifier.of("mountainores", "remove_" + vanillaPlacedFeaturePath))
                .add(
                        ModificationPhase.REMOVALS,
                        BiomeSelectors.foundInOverworld(),
                        (selection, context) -> context.getGenerationSettings().removeFeature(GenerationStep.Feature.UNDERGROUND_ORES, featureKey)
                );

        if (logEach) {
            LOGGER.info("[mountainores] Remove vanilla PlacedFeature from biomes: {}", featureId);
        }
    }

    private static void addVanillaLikeUndergroundOres() {
        Predicate<BiomeSelectionContext> overworldExceptDripstoneCaves = (context) ->
            BiomeSelectors.foundInOverworld().test(context) && !context.getBiomeKey().equals(BiomeKeys.DRIPSTONE_CAVES);

        // Underground replacements (Y<=63, vanilla-like, slightly reduced; emerald intentionally excluded)
        addOreFeatureToBiomes("coal__underground__y-32_0__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("coal__underground__y1_68__placed", overworldExceptDripstoneCaves);

        addOreFeatureToBiomes("iron__underground__y-64_0__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("iron__underground__main__y-24_56__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("iron__underground__low__y1_32__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("iron__underground__tail__y24_68__placed", overworldExceptDripstoneCaves);

        // Vanilla-like "small iron" extra hits
        addOreFeatureToBiomes("iron__underground_small__y-64_0__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("iron__underground_small__y1_68__placed", overworldExceptDripstoneCaves);

        addOreFeatureToBiomes("copper__underground__y-16_0__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("copper__underground__low__y1_48__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("copper__underground__peak__y32_68__placed", overworldExceptDripstoneCaves);

        // Special-case: dripstone caves get extra-large copper instead of normal copper placements.
        // Note: we intentionally deviate from vanilla here by allowing the placement across all heights
        // (some packs can generate dripstone caves far above vanilla terrain heights).
        addOreFeatureToBiomes(
            "copper__dripstone_caves__large__all_heights__placed",
            BiomeSelectors.includeByKey(BiomeKeys.DRIPSTONE_CAVES)
        );

        addOreFeatureToBiomes("gold__underground__main__y-64_32__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("gold__underground__deep__y-64_-48__placed", overworldExceptDripstoneCaves);

        // Badlands extra gold (vanilla-like ore_gold_extra replacement)
        // Uses all heights (below_top: 0) to support packs like JJ Thunder where Badlands can spawn at extreme heights.
        addOreFeatureToBiomes(
            "gold__badlands_extra__y32_all_heights__placed",
            BiomeSelectors.includeByKey(BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS)
        );

        addOreFeatureToBiomes("redstone__underground__best__y-64_-54__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("redstone__underground__tail__y-54_16__placed", overworldExceptDripstoneCaves);

        addOreFeatureToBiomes("lapis__underground__open__y-32_32__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("lapis__underground__buried__y-64_64__placed", overworldExceptDripstoneCaves);

        addOreFeatureToBiomes("diamond__underground__best__y-64_-54__placed", overworldExceptDripstoneCaves);
        addOreFeatureToBiomes("diamond__underground__tail__y-54_16__placed", overworldExceptDripstoneCaves);

        // Vanilla-like large diamond veins (ore_diamond_large equivalent, rarity 1/9)
        addOreFeatureToBiomes("diamond__underground_large__y-64_-54__placed", overworldExceptDripstoneCaves);
    }

    private static void addOreFeatureToBiomes(String featureName) {
        addOreFeatureToBiomes(featureName, BiomeSelectors.foundInOverworld());
    }

    private static void addOreFeatureToBiomes(String featureName, Predicate<BiomeSelectionContext> selector) {
        Identifier featureId = Identifier.of("mountainores", featureName);
        RegistryKey<PlacedFeature> featureKey = RegistryKey.of(RegistryKeys.PLACED_FEATURE, featureId);

        // Feature zu allen Biomen hinzufügen
        LOGGER.info("[mountainores] Add PlacedFeature to biomes: {}", featureId);
        BiomeModifications.addFeature(
            selector,
                GenerationStep.Feature.UNDERGROUND_ORES,
                featureKey
        );
    }
}
