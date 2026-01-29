package de.kin.mountainores;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MountainOresConfig {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "mountainores.json";

	/**
	 * When enabled:
	 * - removes selected vanilla (minecraft:ore_*) placed-features from the Overworld
	 * - adds MountainOres underground (Y<=63) replacements (vanilla-like, slightly reduced)
	 *
	 * This prevents datapacks (like JJThunder) from overriding ore spawn via minecraft:ore_*.
	 */
	public boolean overrideVanillaOres = true;

	public boolean logVanillaOreOverride = false;

	public static MountainOresConfig load(Logger logger) {
		Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		MountainOresConfig config = readOrDefault(configPath, logger);
		writeIfMissing(configPath, config, logger);
		return config;
	}

	private static MountainOresConfig readOrDefault(Path path, Logger logger) {
		if (!Files.exists(path)) {
			return new MountainOresConfig();
		}

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			MountainOresConfig parsed = GSON.fromJson(reader, MountainOresConfig.class);
			return parsed != null ? parsed : new MountainOresConfig();
		} catch (Exception e) {
			logger.warn("[mountainores] Failed to read config {} (using defaults): {}", path, e.toString());
			return new MountainOresConfig();
		}
	}

	private static void writeIfMissing(Path path, MountainOresConfig config, Logger logger) {
		if (Files.exists(path)) {
			return;
		}

		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(config, writer);
			}
			logger.info("[mountainores] Wrote default config to {}", path);
		} catch (IOException e) {
			logger.warn("[mountainores] Failed to write default config {}: {}", path, e.toString());
		}
	}
}
