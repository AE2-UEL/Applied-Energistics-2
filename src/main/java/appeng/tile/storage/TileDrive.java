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

package appeng.tile.storage;


import appeng.api.AEApi;
import appeng.api.implementations.tiles.IChestOrDrive;
import appeng.api.networking.GridFlags;
import appeng.api.networking.events.MENetworkCellArrayUpdate;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.*;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IPriorityHost;
import appeng.me.GridAccessException;
import appeng.me.helpers.MachineSource;
import appeng.me.storage.DriveWatcher;
import appeng.tile.grid.AENetworkInvTile;
import appeng.tile.inventory.AppEngCellInventory;
import appeng.util.Platform;
import appeng.util.inv.InvOperation;
import appeng.util.inv.filter.IAEItemFilter;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;

import java.io.IOException;
import java.util.*;


public class TileDrive extends AENetworkInvTile implements IChestOrDrive, IPriorityHost {

    private final AppEngCellInventory inv = new AppEngCellInventory(this, 10);
    private final ICellHandler[] handlersBySlot = new ICellHandler[10];
    private final DriveWatcher<IAEItemStack>[] invBySlot = new DriveWatcher[10];
    private final IActionSource mySrc;
    private boolean isCached = false;
    private final Map<IStorageChannel<? extends IAEStack<?>>, List<IMEInventoryHandler>> inventoryHandlers;
    private int priority = 0;
    private boolean wasActive = false;
    private final DriveCellManager cellManager;

    /**
     * The state of all cells inside a drive as bitset, using the following format.
     * <p>
     * Bit 29-0: 3 bits as state of each cell with the cell in slot 0 located in the 3 least significant bits.
     * <p>
     * Cell states:
     * Bit 2-0: cell status, representing {@link appeng.block.storage.DriveSlotState}.
     */
    private int cellState = 0;
    private boolean powered;
    // bit index corresponds to cell index
    private int blinking;

    public TileDrive() {
        this.mySrc = new MachineSource(this);
        this.getProxy().setFlags(GridFlags.REQUIRE_CHANNEL);
        this.inv.setFilter(new CellValidInventoryFilter());
        this.inventoryHandlers = new IdentityHashMap<>();
        this.cellManager = new DriveCellManager(this, inv, handlersBySlot, invBySlot, inventoryHandlers);
    }

    @Override
    protected void writeToStream(final ByteBuf data) throws IOException {
        super.writeToStream(data);

        int newState = 0;
        for (int x = 0; x < this.getCellCount(); x++) {
            newState |= (this.getCellStatus(x) << (3 * x));
        }

        data.writeInt(newState);
        data.writeBoolean(this.getProxy().isActive());
        data.writeInt(this.blinking);
    }

    @Override
    protected boolean readFromStream(final ByteBuf data) throws IOException {
        final boolean c = super.readFromStream(data);
        final int oldCellState = this.cellState;
        final boolean oldPowered = this.powered;
        final int oldBlinking = this.blinking;
        this.cellState = data.readInt();
        this.powered = data.readBoolean();
        this.blinking = data.readInt();
        return oldCellState != this.cellState || oldPowered != this.powered || oldBlinking != this.blinking || c;
    }

    @Override
    public int getCellCount() {
        return 10;
    }

    @Override
    public int getCellStatus(final int slot) {
        if (Platform.isClient()) {
            return (this.cellState >> (slot * 3)) & 0b111;
        }

        final DriveWatcher handler = this.invBySlot[slot];
        if (handler == null) {
            return 0;
        }

        return handler.getStatus();
    }

    @Override
    public boolean isPowered() {
        if (Platform.isClient()) {
            return this.powered;
        }

        return this.getProxy().isActive();
    }

    @Override
    public boolean isCellBlinking(final int slot) {
        return (this.blinking & (1 << slot)) == 1;
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.isCached = false;
        this.priority = data.getInteger("priority");
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("priority", this.priority);
        return data;
    }

    @MENetworkEventSubscribe
    public void powerRender(final MENetworkPowerStatusChange c) {
        this.recalculateDisplay();
    }

    private void recalculateDisplay() {
        final boolean currentActive = this.getProxy().isActive();
        final int oldCellState = this.cellState;
        final boolean oldPowered = this.powered;

        this.powered = currentActive;

        if (this.wasActive != currentActive) {
            this.wasActive = currentActive;
            try {
                this.getProxy().getGrid().postEvent(new MENetworkCellArrayUpdate());
            } catch (final GridAccessException e) {
                // :P
            }
        }

        for (int x = 0; x < this.getCellCount(); x++) {
            cellState |= (this.getCellStatus(x) << (3 * x));
        }

        if (oldCellState != this.cellState || oldPowered != this.powered) {
            this.markForUpdate();
        }
    }

    @MENetworkEventSubscribe
    public void channelRender(final MENetworkChannelsChanged c) {
        this.recalculateDisplay();
    }

    @Override
    public AECableType getCableConnectionType(final AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public IItemHandler getInternalInventory() {
        return this.inv;
    }

    @Override
    public void onChangeInventory(final IItemHandler inv, final int slot, final InvOperation mc, final ItemStack removed, final ItemStack added) {
        if (this.isCached) {
            this.isCached = false; // recalculate the storage cell.
            this.updateState();
        }

        try {
            if (this.getProxy().isActive()) {
                final IStorageGrid gs = this.getProxy().getStorage();
                Platform.postChanges(gs, removed, added, this.mySrc);
            }
            this.getProxy().getGrid().postEvent(new MENetworkCellArrayUpdate());
        } catch (final GridAccessException ignored) {
        }

        this.markForUpdate();
    }

    private void updateState() {
        if (!this.isCached) {
            this.cellManager.updateState(this.isCached);
            this.isCached = true;
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updateState();
    }

    @Override
    public List<IMEInventoryHandler> getCellArray(final IStorageChannel channel) {
        this.updateState();
        return this.inventoryHandlers.get(channel);
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(final int newValue) {
        this.priority = newValue;
        this.saveChanges();

        this.isCached = false; // recalculate the storage cell.
        this.updateState();

        try {
            this.getProxy().getGrid().postEvent(new MENetworkCellArrayUpdate());
        } catch (final GridAccessException e) {
            // :P
        }
    }

    @Override
    public void blinkCell(final int slot) {
        this.blinking |= (1 << slot);

        this.recalculateDisplay();
    }

    @Override
    public void saveChanges(final ICellInventory<?> cellInventory) {
        this.world.markChunkDirty(this.pos, this);
    }

    private class CellValidInventoryFilter implements IAEItemFilter {

        @Override
        public boolean allowExtract(IItemHandler inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(IItemHandler inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && AEApi.instance().registries().cell().isCellHandled(stack);
        }

    }

    @Override
    public ItemStack getItemStackRepresentation() {
        return AEApi.instance().definitions().blocks().drive().maybeStack(1).orElse(ItemStack.EMPTY);
    }

    @Override
    public GuiBridge getGuiBridge() {
        return GuiBridge.GUI_DRIVE;
    }
}
