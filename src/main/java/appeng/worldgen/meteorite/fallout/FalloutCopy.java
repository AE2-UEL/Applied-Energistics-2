/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2015, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.worldgen.meteorite.fallout;


import appeng.api.definitions.IBlockDefinition;
import appeng.util.Platform;
import appeng.worldgen.meteorite.MeteoriteBlockPutter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Random;


public class FalloutCopy extends Fallout {
    private static final double SPECIFIED_BLOCK_THRESHOLD = 0.9;
    private static final double AIR_BLOCK_THRESHOLD = 0.8;
    private static final double BLOCK_THRESHOLD_STEP = 0.1;

    private final IBlockState block;
    private final MeteoriteBlockPutter putter;

    public FalloutCopy(
            final World w,
            final BlockPos pos,
            final MeteoriteBlockPutter putter,
            final IBlockDefinition skyStoneDefinition,
            final Random random) {
        super(putter, skyStoneDefinition, random);
        this.putter = putter;
        var biome = w.getBiome(pos);
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.MESA)) {
            block = Blocks.HARDENED_CLAY.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
            block = Blocks.SNOW.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SANDY)) {
            block = Blocks.SAND.getDefaultState();
        } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.PLAINS)
                || BiomeDictionary.hasType(biome, BiomeDictionary.Type.FOREST)) {
            block = Blocks.DIRT.getDefaultState();
        } else {
            block = Blocks.COBBLESTONE.getDefaultState();
        }
    }

    @Override
    public void getRandomFall(final World w, final BlockPos pos) {
        var a = this.random.nextFloat();
        if (a > SPECIFIED_BLOCK_THRESHOLD) {
            this.putter.put(w, pos, this.block);
        } else {
            this.getOther(w, pos, a);
        }
    }

    public void getOther(final World w, final BlockPos pos, final double a) {
    }

    @Override
    public void getRandomInset(final World w, final BlockPos pos) {
        var a = this.random.nextFloat();
        if (a > SPECIFIED_BLOCK_THRESHOLD) {
            this.putter.put(w, pos, this.block);
        } else if (a > AIR_BLOCK_THRESHOLD) {
            this.putter.put(w, pos, Platform.AIR_BLOCK);
        } else {
            this.getOther(w, pos, a - BLOCK_THRESHOLD_STEP);
        }
    }
}
