package de.kin.mountainores.placement;

import com.mojang.serialization.MapCodec;
import de.kin.mountainores.HeightScaler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.heightprovider.HeightProvider;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;

import java.util.stream.Stream;

/**
 * A drop-in replacement for {@code minecraft:height_range} that transparently
 * scales Y values above sea-level ({@link HeightScaler#SCALE_THRESHOLD})
 * based on the configured {@code maxWorldHeight}.
 *
 * <p>Usage in placed-feature JSON:
 * <pre>{@code
 * {
 *   "type": "mountainores:scaled_height_range",
 *   "height": {
 *     "type": "minecraft:trapezoid",
 *     "min_inclusive": { "absolute": 120 },
 *     "max_inclusive": { "absolute": 260 },
 *     "plateau": 72
 *   }
 * }
 * }</pre>
 *
 * <p>When {@code maxWorldHeight == 2032} (the default), this behaves identically
 * to {@code minecraft:height_range}.  For any other value the sampled Y is
 * linearly scaled so the full ore distribution fits the actual world height.
 */
public class ScaledHeightRangePlacementModifier extends PlacementModifier {

    public static final MapCodec<ScaledHeightRangePlacementModifier> MODIFIER_CODEC =
            HeightProvider.CODEC.fieldOf("height")
                    .xmap(ScaledHeightRangePlacementModifier::new,
                           mod -> mod.height);

    public static final PlacementModifierType<ScaledHeightRangePlacementModifier> TYPE =
            () -> MODIFIER_CODEC;

    private final HeightProvider height;

    public ScaledHeightRangePlacementModifier(HeightProvider height) {
        this.height = height;
    }

    @Override
    public Stream<BlockPos> getPositions(FeaturePlacementContext context,
                                         Random random,
                                         BlockPos pos) {
        // Sample from the HeightProvider (uses unscaled reference values from JSON)
        int y = this.height.get(random, context);

        // Resolve effective world height: auto-detect from world or manual config
        int worldTopY = context.getWorld().getBottomY() + context.getWorld().getHeight();
        int maxWorldHeight = HeightScaler.resolveMaxWorldHeight(worldTopY);
        y = HeightScaler.scaleY(y, maxWorldHeight);

        return Stream.of(pos.withY(y));
    }

    @Override
    public PlacementModifierType<?> getType() {
        return TYPE;
    }

    // ── Registration ────────────────────────────────────────────────────

    private static boolean registered = false;

    /**
     * Registers the {@code mountainores:scaled_height_range} placement modifier type.
     * Safe to call multiple times; only the first call has an effect.
     */
    public static void register() {
        if (registered) return;
        Registry.register(
                Registries.PLACEMENT_MODIFIER_TYPE,
                Identifier.of("mountainores", "scaled_height_range"),
                TYPE
        );
        registered = true;
    }
}
