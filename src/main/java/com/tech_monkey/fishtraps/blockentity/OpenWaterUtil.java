package com.tech_monkey.fishtraps.blockentity;

import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public final class OpenWaterUtil {
    private OpenWaterUtil() {}

    public static boolean isTrapOpenWater(ServerWorld world, BlockPos trapPos) {
        // Check 1 block above
        if (!isWater(world, trapPos.add(0, 1, 0))) return false;
        
        // Check 2 blocks in each cardinal direction (at same level)
        // North (negative Z)
        if (!isWater(world, trapPos.add(0, 0, -1))) return false;
        if (!isWater(world, trapPos.add(0, 0, -2))) return false;
        
        // South (positive Z)
        if (!isWater(world, trapPos.add(0, 0, 1))) return false;
        if (!isWater(world, trapPos.add(0, 0, 2))) return false;
        
        // East (positive X)
        if (!isWater(world, trapPos.add(1, 0, 0))) return false;
        if (!isWater(world, trapPos.add(2, 0, 0))) return false;
        
        // West (negative X)
        if (!isWater(world, trapPos.add(-1, 0, 0))) return false;
        if (!isWater(world, trapPos.add(-2, 0, 0))) return false;

        return true;
    }

    private static boolean isWater(ServerWorld world, BlockPos pos) {
        FluidState fluid = world.getFluidState(pos);
        return fluid != null && fluid.isIn(FluidTags.WATER);
    }
}