package de.kin.mountainores.feature;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class VariableOreFeature extends Feature<OreFeatureConfig> {
    
    private final int minSize;
    private final int maxSize;

    private final boolean shrinkOnExposure;
    private final float exposureShrinkFactor;
    private final boolean countFluidsAsExposure;

    public VariableOreFeature(Codec<OreFeatureConfig> configCodec, int minSize, int maxSize) {
        this(configCodec, minSize, maxSize, false, 1.0f, false);
    }

    public VariableOreFeature(
            Codec<OreFeatureConfig> configCodec,
            int minSize,
            int maxSize,
            boolean shrinkOnExposure,
            float exposureShrinkFactor,
            boolean countFluidsAsExposure
    ) {
        super(configCodec);
        this.minSize = minSize;
        this.maxSize = maxSize;

        this.shrinkOnExposure = shrinkOnExposure;
        this.exposureShrinkFactor = exposureShrinkFactor;
        this.countFluidsAsExposure = countFluidsAsExposure;
    }

    @Override
    public boolean generate(FeatureContext<OreFeatureConfig> context) {
        Random random = context.getRandom();
        StructureWorldAccess world = context.getWorld();
        OreFeatureConfig config = context.getConfig();
        BlockPos origin = context.getOrigin();

        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        // Zufällige Größe zwischen minSize und maxSize
        int targetBlocks = minSize + random.nextInt(maxSize - minSize + 1);

        // Generiere eine zusammenhängende Ader (nicht "gesprenkelte" Einzelblöcke)
        return generateConnectedVein(world, random, config, origin, targetBlocks, originChunkX, originChunkZ);
    }

    private boolean generateConnectedVein(
            StructureWorldAccess world,
            Random random,
            OreFeatureConfig config,
            BlockPos origin,
            int targetBlocks,
            int originChunkX,
            int originChunkZ
    ) {
        if (targetBlocks <= 0) {
            return false;
        }

        boolean generated = false;
        boolean shrinkApplied = false;

        // Startpunkt: versuche erst Ursprung, dann kleine Umgebung.
        List<BlockPos> placedPositions = new ArrayList<>(Math.min(targetBlocks, 64));
        int placed = 0;

        if (tryPlaceOre(world, random, config, origin)) {
            placedPositions.add(origin);
            placed++;
            generated = true;
        } else {
            BlockPos start = tryFindAndPlaceStart(world, random, config, origin, originChunkX, originChunkZ);
            if (start == null) {
                return false;
            }
            placedPositions.add(start);
            placed++;
            generated = true;
        }

        if (shrinkOnExposure && !shrinkApplied && isExposed(world, placedPositions.get(0))) {
            int shrunkTarget = Math.max(1, (int) Math.floor(targetBlocks * exposureShrinkFactor));
            targetBlocks = shrunkTarget;
            shrinkApplied = true;
        }

        Direction[] directions = Direction.values();
        int maxAttempts = Math.max(48, targetBlocks * 24);

        for (int attempts = 0; attempts < maxAttempts && placed < targetBlocks; attempts++) {
            BlockPos base = placedPositions.get(random.nextInt(placedPositions.size()));
            Direction direction = directions[random.nextInt(directions.length)];
            BlockPos next = base.offset(direction);

            if (!isInWorld(world, next) || !isInSameChunk(next, originChunkX, originChunkZ)) {
                continue;
            }

            if (tryPlaceOre(world, random, config, next)) {
                placedPositions.add(next);
                placed++;

                if (shrinkOnExposure && !shrinkApplied && isExposed(world, next)) {
                    int shrunkTarget = Math.max(1, (int) Math.floor(targetBlocks * exposureShrinkFactor));
                    targetBlocks = shrunkTarget;
                    shrinkApplied = true;
                }
            }
        }

        return generated;
    }

    private BlockPos tryFindAndPlaceStart(
            StructureWorldAccess world,
            Random random,
            OreFeatureConfig config,
            BlockPos origin,
            int originChunkX,
            int originChunkZ
    ) {
        // Kleine lokale Suche: erhöht die Chance, überhaupt in replaceables zu starten.
        for (int i = 0; i < 32; i++) {
            int dx = random.nextInt(5) - 2;
            int dy = random.nextInt(5) - 2;
            int dz = random.nextInt(5) - 2;
            BlockPos candidate = origin.add(dx, dy, dz);

            if (!isInWorld(world, candidate) || !isInSameChunk(candidate, originChunkX, originChunkZ)) {
                continue;
            }

            if (tryPlaceOre(world, random, config, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean tryPlaceOre(
            StructureWorldAccess world,
            Random random,
            OreFeatureConfig config,
            BlockPos pos
    ) {
        // Überprüfe alle Targets (z.B. Stein, Tiefenschiefer)
        for (OreFeatureConfig.Target target : config.targets) {
            if (target.target.test(world.getBlockState(pos), random)) {
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

    private boolean isExposed(StructureWorldAccess world, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos adjacentPos = pos.offset(direction);

            if (!isInWorld(world, adjacentPos)) {
                continue;
            }

            if (world.getBlockState(adjacentPos).isAir()) {
                return true;
            }

            if (countFluidsAsExposure && !world.getFluidState(adjacentPos).isEmpty()) {
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
}
