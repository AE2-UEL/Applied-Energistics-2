package appeng.container.implementations;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.IUpgradeableCellContainer;
import appeng.api.networking.security.IActionHost;
import appeng.container.interfaces.IInventorySlotAware;
import appeng.container.slot.SlotRestrictedInput;
import appeng.core.AEConfig;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.parts.automation.StackUpgradeInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.inv.IAEAppEngInventory;
import appeng.util.inv.InvOperation;
import baubles.api.BaublesApi;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;

public class ContainerWirelessInterfaceTerminal extends ContainerInterfaceTerminal implements IInventorySlotAware, IUpgradeableCellContainer, IAEAppEngInventory {
    private final WirelessTerminalGuiObject wirelessTerminalGUIObject;
    private final int slot;
    private double powerMultiplier = 0.5;
    private int ticks = 0;
    protected AppEngInternalInventory upgrades;
    protected SlotRestrictedInput magnetSlot;

    public ContainerWirelessInterfaceTerminal(InventoryPlayer ip, WirelessTerminalGuiObject guiObject) {
        super(ip, guiObject,false);

        if (guiObject != null) {
            final int slotIndex = guiObject.getInventorySlot();
            if (!guiObject.isBaubleSlot()) {
                this.lockPlayerInventorySlot(slotIndex);
            }
            this.slot = slotIndex;
        } else {
            this.lockPlayerInventorySlot(ip.currentItem);
            this.slot = -1;
        }

        this.bindPlayerInventory(ip,0,0);

        this.wirelessTerminalGUIObject = guiObject;

        upgrades = new StackUpgradeInventory(wirelessTerminalGUIObject.getItemStack(), this, 2);
        this.loadFromNBT();
        setupUpgrades();

    }

    @Override
    public void detectAndSendChanges() {
        if (Platform.isServer()) {
            final ItemStack currentItem;
            if (wirelessTerminalGUIObject.isBaubleSlot()) {
                currentItem = BaublesApi.getBaublesHandler(this.getPlayerInv().player).getStackInSlot(this.slot);
            } else {
                currentItem = this.slot < 0 ? this.getPlayerInv().getCurrentItem() : this.getPlayerInv().getStackInSlot(this.slot);
            }

            if (currentItem.isEmpty()) {
                this.setValidContainer(false);
            } else if (!this.wirelessTerminalGUIObject.getItemStack().isEmpty() && currentItem != this.wirelessTerminalGUIObject.getItemStack()) {
                if (ItemStack.areItemsEqual(this.wirelessTerminalGUIObject.getItemStack(), currentItem)) {
                    if (wirelessTerminalGUIObject.isBaubleSlot()) {
                        BaublesApi.getBaublesHandler(this.getPlayerInv().player).setStackInSlot(this.slot, this.wirelessTerminalGUIObject.getItemStack());
                    } else {
                        this.getPlayerInv().setInventorySlotContents(this.slot, this.wirelessTerminalGUIObject.getItemStack());
                    }
                } else {
                    this.setValidContainer(false);
                }
            }

            // drain 1 ae t
            this.ticks++;
            if (this.ticks > 10) {
                double ext = this.wirelessTerminalGUIObject.extractAEPower(this.getPowerMultiplier() * this.ticks, Actionable.MODULATE, PowerMultiplier.CONFIG);
                if (ext < this.getPowerMultiplier() * this.ticks) {
                    if (Platform.isServer() && this.isValidContainer()) {
                        this.getPlayerInv().player.sendMessage(PlayerMessages.DeviceNotPowered.get());
                    }

                    this.setValidContainer(false);
                }
                this.ticks = 0;
            }

            if (!this.wirelessTerminalGUIObject.rangeCheck()) {
                if (Platform.isServer() && this.isValidContainer()) {
                    this.getPlayerInv().player.sendMessage(PlayerMessages.OutOfRange.get());
                }

                this.setValidContainer(false);
            } else {
                this.setPowerMultiplier(AEConfig.instance().wireless_getDrainRate(this.wirelessTerminalGUIObject.getRange()));
            }

            super.detectAndSendChanges();
        }
    }

    @Override
    public ItemStack slotClick(int slotId, int dragType, ClickType clickTypeIn, @NotNull EntityPlayer player) {
        if (slotId >= 0 && slotId < this.inventorySlots.size()) {
            if (clickTypeIn == ClickType.PICKUP && dragType == 1) {
                if (this.inventorySlots.get(slotId) == magnetSlot) {
                    ItemStack itemStack = magnetSlot.getStack();
                    if (!magnetSlot.getStack().isEmpty()) {
                        NBTTagCompound tag = itemStack.getTagCompound();
                        if (tag == null) {
                            tag = new NBTTagCompound();
                        }
                        if (tag.hasKey("enabled")) {
                            boolean e = tag.getBoolean("enabled");
                            tag.setBoolean("enabled", !e);
                        } else {
                            tag.setBoolean("enabled", false);
                        }
                        magnetSlot.getStack().setTagCompound(tag);
                        magnetSlot.onSlotChanged();
                        return ItemStack.EMPTY;
                    }
                }
            }
        }
        return super.slotClick(slotId, dragType, clickTypeIn, player);
    }

    private double getPowerMultiplier() {
        return this.powerMultiplier;
    }

    void setPowerMultiplier(final double powerMultiplier) {
        this.powerMultiplier = powerMultiplier;
    }

    @Override
    protected IActionHost getActionHost() {
        return wirelessTerminalGUIObject;
    }

    @Override
    public int getInventorySlot() {
        return wirelessTerminalGUIObject.getInventorySlot();
    }

    @Override
    public boolean isBaubleSlot() {
        return wirelessTerminalGUIObject.isBaubleSlot();
    }

    @Override
    public int availableUpgrades() {
        return 1;
    }

    @Override
    public void setupUpgrades() {
        if (wirelessTerminalGUIObject != null) {
            for (int upgradeSlot = 0; upgradeSlot < availableUpgrades(); upgradeSlot++) {
                this.magnetSlot = new SlotRestrictedInput(SlotRestrictedInput.PlacableItemType.UPGRADES, upgrades, upgradeSlot, 183, -1 + upgradeSlot * 18, this.getInventoryPlayer());
                this.magnetSlot.setNotDraggable();
                this.addSlotToContainer(magnetSlot);
            }
        }
    }

    private void loadFromNBT() {
        NBTTagCompound data = wirelessTerminalGUIObject.getItemStack().getTagCompound();
        if (data != null) {
            upgrades.readFromNBT(wirelessTerminalGUIObject.getItemStack().getTagCompound().getCompoundTag("upgrades"));
        }
    }

    @Override
    public void saveChanges() {
        if (Platform.isServer()) {
            NBTTagCompound tag = new NBTTagCompound();

            this.upgrades.writeToNBT(tag, "upgrades");

            this.wirelessTerminalGUIObject.saveChanges(tag);
        }
    }

    @Override
    public void onChangeInventory(IItemHandler inv, int slot, InvOperation mc, ItemStack removedStack, ItemStack newStack) {

    }
}
