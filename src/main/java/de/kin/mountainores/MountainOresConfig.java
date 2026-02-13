package de.kin.mountainores;

import com.moandjiezana.toml.Toml;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class MountainOresConfig {

	private static final String FILE_NAME = "mountainores.toml";

	/**
	 * When enabled:
	 * - removes selected vanilla (minecraft:ore_*) placed-features from the Overworld
	 * - adds MountainOres underground (Y<=63) replacements (vanilla-like, slightly reduced)
	 *
	 * This prevents datapacks (like JJThunder) from overriding ore spawn via minecraft:ore_*.
	 */
	public boolean overrideVanillaOres = true;

	public boolean logVanillaOreOverride = false;

	/**
	 * When true (default), the mod reads the actual world height at runtime
	 * from the dimension's HeightLimitView (e.g. 320 for vanilla, 512 for
	 * Lithosphere) and scales ore placement automatically.
	 *
	 * Set to false to use the manual {@link #maxWorldHeight} value instead.
	 */
	public boolean autoDetectWorldHeight = true;

	/**
	 * Manual override for the maximum world height.
	 * Only used when {@link #autoDetectWorldHeight} is {@code false}.
	 *
	 * All above-ground ore placement (Y>=64) is designed for a reference height of 2032.
	 * If your world uses a different max height (e.g. 512 for Lithosphere, 320 for vanilla),
	 * set this value and all ore Y-ranges will be scaled proportionally.
	 *
	 * Underground ores (Y<64) are never scaled.
	 *
	 * Default: 2032 (no scaling).
	 */
	public int maxWorldHeight = 2032;

	// ── Load / Save ─────────────────────────────────────────────────────

	public static MountainOresConfig load(Logger logger) {
		Path configDir = FabricLoader.getInstance().getConfigDir();
		Path configPath = configDir.resolve(FILE_NAME);
		MountainOresConfig config = readOrDefault(configPath, logger);
		writeIfMissing(configPath, config, logger);
		return config;
	}

	private static MountainOresConfig readOrDefault(Path path, Logger logger) {
		if (!Files.exists(path)) {
			return new MountainOresConfig();
		}

		try {
			Toml toml = new Toml().read(path.toFile());
			MountainOresConfig config = new MountainOresConfig();

			config.overrideVanillaOres  = toml.getBoolean("overrideVanillaOres",  config.overrideVanillaOres);
			config.logVanillaOreOverride = toml.getBoolean("logVanillaOreOverride", config.logVanillaOreOverride);
			config.autoDetectWorldHeight = toml.getBoolean("autoDetectWorldHeight", config.autoDetectWorldHeight);

			Long maxHeight = toml.getLong("maxWorldHeight");
			if (maxHeight != null) {
				config.maxWorldHeight = maxHeight.intValue();
			}

			return config;
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
			Files.writeString(path, toTomlString(config), StandardCharsets.UTF_8);
			logger.info("[mountainores] Wrote default config to {}", path);
		} catch (IOException e) {
			logger.warn("[mountainores] Failed to write default config {}: {}", path, e.toString());
		}
	}

	/**
	 * Generates a well-commented TOML representation of the config.
	 */
	private static String toTomlString(MountainOresConfig cfg) {
		StringBuilder sb = new StringBuilder();
		sb.append("# ╔══════════════════════════════════════════════════╗\n");
		sb.append("# ║         MountainOres Configuration              ║\n");
		sb.append("# ╚══════════════════════════════════════════════════╝\n");
		sb.append("\n");

		sb.append("# Replace vanilla ore generation with MountainOres equivalents.\n");
		sb.append("# When true, selected vanilla ore placed-features (minecraft:ore_*)\n");
		sb.append("# are removed from the Overworld and replaced with MountainOres\n");
		sb.append("# underground variants (Y <= 63) that are vanilla-like but slightly\n");
		sb.append("# reduced. This prevents datapacks from interfering with ore spawns.\n");
		sb.append("overrideVanillaOres = ").append(cfg.overrideVanillaOres).append("\n");
		sb.append("\n");

		sb.append("# Log which vanilla ore features were replaced (useful for debugging).\n");
		sb.append("logVanillaOreOverride = ").append(cfg.logVanillaOreOverride).append("\n");
		sb.append("\n");

		sb.append("# ── Height Scaling ───────────────────────────────────\n");
		sb.append("\n");

		sb.append("# When true (default), the mod automatically reads the actual world\n");
		sb.append("# height from the dimension at runtime (e.g. 320 for vanilla, 512 for\n");
		sb.append("# Lithosphere) and scales ore placement to fit.\n");
		sb.append("#\n");
		sb.append("# Set to false to use the manual 'maxWorldHeight' value below instead.\n");
		sb.append("autoDetectWorldHeight = ").append(cfg.autoDetectWorldHeight).append("\n");
		sb.append("\n");

		sb.append("# Manual override for the maximum world height.\n");
		sb.append("# Only used when autoDetectWorldHeight = false.\n");
		sb.append("#\n");
		sb.append("# All above-ground ore placement (Y >= 64) is designed for a reference\n");
		sb.append("# height of 2032. If your world uses a different max height, set this\n");
		sb.append("# value and all ore Y-ranges + counts will be scaled proportionally.\n");
		sb.append("# Underground ores (Y < 64) are never scaled.\n");
		sb.append("maxWorldHeight = ").append(cfg.maxWorldHeight).append("\n");

		return sb.toString();
	}
}
