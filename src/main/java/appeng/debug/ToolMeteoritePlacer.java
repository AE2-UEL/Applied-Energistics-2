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

package appeng.debug;


import appeng.items.AEBaseItem;
import appeng.util.Platform;
import appeng.worldgen.meteorite.MeteorConstants;
import appeng.worldgen.meteorite.MeteoritePlacer;
import appeng.worldgen.meteorite.debug.MeteoriteSpawner;
import appeng.worldgen.meteorite.settings.CraterType;
import appeng.worldgen.meteorite.settings.PlacedMeteoriteSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.jetbrains.annotations.NotNull;


public class ToolMeteoritePlacer extends AEBaseItem {
    private static final String MODE_TAG = "mode";

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, @NotNull EntityPlayer player, @NotNull EnumHand hand) {
        if (world.isRemote) {
            return ActionResult.newResult(EnumActionResult.PASS, player.getHeldItem(hand));
        }
        if (hand == EnumHand.MAIN_HAND && player.isSneaking()) {
            final ItemStack itemStack = player.getHeldItemMainhand();
            NBTTagCompound nbt = Platform.openNbtData(itemStack);
            if (nbt.hasKey(MODE_TAG, net.minecraftforge.common.util.Constants.NBT.TAG_BYTE)) {
                final byte mode = nbt.getByte("mode");
                nbt.setByte(MODE_TAG, (byte) ((mode + 1) % CraterType.values().length));
            } else {
                nbt.setByte(MODE_TAG, (byte) CraterType.NORMAL.ordinal());
            }
            var craterType = getCraterType(itemStack);

            player.sendStatusMessage(new TextComponentString(craterType.name()), true);
            return ActionResult.newResult(EnumActionResult.SUCCESS, itemStack);
        }

        return super.onItemRightClick(world, player, hand);
    }

    private CraterType getCraterType(ItemStack stack) {
        NBTTagCompound nbt = Platform.openNbtData(stack).copy();
        var typeIndex = nbt.getByte(MODE_TAG);
        return CraterType.values()[typeIndex];
    }

    @Override
    @NotNull
    public EnumActionResult onItemUseFirst(final @NotNull EntityPlayer player, final @NotNull World world, final @NotNull BlockPos pos, final @NotNull EnumFacing side, final float hitX, final float hitY, final float hitZ, final @NotNull EnumHand hand) {
        if (world.isRemote) {
            return EnumActionResult.PASS;
        }
        ItemStack heldStack = player.getHeldItemMainhand();
        if (!heldStack.hasTagCompound()) {
            var nbt = new NBTTagCompound();
            nbt.setByte(MODE_TAG, (byte) CraterType.NONE.ordinal());
            heldStack.setTagCompound(nbt);
        }

        // See MapGenMeteorite.Start for original code
        float coreRadius = world.rand.nextFloat()
                * (MeteorConstants.MAX_METEOR_RADIUS - MeteorConstants.MIN_METEOR_RADIUS)
                + MeteorConstants.MIN_METEOR_RADIUS;
        boolean pureCrater = world.rand.nextFloat() > 0.5f;
        CraterType craterType = getCraterType(heldStack);

        MeteoriteSpawner spawner = new MeteoriteSpawner();
        PlacedMeteoriteSettings spawned = spawner.trySpawnMeteorite(world, pos, coreRadius, craterType, pureCrater);

        if (spawned == null) {
            player.sendMessage(new TextComponentString("Un-suitable Location."));
            return EnumActionResult.FAIL;
        }

        // Assume maximum size
        int range = (int) Math.ceil((coreRadius * 2 + 5) * 5f);

        StructureBoundingBox boundingBox = new StructureBoundingBox(
                pos.getX() - range, pos.getY() - 10, pos.getZ() - range,
                pos.getX() + range, pos.getY() + 10, pos.getZ() + range);

        MeteoritePlacer.place(world, spawned, boundingBox, true);

        player.sendMessage(new TextComponentString("Spawned at y=" + spawned.getPos().getY() + " range=" + range));

        return EnumActionResult.SUCCESS;
    }
}
