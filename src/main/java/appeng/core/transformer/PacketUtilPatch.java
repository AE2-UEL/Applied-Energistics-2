/*
 * Copyright (c) 2018, 2020 Adrian Siekierka
 *
 * This file is part of StackUp.
 *
 * StackUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * StackUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with StackUp.  If not, see <http://www.gnu.org/licenses/>.
 */
package appeng.core.transformer;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

public class PacketUtilPatch {
    public static void writeItemStackFromClientToServer(PacketBuffer buffer, ItemStack stack) {
        if (stack.isEmpty()) {
            buffer.writeShort(-1);
        } else {
            buffer.writeShort(Item.getIdFromItem(stack.getItem()));
            if (stack.getCount() >= 0 && stack.getCount() <= 64) {
                buffer.writeByte(stack.getCount());
            } else {
                buffer.writeByte(-42);
                buffer.writeInt(stack.getCount());
            }
            buffer.writeShort(stack.getMetadata());
            NBTTagCompound tag = null;

            if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
                tag = stack.getTagCompound();
            }

            buffer.writeCompoundTag(tag);
        }
    }
}