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

package appeng.parts.automation;


import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.IConfigManager;
import appeng.core.settings.TickRates;
import appeng.me.GridAccessException;
import appeng.tile.inventory.AppEngInternalAEInventory;
import appeng.util.InventoryAdaptor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;


public abstract class PartSharedItemBus extends PartUpgradeable implements IGridTickable {

    private final AppEngInternalAEInventory config = new AppEngInternalAEInventory(this, 9);
    private boolean lastRedstone = false;
    protected TickRates tickRates;

    /**
     * Indicates that an I/O bus in redstone pulse mode has observed a low to high redstone transition and is waiting to
     * act on this during its next tick.
     */
    private boolean pendingPulse = false;

    public PartSharedItemBus(final TickRates tickRates, final ItemStack is) {
        super(is);
        this.tickRates = tickRates;
        this.getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
    }

    @Override
    public RedstoneMode getRSMode() {
        return (RedstoneMode) this.getConfigManager().getSetting(Settings.REDSTONE_CONTROLLED);
    }

    private boolean isInPulseMode() {
        return getRSMode() == RedstoneMode.SIGNAL_PULSE;
    }

    @Override
    public void upgradesChanged() {
        this.updateRedstoneState();
    }

    @Override
    public void updateSetting(IConfigManager manager, Enum settingName, Enum newValue) {
        super.updateSetting(manager, settingName, newValue);

        this.updateRedstoneState();

        // Ensure we have an up-to-date last redstone state when pulse mode is activated to
        // correctly detect subsequent pulses
        if (isInPulseMode()) {
            this.lastRedstone = getHost().hasRedstone(this.getSide());
        }
    }

    @Override
    public void readFromNBT(final NBTTagCompound extra) {
        super.readFromNBT(extra);
        this.getConfig().readFromNBT(extra, "config");
        pendingPulse = isInPulseMode() && extra.getBoolean("pendingPulse");
    }

    @Override
    public void writeToNBT(final NBTTagCompound extra) {
        super.writeToNBT(extra);
        this.getConfig().writeToNBT(extra, "config");
        if (isInPulseMode() && pendingPulse) {
            extra.setBoolean("pendingPulse", true);
        }

    }

    @Override
    public IItemHandler getInventoryByName(final String name) {
        if (name.equals("config")) {
            return this.getConfig();
        }

        return super.getInventoryByName(name);
    }

    @Override
    public void onNeighborChanged(IBlockAccess w, BlockPos pos, BlockPos neighbor) {
        if (isInPulseMode()) {
            var hostIsPowered = this.getHost().hasRedstone(this.getSide());
            if (this.lastRedstone != hostIsPowered) {
                this.lastRedstone = hostIsPowered;
                if (this.lastRedstone && !this.pendingPulse) {
                    // Perform the action based on the pulse on the next tick
                    this.pendingPulse = true;
                    alertDevice();
                }
            }
        } else {
            // This handles waking up the bus if the adjacent redstone has changed
            this.updateRedstoneState();
        }
    }

    private void alertDevice() {
        try {
            this.getProxy().getTick().alertDevice(this.getProxy().getNode());
        } catch (final GridAccessException e) {
            //  Ciallo～(∠・ω< )⌒★
        }
    }

    protected InventoryAdaptor getHandler() {
        final TileEntity self = this.getHost().getTile();
        final TileEntity target = this.getTileEntity(self, self.getPos().offset(this.getSide().getFacing()));

        return InventoryAdaptor.getAdaptor(target, this.getSide().getFacing().getOpposite());
    }

    private TileEntity getTileEntity(final TileEntity self, final BlockPos pos) {
        final World w = self.getWorld();

        if (w.getChunkProvider().getLoadedChunk(pos.getX() >> 4, pos.getZ() >> 4) != null) {
            return w.getTileEntity(pos);
        }

        return null;
    }

    protected int availableSlots() {
        return Math.min(1 + this.getInstalledUpgrades(Upgrades.CAPACITY) * 4, this.getConfig().getSlots());
    }

    protected int calculateItemsToSend() {
        switch (this.getInstalledUpgrades(Upgrades.SPEED)) {
            default:
            case 0:
                return 1;
            case 1:
                return 8;
            case 2:
                return 32;
            case 3:
                return 64;
            case 4:
                return 96;
        }
    }

    /**
     * Checks if the bus can actually do something.
     * <p>
     * Currently this tests if the chunk for the target is actually loaded.
     *
     * @return true, if the the bus should do its work.
     */
    protected boolean canDoBusWork() {
        final TileEntity self = this.getHost().getTile();
        final BlockPos selfPos = self.getPos().offset(this.getSide().getFacing());
        final int xCoordinate = selfPos.getX();
        final int zCoordinate = selfPos.getZ();
        final World world = self.getWorld();

        return world != null && world.getChunkProvider().getLoadedChunk(xCoordinate >> 4, zCoordinate >> 4) != null;
    }

    private void updateRedstoneState() {
        // Clear the pending pulse flag if the upgrade is removed or the config is toggled off
        if (!this.isInPulseMode()) {
            this.pendingPulse = false;
        }
        try {
            if (!this.isSleeping()) {
                this.getProxy().getTick().wakeDevice(this.getProxy().getNode());
            } else {
                this.getProxy().getTick().sleepDevice(this.getProxy().getNode());
            }
        } catch (final GridAccessException e) {
            // :P
        }
    }

    @Override
    protected boolean isSleeping() {
        if (isInPulseMode() && this.pendingPulse) {
            return false;
        } else {
            return super.isSleeping();
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();

        // To correctly detect pulses (changes from low to high), we need to know the current state when we
        // are added to the world.
        this.lastRedstone = this.getHost().hasRedstone(this.getSide());
        // We may have observed a redstone pulse before, but were unable to act on it due to being unloaded before
        // we ticked again. Ensure that we do act on this pulse as soon as possible.
        if (pendingPulse) {
            alertDevice();
        }
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return new TickingRequest(TickRates.ExportBus.getMin(), TickRates.ExportBus.getMax(), this.isSleeping(), true);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        // Sometimes between being woken up and actually doing work, the config/redstone mode may have changed
        // put us back to sleep if that was the case
        if (this.isSleeping()) {
            return TickRateModulation.SLEEP;
        }

        // Reset a potential redstone pulse trigger
        this.pendingPulse = false;

        TickRateModulation hasDoneWork = this.doBusWork();

        // We may be back to sleep (i.e. in pulse mode)
        if (isSleeping()) {
            return TickRateModulation.SLEEP;
        } else {
            return hasDoneWork;
        }
    }

    protected abstract TickRateModulation doBusWork();

    AppEngInternalAEInventory getConfig() {
        return this.config;
    }
}
