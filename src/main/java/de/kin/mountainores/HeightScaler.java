package de.kin.mountainores;

/**
 * Central height-scaling utility for MountainOres.
 *
 * All above-ground ore placement (Y >= {@link #SCALE_THRESHOLD}) is authored
 * for a reference maximum world height of {@link #REFERENCE_HEIGHT}.
 * At runtime the effective max world height is determined in one of two ways:
 * <ul>
 *   <li><b>Auto-detect</b> (default): the actual world top Y is read from the
 *       dimension's {@code HeightLimitView} (e.g. 320 vanilla, 512 Lithosphere).</li>
 *   <li><b>Manual override</b>: the user sets {@code autoDetectWorldHeight=false}
 *       and specifies {@code maxWorldHeight} in the config file.</li>
 * </ul>
 *
 * Underground values (Y < {@link #SCALE_THRESHOLD}) are never touched.
 */
public final class HeightScaler {

    /** Y values below this are considered "underground" and never scaled. */
    public static final int SCALE_THRESHOLD = 64;

    /** The max world height the JSON placement values are designed for. */
    public static final int REFERENCE_HEIGHT = 2032;

    private HeightScaler() {}

    /** Tracks whether we already logged the detected height (once per session). */
    private static volatile boolean loggedDetectedHeight = false;

    // ── Resolution ──────────────────────────────────────────────────────

    /**
     * Resolve the effective maximum world height.
     *
     * <p>If {@code autoDetectWorldHeight} is enabled (the default), the given
     * {@code actualWorldTopY} is returned directly.  Otherwise the manually
     * configured {@code maxWorldHeight} is used.
     *
     * <p>The first time this is called, the resolved height is logged.
     *
     * @param actualWorldTopY the real top Y of the current world
     *                        (e.g. from {@code world.getTopY()})
     * @return the height to use for scaling calculations
     */
    public static int resolveMaxWorldHeight(int actualWorldTopY) {
        MountainOresConfig cfg = MountainOres.CONFIG;
        int effective;
        if (cfg != null && !cfg.autoDetectWorldHeight) {
            effective = cfg.maxWorldHeight;
        } else {
            effective = actualWorldTopY;
        }

        if (!loggedDetectedHeight) {
            loggedDetectedHeight = true;
            double factor = getFactor(effective);
            MountainOres.LOGGER.info(
                    "[mountainores] World height resolved: detected={}, effective={}, factor={}, mode={}",
                    actualWorldTopY, effective,
                    String.format("%.4f", factor),
                    (cfg != null && !cfg.autoDetectWorldHeight) ? "manual" : "auto-detect");
        }

        return effective;
    }

    /**
     * Returns the manually configured {@code maxWorldHeight} from the config.
     * Useful for logging; at runtime prefer {@link #resolveMaxWorldHeight(int)}.
     */
    public static int getConfiguredMaxWorldHeight() {
        MountainOresConfig cfg = MountainOres.CONFIG;
        return cfg != null ? cfg.maxWorldHeight : REFERENCE_HEIGHT;
    }

    /**
     * Whether auto-detection of world height is enabled.
     */
    public static boolean isAutoDetect() {
        MountainOresConfig cfg = MountainOres.CONFIG;
        return cfg == null || cfg.autoDetectWorldHeight;
    }

    // ── Scaling helpers ─────────────────────────────────────────────────

    /**
     * Scale a single Y value from the reference coordinate space to the
     * given maximum world height.
     *
     * @param y              the original Y value (designed for {@link #REFERENCE_HEIGHT})
     * @param maxWorldHeight the effective maximum world height
     * @return the scaled Y value, clamped to [{@link #SCALE_THRESHOLD}, maxWorldHeight]
     */
    public static int scaleY(int y, int maxWorldHeight) {
        if (maxWorldHeight == REFERENCE_HEIGHT || y < SCALE_THRESHOLD) {
            return y;
        }
        double factor = (double) (maxWorldHeight - SCALE_THRESHOLD)
                      / (double) (REFERENCE_HEIGHT - SCALE_THRESHOLD);
        int scaled = SCALE_THRESHOLD + (int) Math.round((y - SCALE_THRESHOLD) * factor);
        return Math.max(SCALE_THRESHOLD, Math.min(scaled, maxWorldHeight));
    }

    /**
     * Returns the linear scaling factor for the given world height.
     *
     * @param maxWorldHeight the effective maximum world height
     * @return 1.0 when {@code maxWorldHeight == REFERENCE_HEIGHT}
     */
    public static double getFactor(int maxWorldHeight) {
        if (maxWorldHeight == REFERENCE_HEIGHT) return 1.0;
        return (double) (maxWorldHeight - SCALE_THRESHOLD)
             / (double) (REFERENCE_HEIGHT - SCALE_THRESHOLD);
    }

    /**
     * Scale a count value proportionally to the given world height.
     * Always returns at least 1 so ore placement never gets completely removed.
     *
     * @param count          the original count (designed for {@link #REFERENCE_HEIGHT})
     * @param maxWorldHeight the effective maximum world height
     * @return the scaled count, at least 1
     */
    public static int scaleCount(int count, int maxWorldHeight) {
        double factor = getFactor(maxWorldHeight);
        if (factor == 1.0) return count;
        return Math.max(1, (int) Math.round(count * factor));
    }
}
