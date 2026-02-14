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

@SuppressWarnings("null")
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
        addOreFeatureToBiomes("coal__main__placed");
        addOreFeatureToBiomes("coal__mid_altitude__placed");
        addOreFeatureToBiomes("coal__high__placed");
        addOreFeatureToBiomes("coal__high_tail__placed");

        // Iron (bands 1-4)
        addOreFeatureToBiomes("iron__main__placed");
        addOreFeatureToBiomes("iron__mid_altitude__placed");
        addOreFeatureToBiomes("iron__high__placed");
        addOreFeatureToBiomes("iron__mountain_tail__placed");
        addOreFeatureToBiomes("iron__high_tail__placed");

        // Copper (bands 1-4; weak tail in band 4)
        addOreFeatureToBiomes("copper__main__placed");
        addOreFeatureToBiomes("copper__mid_altitude__placed");
        addOreFeatureToBiomes("copper__high__placed");
        addOreFeatureToBiomes("copper__mountain_tail__placed");

        // Gold + Emerald (band 5 + modest tail in band 6)
        addOreFeatureToBiomes("gold__mountain_main__placed");
        addOreFeatureToBiomes("emerald__mountain_main__placed");
        addOreFeatureToBiomes("gold__mountain_tail__placed");
        addOreFeatureToBiomes("emerald__mountain_tail__placed");

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
        // Use simple BiomeSelectors.foundInOverworld() for maximum compatibility with C2ME and other mods
        // Dripstone caves will get both normal copper AND large copper - slightly more copper, but more reliable
        
        // Underground replacements (Y<=63, vanilla-like; emerald intentionally excluded)
        addOreFeatureToBiomes("coal__underground__deep__placed");
        addOreFeatureToBiomes("coal__underground__upper__placed");

        addOreFeatureToBiomes("iron__underground__deep__placed");
        addOreFeatureToBiomes("iron__underground__main__placed");
        addOreFeatureToBiomes("iron__underground__low__placed");
        addOreFeatureToBiomes("iron__underground__tail__placed");

        // Vanilla-like "small iron" extra hits
        addOreFeatureToBiomes("iron__underground_small__deep__placed");
        addOreFeatureToBiomes("iron__underground_small__upper__placed");

        addOreFeatureToBiomes("copper__underground__deep__placed");
        addOreFeatureToBiomes("copper__underground__low__placed");
        addOreFeatureToBiomes("copper__underground__peak__placed");

        // Special-case: dripstone caves get extra-large copper in addition to normal copper placements.
        addOreFeatureToBiomes(
            "copper__dripstone_caves__large__placed",
            BiomeSelectors.includeByKey(BiomeKeys.DRIPSTONE_CAVES)
        );

        addOreFeatureToBiomes("gold__underground__main__placed");
        addOreFeatureToBiomes("gold__underground__deep__placed");

        // Badlands extra gold (vanilla-like ore_gold_extra replacement)
        // Uses all heights (below_top: 0) to support packs like JJ Thunder where Badlands can spawn at extreme heights.
        addOreFeatureToBiomes(
            "gold__badlands_extra__placed",
            BiomeSelectors.includeByKey(BiomeKeys.BADLANDS, BiomeKeys.ERODED_BADLANDS, BiomeKeys.WOODED_BADLANDS)
        );

        addOreFeatureToBiomes("redstone__underground__best__placed");
        addOreFeatureToBiomes("redstone__underground__tail__placed");

        addOreFeatureToBiomes("lapis__underground__open__placed");
        addOreFeatureToBiomes("lapis__underground__buried__placed");

        addOreFeatureToBiomes("diamond__underground__best__placed");
        addOreFeatureToBiomes("diamond__underground__tail__placed");

        // Vanilla-like large diamond veins (ore_diamond_large equivalent, rarity 1/9)
        addOreFeatureToBiomes("diamond__underground_large__placed");
    }

    private static void addOreFeatureToBiomes(String featureName) {
        addOreFeatureToBiomes(featureName, BiomeSelectors.foundInOverworld());
    }

    private static void addOreFeatureToBiomes(String featureName, Predicate<BiomeSelectionContext> selector) {
        Identifier featureId = Identifier.of("mountainores", featureName);
        RegistryKey<PlacedFeature> featureKey = RegistryKey.of(RegistryKeys.PLACED_FEATURE, featureId);

        LOGGER.info("[mountainores] Add PlacedFeature to biomes: {}", featureId);
        
        // Use BiomeModifications.create() with ADDITIONS phase for consistent registration
        BiomeModifications.create(Identifier.of("mountainores", "add_" + featureName))
                .add(
                        ModificationPhase.ADDITIONS,
                        selector,
                        (selectionContext, modificationContext) -> {
                            modificationContext.getGenerationSettings().addFeature(
                                    GenerationStep.Feature.UNDERGROUND_ORES,
                                    featureKey
                            );
                        }
                );
    }
}
