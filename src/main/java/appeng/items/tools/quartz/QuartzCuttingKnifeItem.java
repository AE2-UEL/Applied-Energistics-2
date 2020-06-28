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

package appeng.items.tools.quartz;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import appeng.api.features.AEFeature;
import appeng.api.implementations.guiobjects.IGuiItem;
import appeng.api.implementations.guiobjects.IGuiItemObject;
import appeng.container.ContainerLocator;
import appeng.container.ContainerOpener;
import appeng.container.implementations.QuartzKnifeContainer;
import appeng.items.AEBaseItem;
import appeng.items.contents.QuartzKnifeObj;
import appeng.util.Platform;

public class QuartzCuttingKnifeItem extends AEBaseItem implements IGuiItem {
    private final AEFeature type;

    public QuartzCuttingKnifeItem(Item.Settings props, final AEFeature type) {
        super(props);
        this.type = type;
    }

    @Override
    public ActionResult onItemUse(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (Platform.isServer() && player != null) {
            ContainerOpener.openContainer(QuartzKnifeContainer.TYPE, context.getPlayer(),
                    ContainerLocator.forItemUseContext(context));
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> onItemRightClick(final World w, final PlayerEntity p, final Hand hand) {
        if (Platform.isServer()) {
            ContainerOpener.openContainer(QuartzKnifeContainer.TYPE, p, ContainerLocator.forHand(p, hand));
        }
        p.swingArm(hand);
        return new TypedActionResult<>(ActionResult.SUCCESS, p.getStackInHand(hand));
    }

    @Override
    public boolean getIsRepairable(final ItemStack a, final ItemStack b) {
        return Platform.canRepair(this.type, a, b);
    }

    @Override
    public ItemStack getRecipeRemainder(final ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        copy.setDamage(itemStack.getDamage() + 1);

        return copy;
    }

    @Override
    public boolean hasRecipeRemainder(final ItemStack stack) {
        return true;
    }

    @Override
    public IGuiItemObject getGuiObject(final ItemStack is, int playerInventorySlot, final World world,
            final BlockPos pos) {
        return new QuartzKnifeObj(is);
    }
}
