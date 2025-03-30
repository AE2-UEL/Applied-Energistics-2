package appeng.tile.storage;

import appeng.api.AEApi;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.DriveWatcher;
import appeng.tile.inventory.AppEngCellInventory;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class DriveCellManager {
    private final TileDrive drive;
    private final AppEngCellInventory inv;
    private final ICellHandler[] handlersBySlot;
    private final DriveWatcher<IAEItemStack>[] invBySlot;
    private final Map<IStorageChannel<? extends IAEStack<?>>, List<IMEInventoryHandler>> inventoryHandlers;

    public DriveCellManager(TileDrive drive, AppEngCellInventory inv,
                            ICellHandler[] handlersBySlot, DriveWatcher<IAEItemStack>[] invBySlot,
                            Map<IStorageChannel<? extends IAEStack<?>>, List<IMEInventoryHandler>> inventoryHandlers) {
        this.drive = drive;
        this.inv = inv;
        this.handlersBySlot = handlersBySlot;
        this.invBySlot = invBySlot;
        this.inventoryHandlers = inventoryHandlers;
    }

    public void updateState(boolean isCached) {
        if (!isCached) {
            final Collection<IStorageChannel<? extends IAEStack<?>>> storageChannels =
                    AEApi.instance().storage().storageChannels();
            storageChannels.forEach(channel -> this.inventoryHandlers.put(channel, new ArrayList<>(10)));

            double power = 2.0;

            for (int x = 0; x < this.inv.getSlots(); x++) {
                processCellSlot(x, storageChannels, power);
            }

            drive.getProxy().setIdlePowerUsage(power);
        }
    }

    private void processCellSlot(int slot, Collection<IStorageChannel<? extends IAEStack<?>>> channels, double power) {
        final ItemStack is = this.inv.getStackInSlot(slot);
        this.invBySlot[slot] = null;
        this.handlersBySlot[slot] = null;

        if (!is.isEmpty()) {
            this.handlersBySlot[slot] = AEApi.instance().registries().cell().getHandler(is);

            if (this.handlersBySlot[slot] != null) {
                for (IStorageChannel<? extends IAEStack<?>> channel : channels) {
                    ICellInventoryHandler cell = this.handlersBySlot[slot].getCellInventory(is, drive, channel);

                    if (cell != null) {
                        this.inv.setHandler(slot, cell);
                        power += this.handlersBySlot[slot].cellIdleDrain(is, cell);

                        final DriveWatcher<IAEItemStack> ih = new DriveWatcher(cell, is,
                                this.handlersBySlot[slot], drive);
                        ih.setPriority(drive.getPriority());
                        this.invBySlot[slot] = ih;
                        this.inventoryHandlers.get(channel).add(ih);
                        break;
                    }
                }
            }
        }
    }
}