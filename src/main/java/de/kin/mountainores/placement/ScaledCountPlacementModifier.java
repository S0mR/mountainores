package de.kin.mountainores.placement;

import com.mojang.serialization.MapCodec;
import de.kin.mountainores.HeightScaler;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.FeaturePlacementContext;
import net.minecraft.world.gen.placementmodifier.PlacementModifier;
import net.minecraft.world.gen.placementmodifier.PlacementModifierType;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * A drop-in replacement for {@code minecraft:count} that scales the attempt
 * count proportionally to the effective world height.
 *
 * <p>By default the world height is auto-detected from the dimension at
 * runtime.  If {@code autoDetectWorldHeight} is set to {@code false} in the
 * config, the manually configured {@code maxWorldHeight} is used instead.
 *
 * <p>When the world is shorter than the reference height (2032), above-ground
 * bands are compressed into fewer Y levels.  Without scaling the count, the
 * ore <em>density</em> (ores per stone block) would increase proportionally.
 * This modifier compensates by reducing the count by the same linear factor.
 *
 * <p>Usage in placed-feature JSON:
 * <pre>{@code
 * {
 *   "type": "mountainores:scaled_count",
 *   "count": 8
 * }
 * }</pre>
 *
 * <p>When the effective world height equals {@link HeightScaler#REFERENCE_HEIGHT},
 * this behaves identically to {@code minecraft:count}.
 */
public class ScaledCountPlacementModifier extends PlacementModifier {

    public static final MapCodec<ScaledCountPlacementModifier> MODIFIER_CODEC =
            IntProvider.VALUE_CODEC.fieldOf("count")
                    .xmap(ScaledCountPlacementModifier::new,
                           mod -> mod.count);

    public static final PlacementModifierType<ScaledCountPlacementModifier> TYPE =
            () -> MODIFIER_CODEC;

    private final IntProvider count;

    public ScaledCountPlacementModifier(IntProvider count) {
        this.count = count;
    }

    @Override
    public Stream<BlockPos> getPositions(FeaturePlacementContext context,
                                         Random random,
                                         BlockPos pos) {
        // Resolve effective world height: auto-detect from world or manual config
        int worldTopY = context.getWorld().getBottomY() + context.getWorld().getHeight();
        int maxWorldHeight = HeightScaler.resolveMaxWorldHeight(worldTopY);
        int rawCount = this.count.get(random);
        int scaledCount = HeightScaler.scaleCount(rawCount, maxWorldHeight);
        return IntStream.range(0, scaledCount).mapToObj(i -> pos);
    }

    @Override
    public PlacementModifierType<?> getType() {
        return TYPE;
    }

    // ── Registration ────────────────────────────────────────────────────

    private static boolean registered = false;

    /**
     * Registers the {@code mountainores:scaled_count} placement modifier type.
     * Safe to call multiple times; only the first call has an effect.
     */
    public static void register() {
        if (registered) return;
        Registry.register(
                Registries.PLACEMENT_MODIFIER_TYPE,
                Identifier.of("mountainores", "scaled_count"),
                TYPE
        );
        registered = true;
    }
}
