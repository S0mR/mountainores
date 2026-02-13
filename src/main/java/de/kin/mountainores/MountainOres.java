package de.kin.mountainores;

import de.kin.mountainores.placement.ScaledCountPlacementModifier;
import de.kin.mountainores.placement.ScaledHeightRangePlacementModifier;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MountainOres implements ModInitializer {
	public static final String MOD_ID = "mountainores";
	public static MountainOresConfig CONFIG;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		CONFIG = MountainOresConfig.load(LOGGER);

		// Register custom placement modifier types
		// (must happen before worldgen JSONs are deserialized)
		ScaledHeightRangePlacementModifier.register();
		ScaledCountPlacementModifier.register();

		FeatureRegistrar.registerFeatures();
		WorldGenRegistrar.registerAll();

		if (HeightScaler.isAutoDetect()) {
			LOGGER.info("[mountainores] Height scaling: auto-detect enabled (reference={})",
					HeightScaler.REFERENCE_HEIGHT);
		} else {
			LOGGER.info("[mountainores] Height scaling: manual maxWorldHeight={} (reference={})",
					CONFIG.maxWorldHeight, HeightScaler.REFERENCE_HEIGHT);
		}

		LOGGER.info("Mountain Ores initialized successfully");
	}
}