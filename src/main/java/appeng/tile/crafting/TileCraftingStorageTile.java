/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
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

package appeng.tile.crafting;


import appeng.api.AEApi;
import appeng.api.definitions.IBlocks;
import appeng.block.crafting.BlockCraftingUnit;
import net.minecraft.item.ItemStack;

import java.util.Optional;


public class TileCraftingStorageTile extends TileCraftingTile {
    private static final int KILO_SCALAR = 1024;

    @Override
    protected ItemStack getItemFromTile(final Object obj) {
        final int storageKiloBytes = ((TileCraftingTile) obj).getStorageBytes() / KILO_SCALAR;

        return CraftingStorageType.fromKiloBytes(storageKiloBytes)
                .flatMap(CraftingStorageType::getItemStack)
                .orElseGet(() -> super.getItemFromTile(obj));
    }

    @Override
    public boolean isAccelerator() {
        return false;
    }

    @Override
    public boolean isStorage() {
        return true;
    }

    @Override
    public int getStorageBytes() {
        if (this.world == null || this.notLoaded() || this.isInvalid()) {
            return 0;
        }

        final int kiloBytes = this.getStorageBytes() / KILO_SCALAR;
        return CraftingStorageType.fromKiloBytes(kiloBytes)
                .map(CraftingStorageType::getStorageBytes)
                .orElse(0);
    }
}
