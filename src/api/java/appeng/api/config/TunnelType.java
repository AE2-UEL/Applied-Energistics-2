/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 AlgorithmX2
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.config;


import appeng.api.AEApi;
import appeng.api.definitions.IItemDefinition;
import appeng.api.definitions.IParts;
import appeng.api.parts.IPartItem;
import com.google.common.base.Preconditions;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

import javax.annotation.Nonnull;
import java.util.function.Function;
import java.util.function.Supplier;

public enum TunnelType {
    ME(tryPartStack(IParts::p2PTunnelME)),
    IC2_POWER(tryPartStack(IParts::p2PTunnelEU)),
    FE_POWER(tryPartStack(IParts::p2PTunnelFE)),
    GTEU_POWER(tryPartStack(IParts::p2PTunnelGTEU)),
    REDSTONE(tryPartStack(IParts::p2PTunnelRedstone)),
    FLUID(tryPartStack(IParts::p2PTunnelFluids)),
    ITEM(tryPartStack(IParts::p2PTunnelItems)),
    LIGHT(tryPartStack(IParts::p2PTunnelLight));

    private ItemStack partItemStack;
    private Supplier<ItemStack> partItemStackSupplier;

    @Deprecated
    TunnelType() {
        this.partItemStack = ItemStack.EMPTY;
        this.partItemStackSupplier = null;
    }

	// Public facing.
    TunnelType(ItemStack partItemStack) {
        this.partItemStack = partItemStack;
        this.partItemStackSupplier = null;
    }

    // Work around things instantiating this class too early.
    TunnelType(Supplier<ItemStack> supplier) {
        this.partItemStack = null;
        this.partItemStackSupplier = supplier;
    }

    private static Supplier<ItemStack> tryPartStack(Function<IParts, IItemDefinition> supplier) {
        return () -> supplier.apply(AEApi.instance().definitions().parts()).maybeStack(1).orElse(ItemStack.EMPTY);
    }

    public static TunnelType registerTunnelType(@Nonnull String name, @Nonnull ItemStack partItemStack) {
        Preconditions.checkArgument(partItemStack.isEmpty() || partItemStack.getItem() instanceof IPartItem<?>,
                "Part item must be an instance of IPartItem");

        return EnumHelper.addEnum(TunnelType.class, name, new Class[]{ItemStack.class}, partItemStack);
    }

    public ItemStack getPartItemStack() {
        if (this.partItemStackSupplier != null) {
            this.partItemStack = this.partItemStackSupplier.get();
            this.partItemStackSupplier = null;
        }
        return partItemStack;
    }
}
