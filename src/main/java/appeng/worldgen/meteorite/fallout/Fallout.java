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
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Random;


public class Fallout {
    protected final Random random;
    private final MeteoriteBlockPutter putter;
    private final IBlockDefinition skyStoneDefinition;

    public Fallout(final MeteoriteBlockPutter putter, final IBlockDefinition skyStoneDefinition, final Random random) {
        this.putter = putter;
        this.skyStoneDefinition = skyStoneDefinition;
        this.random = random;
    }

    public int adjustCrater() {
        return 0;
    }

    public void getRandomFall(final World w, final BlockPos pos) {
        var r = this.random.nextFloat();
        if (r > 0.9) {
            this.putter.put(w, pos, Blocks.STONE);
        } else if (r > 0.8) {
            this.putter.put(w, pos, Blocks.COBBLESTONE);
        } else if (r > 0.7) {
            this.putter.put(w, pos, w.getBiome(pos).fillerBlock);
        } else {
            this.putter.put(w, pos, Blocks.GRAVEL);
        }
    }

    public void getRandomInset(final World w, final BlockPos pos) {
        var r = this.random.nextFloat();
        if (r > 0.9) {
            this.putter.put(w, pos, Blocks.COBBLESTONE);
        } else if (r > 0.8) {
            this.putter.put(w, pos, Blocks.STONE);
        } else if (r > 0.7) {
            this.putter.put(w, pos, w.getBiome(pos).topBlock);
        } else if (r > 0.6) {
            this.skyStoneDefinition.maybeBlock().ifPresent(block -> this.putter.put(w, pos, block));
        } else if (r > 0.5) {
            this.putter.put(w, pos, Blocks.GRAVEL);
        } else {
            this.putter.put(w, pos, Platform.AIR_BLOCK);
        }
    }
}
