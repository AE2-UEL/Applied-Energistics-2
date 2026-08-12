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

package appeng.worldgen.meteorite;


import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.BlockFlags;


public class MeteoriteBlockPutter {
    private boolean update;

    public MeteoriteBlockPutter(boolean update) {
        this.update = update;
    }

    public void putSilent(final World w, final BlockPos pos, final Block blk, final IBlockState originalState) {
        var oldUpdate = update;
        update = false;
        put(w, pos, blk, originalState);
        update = oldUpdate;
    }

    public void put(final World w, final BlockPos pos, final Block blk, final IBlockState originalState) {
        Block original = originalState.getBlock();
        if (original == blk) {
            return;
        }
        put(w, pos, blk.getDefaultState(), originalState);
    }

    public void put(final World w, final BlockPos pos, final Block blk) {
        var originalState = w.getBlockState(pos);
        put(w, pos, blk, originalState);
    }

    public void put(final World w, final BlockPos pos, final IBlockState state, final IBlockState originalState) {
        if (isUnbreakable(w, pos, originalState) || state == originalState) {
            return;
        }
        var flags = BlockFlags.DEFAULT | BlockFlags.NO_OBSERVERS;
        if (!update) {
            flags &= ~BlockFlags.NOTIFY_NEIGHBORS;
        }

        w.setBlockState(pos, state, flags);
    }

    public void put(final World w, final BlockPos pos, final IBlockState state) {
        var originalState = w.getBlockState(pos);
        put(w, pos, state, originalState);
    }

    private boolean isUnbreakable(final World w, final BlockPos pos, final IBlockState state) {
        return state.getBlock() == Blocks.BEDROCK || state.getBlockHardness(w, pos) < 0.0F;
    }
}
