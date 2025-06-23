package appeng.client.gui.implementations;

import appeng.helpers.WirelessTerminalGuiObject;
import net.minecraft.client.gui.Gui;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiWirelessInterfaceTerminal extends GuiInterfaceTerminal {
    public GuiWirelessInterfaceTerminal(InventoryPlayer inventoryPlayer, final WirelessTerminalGuiObject te) {
        super(inventoryPlayer, te);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/wirelessupgrades.png");
        Gui.drawModalRectWithCustomSizedTexture(offsetX + 189, offsetY + 165, 0, 0, 32, 32, 32, 32);
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
    }
}
