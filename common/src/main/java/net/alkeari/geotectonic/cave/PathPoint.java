package net.alkeari.geotectonic.cave;

import net.minecraft.core.BlockPos;

public record PathPoint(BlockPos center, float widthRadius, float heightRadius) {}
