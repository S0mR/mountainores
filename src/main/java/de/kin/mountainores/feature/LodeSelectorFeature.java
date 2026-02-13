package de.kin.mountainores.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import de.kin.mountainores.HeightScaler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.List;

public class LodeSelectorFeature extends Feature<LodeSelectorFeature.Config> {

    public record Entry(
            int weight,
            int minY,
            int maxY,
            int minSize,
            int maxSize,
            OreFeatureConfig ore
    ) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("weight").forGetter(Entry::weight),
                Codec.INT.fieldOf("min_y").forGetter(Entry::minY),
                Codec.INT.fieldOf("max_y").forGetter(Entry::maxY),
                Codec.INT.fieldOf("min_size").forGetter(Entry::minSize),
                Codec.INT.fieldOf("max_size").forGetter(Entry::maxSize),
                OreFeatureConfig.CODEC.fieldOf("ore").forGetter(Entry::ore)
        ).apply(instance, Entry::new));
    }

    public record Config(
            List<Entry> entries,
            float exposureShrinkFactor
        ) implements FeatureConfig {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("entries").forGetter(Config::entries),
                Codec.FLOAT.optionalFieldOf("exposure_shrink_factor", 0.55f).forGetter(Config::exposureShrinkFactor)
        ).apply(instance, Config::new));
    }

    public LodeSelectorFeature(Codec<Config> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<Config> context) {
        Random random = context.getRandom();
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Config config = context.getConfig();

        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        Entry entry = pickEntry(config.entries(), random);
        if (entry == null) {
            return false;
        }

        if (entry.minSize() <= 0 || entry.maxSize() < entry.minSize()) {
            return false;
        }

        // Scale lode Y-range to the effective world height
        int worldTopY = world.getBottomY() + world.getHeight();
        int maxWorldHeight = HeightScaler.resolveMaxWorldHeight(worldTopY);
        int scaledMinY = HeightScaler.scaleY(entry.minY(), maxWorldHeight);
        int scaledMaxY = HeightScaler.scaleY(entry.maxY(), maxWorldHeight);

        int clampedMinY = Math.max(scaledMinY, world.getBottomY());
        int worldTopExclusive = world.getBottomY() + world.getHeight();
        int clampedMaxY = Math.min(scaledMaxY, worldTopExclusive - 1);
        if (clampedMinY > clampedMaxY) {
            return false;
        }

        int y = clampedMinY + random.nextInt(clampedMaxY - clampedMinY + 1);
        BlockPos lodeOrigin = new BlockPos(origin.getX(), y, origin.getZ());

        int targetBlocks = entry.minSize() + random.nextInt(entry.maxSize() - entry.minSize() + 1);
        float shrinkFactor = clamp01(config.exposureShrinkFactor());
        return generateOreCluster(world, random, entry.ore(), lodeOrigin, targetBlocks, shrinkFactor, originChunkX, originChunkZ);
    }

    private static Entry pickEntry(List<Entry> entries, Random random) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (Entry entry : entries) {
            if (entry.weight() > 0) {
                totalWeight += entry.weight();
            }
        }
        if (totalWeight <= 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Entry entry : entries) {
            if (entry.weight() <= 0) {
                continue;
            }
            cumulative += entry.weight();
            if (roll < cumulative) {
                return entry;
            }
        }

        return null;
    }

    private static boolean generateOreCluster(
            StructureWorldAccess world,
            Random random,
            OreFeatureConfig oreConfig,
            BlockPos origin,
            int targetBlocks,
            float exposureShrinkFactor,
            int originChunkX,
            int originChunkZ
    ) {
        if (targetBlocks <= 0) {
            return false;
        }

        boolean generated = false;
        boolean shrinkApplied = false;
        int placed = 0;

        double radius = Math.max(2.0, Math.cbrt((double) targetBlocks) * 2.0);
        int maxAttempts = Math.max(12, targetBlocks * 6);

        for (int attempts = 0; attempts < maxAttempts && placed < targetBlocks; attempts++) {
            BlockPos blockPos = randomPosInSphere(origin, radius, random);

            if (!isInWorld(world, blockPos) || !isInSameChunk(blockPos, originChunkX, originChunkZ)) {
                continue;
            }

            if (tryPlaceOre(world, random, oreConfig, blockPos)) {
                placed++;
                generated = true;

                if (!shrinkApplied && isExposedToAirOrFluid(world, blockPos)) {
                    int shrunkTarget = Math.max(1, (int) Math.floor(targetBlocks * exposureShrinkFactor));
                    targetBlocks = shrunkTarget;
                    shrinkApplied = true;
                }
            }
        }

        return generated;
    }

    private static BlockPos randomPosInSphere(BlockPos origin, double radius, Random random) {
        double radiusSq = radius * radius;

        double x = 0;
        double y = 0;
        double z = 0;

        for (int i = 0; i < 5; i++) {
            x = (random.nextDouble() * 2.0 - 1.0) * radius;
            y = (random.nextDouble() * 2.0 - 1.0) * radius;
            z = (random.nextDouble() * 2.0 - 1.0) * radius;

            if (x * x + y * y + z * z <= radiusSq) {
                break;
            }
        }

        return origin.add((int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
    }

    private static boolean tryPlaceOre(
            StructureWorldAccess world,
            Random random,
            OreFeatureConfig config,
            BlockPos pos
    ) {
        for (OreFeatureConfig.Target target : config.targets) {
            if (target.target.test(world.getBlockState(pos), random)) {
                // For lodes we generally keep discard chance at 0, but still respect the config.
                if (shouldDiscardDueToAirExposure(world, pos, config, random)) {
                    return false;
                }

                world.setBlockState(pos, target.state, 2);
                return true;
            }
        }
        return false;
    }

    private static boolean shouldDiscardDueToAirExposure(
            StructureWorldAccess world,
            BlockPos pos,
            OreFeatureConfig config,
            Random random
    ) {
        float discardChance = config.discardOnAirChance;
        if (discardChance <= 0.0f) {
            return false;
        }

        if (!isExposedToAir(world, pos)) {
            return false;
        }

        return random.nextFloat() < discardChance;
    }

    private static boolean isExposedToAir(StructureWorldAccess world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.offset(direction);
            if (!isInWorld(world, adjacentPos)) {
                continue;
            }
            if (world.getBlockState(adjacentPos).isAir()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isExposedToAirOrFluid(StructureWorldAccess world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.offset(direction);

            if (!isInWorld(world, adjacentPos)) {
                continue;
            }

            if (world.getBlockState(adjacentPos).isAir()) {
                return true;
            }

            if (!world.getFluidState(adjacentPos).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInWorld(StructureWorldAccess world, BlockPos pos) {
        int bottomY = world.getBottomY();
        int topYExclusive = bottomY + world.getHeight();
        int y = pos.getY();
        return y >= bottomY && y < topYExclusive;
    }

    private static boolean isInSameChunk(BlockPos pos, int originChunkX, int originChunkZ) {
        return (pos.getX() >> 4) == originChunkX && (pos.getZ() >> 4) == originChunkZ;
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }
}
