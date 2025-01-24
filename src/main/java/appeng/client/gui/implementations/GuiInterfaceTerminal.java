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

package appeng.client.gui.implementations;


import appeng.api.AEApi;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.MEGuiTooltipTextField;
import appeng.client.me.ClientDCInternalInv;
import appeng.client.me.SlotDisconnected;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.helpers.DualityInterface;
import appeng.helpers.PatternHelper;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.util.BlockPosUtils;
import appeng.util.Platform;
import com.google.common.collect.HashMultimap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.*;

import static appeng.client.render.BlockPosHighlighter.hilightBlock;
import static appeng.helpers.ItemStackHelper.stackFromNBT;


public class GuiInterfaceTerminal extends AEBaseGui {

    private static final int OFFSET_X = 21;
    private static final int MAGIC_HEIGHT_NUMBER = 52 + 99;
    private static final String MOLECULAR_ASSEMBLER = "tile.appliedenergistics2.molecular_assembler";

    private final boolean jeiEnabled;
    private final int jeiButtonPadding;

    private final HashMap<Long, ClientDCInternalInv> byId = new HashMap<>();
    private final HashMultimap<String, ClientDCInternalInv> byName = HashMultimap.create();
    private final HashMap<ClientDCInternalInv, BlockPos> blockPosHashMap = new HashMap<>();
    private final HashMap<GuiButton, ClientDCInternalInv> guiButtonHashMap = new HashMap<>();
    private final Map<ClientDCInternalInv, Integer> numUpgradesMap = new HashMap<>();
    private final ArrayList<String> names = new ArrayList<>();
    private final ArrayList<Object> lines = new ArrayList<>();
    private final Set<Object> matchedStacks = new HashSet<>();
    private final Map<String, Set<Object>> cachedSearches = new WeakHashMap<>();
    private final Map<ClientDCInternalInv, Integer> dimHashMap = new HashMap<>();

    private final MEGuiTooltipTextField searchFieldOutputs;
    private final MEGuiTooltipTextField searchFieldInputs;
    private final MEGuiTooltipTextField searchFieldNames;

    private final GuiImgButton guiButtonHideFull;
    private final GuiImgButton guiButtonAssemblersOnly;
    private final GuiImgButton guiButtonBrokenRecipes;
    private final GuiImgButton terminalStyleBox;

    private boolean refreshList = false;

    /* These are worded so that the intended default is false */
    private boolean onlyShowWithSpace = false;
    private boolean onlyMolecularAssemblers = false;
    private boolean onlyBrokenRecipes = false;
    private int rows = 6;

    public GuiInterfaceTerminal(final InventoryPlayer inventoryPlayer, final PartInterfaceTerminal te) {
        super(new ContainerInterfaceTerminal(inventoryPlayer, te));

        final GuiScrollbar scrollbar = new GuiScrollbar();
        this.setScrollBar(scrollbar);
        this.xSize = 208;
        this.ySize = 255;
        this.jeiEnabled = Platform.isModLoaded("jei");
        this.jeiButtonPadding = jeiEnabled ? 22 : 0;

        searchFieldInputs = createTextField(86, 12, ButtonToolTips.SearchFieldInputs.getLocal());
        searchFieldOutputs = createTextField(86, 12, ButtonToolTips.SearchFieldOutputs.getLocal());
        searchFieldNames = createTextField(71, 12, ButtonToolTips.SearchFieldNames.getLocal());
        searchFieldNames.setFocused(true);

        guiButtonAssemblersOnly = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonHideFull = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonBrokenRecipes = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        terminalStyleBox = new GuiImgButton(0, 0, Settings.TERMINAL_STYLE, null);
    }

    private MEGuiTooltipTextField createTextField(final int width, final int height, final String tooltip) {
        MEGuiTooltipTextField textField = new MEGuiTooltipTextField(width, height, tooltip) {
            @Override
            public void onTextChange(String oldText) {
                refreshList();
            }
        };
        textField.setEnableBackgroundDrawing(false);
        textField.setMaxStringLength(25);
        textField.setTextColor(0xFFFFFF);
        textField.setCursorPositionZero();
        return textField;
    }

    private void setScrollBar() {
        this.getScrollBar().setTop(52).setLeft(189).setHeight(this.rows * 18 - 2);
        this.getScrollBar().setRange(0, this.lines.size() - 1, 1);
    }

    private int calculateRowsCount() {
        final int maxRows = getMaxRows();
        final int jeiPadding = jeiEnabled ? 22 + 18 : 0;
        final int extraSpace = this.height - MAGIC_HEIGHT_NUMBER - jeiPadding;

        return Math.max(6, Math.min(maxRows, extraSpace / 18));
    }

    @Override
    public void initGui() {
        this.rows = calculateRowsCount();

        super.initGui();

        this.ySize = MAGIC_HEIGHT_NUMBER + this.rows * 18;
        final int unusedSpace = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));

        searchFieldInputs.x = guiLeft + 32;
        searchFieldInputs.y = guiTop + 25;
        searchFieldOutputs.x = guiLeft + 32;
        searchFieldOutputs.y = guiTop + 38;
        searchFieldNames.x = guiLeft + 32 + 99;
        searchFieldNames.y = guiTop + 38;

        terminalStyleBox.x = guiLeft - 18;
        terminalStyleBox.y = guiTop + 8 + jeiButtonPadding;
        guiButtonBrokenRecipes.x = guiLeft - 18;
        guiButtonBrokenRecipes.y = terminalStyleBox.y + 20;
        guiButtonHideFull.x = guiLeft - 18;
        guiButtonHideFull.y = guiButtonBrokenRecipes.y + 20;
        guiButtonAssemblersOnly.x = guiLeft - 18;
        guiButtonAssemblersOnly.y = guiButtonHideFull.y + 20;

        this.setScrollBar();
        this.repositionSlots();
    }

    protected void repositionSlots() {
        for (final Object obj : this.inventorySlots.inventorySlots) {
            if (obj instanceof AppEngSlot slot) {
                slot.yPos = this.ySize + slot.getY() - 78 - 7;
                slot.xPos = slot.getX() + 14;
            }
        }
    }

    @Override
    public List<Rectangle> getJEIExclusionArea() {
        Rectangle tallButton = new Rectangle(this.guiLeft - 18, this.guiTop + 24 + 24, 18, 18);
        List<Rectangle> area = new ArrayList<>();
        area.add(tallButton);
        return area;
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRenderer.drawString(this.getGuiDisplayName(GuiText.InterfaceTerminal.getLocal()), OFFSET_X + 2, 6, 4210752);
        this.fontRenderer.drawString(GuiText.inventory.getLocal(), OFFSET_X + 2, this.ySize - 96, 4210752);

        final int currentScroll = this.getScrollBar().getCurrentScroll();

        int offset = 51;
        int linesDraw = 0;
        for (int x = 0; x < rows && linesDraw < rows && currentScroll + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(currentScroll + x);
            if (lineObj instanceof ClientDCInternalInv inv) {

                final int extraLines = numUpgradesMap.get(inv);
                for (int row = 0; row < 1 + extraLines && linesDraw < rows; ++row) {
                    for (int z = 0; z < 9; z++) {
                        if (this.matchedStacks.contains(inv.getInventory().getStackInSlot(z + (row * 9)))) {
                            drawRect(z * 18 + 22, 1 + offset, z * 18 + 22 + 16, 1 + offset + 16, 0x2A00FF00);
                        }
                    }
                    linesDraw++;
                    offset += 18;
                }
            } else if (lineObj instanceof String name) {
                final int rows = this.byName.get(name).size();
                if (rows > 1) {
                    name = name + " (" + rows + ')';
                }

                while (name.length() > 2 && this.fontRenderer.getStringWidth(name) > 158) {
                    name = name.substring(0, name.length() - 1);
                }
                this.fontRenderer.drawString(name, OFFSET_X + 3, 6 + offset, 4210752);
                linesDraw++;
                offset += 18;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        buttonList.clear();
        guiButtonHashMap.clear();
        inventorySlots.inventorySlots.removeIf(slot -> slot instanceof SlotDisconnected);

        guiButtonAssemblersOnly.set(onlyMolecularAssemblers ? ActionItems.MOLECULAR_ASSEMBLERS_ON : ActionItems.MOLECULAR_ASSEMBLERS_OFF);
        guiButtonHideFull.set(onlyShowWithSpace ? ActionItems.TOGGLE_SHOW_FULL_INTERFACES_OFF : ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON);
        guiButtonBrokenRecipes.set(onlyBrokenRecipes ? ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERNS_ON : ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERNS_OFF);
        terminalStyleBox.set(AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE));

        buttonList.add(guiButtonAssemblersOnly);
        buttonList.add(guiButtonHideFull);
        buttonList.add(guiButtonBrokenRecipes);
        buttonList.add(terminalStyleBox);

        int offset = 51;
        final int currentScroll = this.getScrollBar().getCurrentScroll();
        int linesDraw = 0;

        for (int x = 0; x < rows && linesDraw < rows && currentScroll + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(currentScroll + x);
            if (lineObj instanceof ClientDCInternalInv inv) {

                GuiButton guiButton = new GuiImgButton(guiLeft + 4, guiTop + offset + 1, Settings.ACTIONS, ActionItems.HIGHLIGHT_INTERFACE);
                guiButtonHashMap.put(guiButton, inv);
                this.buttonList.add(guiButton);

                final int extraLines = numUpgradesMap.get(inv);
                for (int row = 0; row < 1 + extraLines && linesDraw < rows; ++row) {
                    for (int z = 0; z < 9; z++) {
                        this.inventorySlots.inventorySlots.add(new SlotDisconnected(inv, z + (row * 9), z * 18 + 22, 1+ offset));
                    }
                    linesDraw++;
                    offset += 18;
                }

            } else if (lineObj instanceof String) {
                linesDraw++;
                offset += 18;
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        drawTooltip(searchFieldInputs, mouseX, mouseY);
        drawTooltip(searchFieldOutputs, mouseX, mouseY);
        drawTooltip(searchFieldNames, mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) throws IOException {
        this.searchFieldInputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldOutputs.mouseClicked(xCoord, yCoord, btn);
        this.searchFieldNames.mouseClicked(xCoord, yCoord, btn);

        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) throws IOException {
        if (guiButtonHashMap.containsKey(btn)) {
            BlockPos blockPos = blockPosHashMap.get(guiButtonHashMap.get(this.selectedButton));
            BlockPos blockPos2 = mc.player.getPosition();
            int playerDim = mc.world.provider.getDimension();
            int interfaceDim = dimHashMap.get(guiButtonHashMap.get(this.selectedButton));
            if (playerDim != interfaceDim) {
                try {
                    mc.player.sendStatusMessage(PlayerMessages.InterfaceInOtherDimParam.get(interfaceDim, DimensionManager.getWorld(interfaceDim).provider.getDimensionType().getName()), false);
                } catch (Exception e) {
                    mc.player.sendStatusMessage(PlayerMessages.InterfaceInOtherDim.get(), false);
                }
            } else {
                hilightBlock(blockPos, System.currentTimeMillis() + 500 * BlockPosUtils.getDistance(blockPos, blockPos2), playerDim);
                mc.player.sendStatusMessage(PlayerMessages.InterfaceHighlighted.get(blockPos.getX(), blockPos.getY(), blockPos.getZ()), false);
            }
            mc.player.closeScreen();
        } else if (btn == guiButtonHideFull) {
            onlyShowWithSpace = !onlyShowWithSpace;
            this.refreshList();
        } else if (btn == guiButtonAssemblersOnly) {
            onlyMolecularAssemblers = !onlyMolecularAssemblers;
            this.refreshList();
        } else if (btn == guiButtonBrokenRecipes) {
            onlyBrokenRecipes = !onlyBrokenRecipes;
            this.refreshList();
        } else if (btn instanceof GuiImgButton iBtn) {
            if (iBtn.getSetting() != Settings.ACTIONS) {
                final Enum<?> cv = iBtn.getCurrentValue();
                final boolean backwards = Mouse.isButtonDown(1);
                final Enum<?> next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());

                if (btn == this.terminalStyleBox) {
                    AEConfig.instance().getConfigManager().putSetting(iBtn.getSetting(), next);
                    this.reinitalize();
                }
                iBtn.set(next);
            }
        }
    }

    private void reinitalize() {
        this.buttonList.clear();
        this.initGui();
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture("guis/newinterfaceterminal.png");

        // draw the top portion of the background, above the interface list
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, 53);

        for (int x = 0; x < this.rows; x++) {
            // draw the background of the rows in the interface list
            this.drawTexturedModalRect(offsetX, offsetY + 53 + x * 18, 0, 52, this.xSize, 18);
        }

        int offset = 51;
        final int ex = this.getScrollBar().getCurrentScroll();
        int linesDraw = 0;
        for (int x = 0; x < this.rows && linesDraw < rows && ex + x < this.lines.size(); x++) {
            final Object lineObj = this.lines.get(ex + x);
            if (lineObj instanceof ClientDCInternalInv) {
                GlStateManager.color(1, 1, 1, 1);

                final int width = 9 * 18;
                final int extraLines = numUpgradesMap.get(lineObj);

                // draw the slot backgrounds
                for (int row = 0; row < 1 + extraLines && linesDraw < rows; ++row) {
                    this.drawTexturedModalRect(offsetX + 20, offsetY + offset, 20, 173, width, 18);

                    offset += 18;
                    linesDraw++;
                }
            } else {
                offset += 18;
                linesDraw++;
            }
        }

        // draw the background below the interface list
        this.drawTexturedModalRect(offsetX, offsetY + 50 + this.rows * 18, 0, 158, this.xSize, 99);

        // draw the text boxes
        this.searchFieldInputs.drawTextBox();
        this.searchFieldOutputs.drawTextBox();
        this.searchFieldNames.drawTextBox();
    }

    @Override
    protected void keyTyped(final char character, final int key) throws IOException {
        if (!this.checkHotbarKeys(key)) {
            if (character == ' ') {
                if ((this.searchFieldInputs.getText().isEmpty() && this.searchFieldInputs.isFocused())
                        || (this.searchFieldOutputs.getText().isEmpty() && this.searchFieldOutputs.isFocused())
                        || (this.searchFieldNames.getText().isEmpty() && this.searchFieldNames.isFocused())) {
                    return;
                }
            } else if (character == '\t') {
                if (handleTab()) {
                    return;
                }
            }

            if (this.searchFieldInputs.textboxKeyTyped(character, key)
                    || this.searchFieldOutputs.textboxKeyTyped(character, key)
                    || this.searchFieldNames.textboxKeyTyped(character, key)) {
                this.refreshList();
            } else {
                super.keyTyped(character, key);
            }
        }
    }

    /** Cycle to the next search bar if tab is pressed, going in reverse if shift is held. */
    private boolean handleTab() {
        if (searchFieldInputs.isFocused()) {
            searchFieldInputs.setFocused(false);
            if (isShiftKeyDown()) searchFieldNames.setFocused(true);
            else searchFieldOutputs.setFocused(true);
            return true;
        } else if (searchFieldOutputs.isFocused()) {
            searchFieldOutputs.setFocused(false);
            if (isShiftKeyDown()) searchFieldInputs.setFocused(true);
            else searchFieldNames.setFocused(true);
            return true;
        } else if (searchFieldNames.isFocused()) {
            searchFieldNames.setFocused(false);
            if (isShiftKeyDown()) searchFieldOutputs.setFocused(true);
            else searchFieldInputs.setFocused(true);
            return true;
        }
        return false;
    }

    public void postUpdate(final NBTTagCompound in) {
        if (in.getBoolean("clear")) {
            this.byId.clear();
            this.refreshList = true;
        }

        for (final Object oKey : in.getKeySet()) {
            final String key = (String) oKey;
            if (key.startsWith("=")) {
                try {
                    final long id = Long.parseLong(key.substring(1), Character.MAX_RADIX);
                    final NBTTagCompound invData = in.getCompoundTag(key);
                    final ClientDCInternalInv current = this.getById(id, invData.getLong("sortBy"), invData.getString("un"));
                    blockPosHashMap.put(current, NBTUtil.getPosFromTag(invData.getCompoundTag("pos")));
                    dimHashMap.put(current, invData.getInteger("dim"));
                    numUpgradesMap.put(current, invData.getInteger("numUpgrades"));

                    for (int x = 0; x < current.getInventory().getSlots(); x++) {
                        final String which = Integer.toString(x);
                        if (invData.hasKey(which)) {
                            current.getInventory().setStackInSlot(x, stackFromNBT(invData.getCompoundTag(which)));
                        }
                    }
                } catch (final NumberFormatException ignored) {
                }
            }
        }

        if (this.refreshList) {
            this.refreshList = false;
            // invalid caches on refresh
            this.cachedSearches.clear();
            this.refreshList();
        }
    }

    /**
     * Rebuilds the list of interfaces.
     * <p>
     * Respects a search term if present (ignores case) and adding only matching patterns.
     */
    private void refreshList() {
        this.byName.clear();
        this.buttonList.clear();
        this.matchedStacks.clear();

        final String searchFieldInputs = this.searchFieldInputs.getText().toLowerCase();
        final String searchFieldOutputs = this.searchFieldOutputs.getText().toLowerCase();
        final String searchFieldNames = this.searchFieldNames.getText().toLowerCase();

        final Set<Object> cachedSearch = this.getCacheForSearchTerm("IN:" + searchFieldInputs + " OUT:" + searchFieldOutputs
                + "NAME:" + searchFieldNames + onlyShowWithSpace + onlyMolecularAssemblers + onlyBrokenRecipes);
        final boolean rebuild = cachedSearch.isEmpty();

        for (final ClientDCInternalInv entry : this.byId.values()) {
            // ignore inventory if not doing a full rebuild and cache already marks it as miss.
            if (!rebuild && !cachedSearch.contains(entry)) {
                continue;
            }

            // Shortcut to skip any filter if search term is ""/empty

            boolean found = searchFieldInputs.isEmpty() && searchFieldOutputs.isEmpty();
            boolean interfaceHasFreeSlots = false;
            boolean interfaceHasBrokenRecipes = false;

            // Search if the current inventory holds a pattern containing the search term.
            if (!found || onlyShowWithSpace || onlyBrokenRecipes) {
                int slot = 0;
                for (final ItemStack itemStack : entry.getInventory()) {
                    if (slot > 8 + numUpgradesMap.get(entry) * 9) {
                        break;
                    }

                    if (itemStack.isEmpty()) {
                        interfaceHasFreeSlots = true;
                    }

                    if (onlyBrokenRecipes && recipeIsBroken(itemStack)) {
                        interfaceHasBrokenRecipes = true;
                    }

                    if ((!searchFieldInputs.isEmpty() && itemStackMatchesSearchTerm(itemStack, searchFieldInputs, 0))
                            || (!searchFieldOutputs.isEmpty() && itemStackMatchesSearchTerm(itemStack, searchFieldOutputs, 1))) {
                        found = true;
                        matchedStacks.add(itemStack);
                    }

                    slot++;
                }
            }

            // Exit if not found
            if (!found) {
                cachedSearch.remove(entry);
                continue;
            }
            // Exit if the interface does not match the name search
            if (!entry.getName().toLowerCase().contains(searchFieldNames)) {
                cachedSearch.remove(entry);
                continue;
            }
            // Exit if molecular assembler filter is on and this is not a molecular assembler
            // Forge documantation said unlocalized name shouldn't be use for logic, so we might need a better way......
            if (onlyMolecularAssemblers && !entry.getUnlocalizedName().equals(MOLECULAR_ASSEMBLER)) {
                cachedSearch.remove(entry);
                continue;
            }
            // Exit if we are only showing interfaces with free slots and there are none free in this interface
            if (onlyShowWithSpace && !interfaceHasFreeSlots) {
                cachedSearch.remove(entry);
                continue;
            }
            // Exit if we are only showing interfaces with broken patterns and there are no broken patterns in this interface
            if (onlyBrokenRecipes && !interfaceHasBrokenRecipes) {
                cachedSearch.remove(entry);
                continue;
            }

            // Successful search
            this.byName.put(entry.getName(), entry);
            cachedSearch.add(entry);
        }

        this.names.clear();
        this.names.addAll(this.byName.keySet());
        Collections.sort(this.names);

        this.lines.clear();
        this.lines.ensureCapacity(this.names.size() + this.byId.size());

        for (final String n : this.names) {
            this.lines.add(n);
            final ArrayList<ClientDCInternalInv> clientInventories = new ArrayList<>(this.byName.get(n));
            Collections.sort(clientInventories);
            this.lines.addAll(clientInventories);
        }

        this.setScrollBar();
    }

    private boolean recipeIsBroken(final ItemStack stack) {
        if (stack == null) return false;
        if (stack.isEmpty()) return false;

        final NBTTagCompound encodedValue = stack.getTagCompound();
        if (encodedValue == null) return true;

        final World w = AppEng.proxy.getWorld();
        if (w == null) return false;

        try {
            new PatternHelper(stack, w);
            return false;
        } catch (Throwable ignored) {
            return true;
        }
    }

    private boolean itemStackMatchesSearchTerm(final ItemStack itemStack, final String searchTerm, int pass) {
        if (itemStack.isEmpty()) {
            return false;
        }

        final NBTTagCompound encodedValue = itemStack.getTagCompound();

        if (encodedValue == null) {
            return searchTerm.matches(GuiText.InvalidPattern.getLocal());
        }

        final NBTTagList tag;
        if (pass == 0) {
            tag = encodedValue.getTagList("in", Constants.NBT.TAG_COMPOUND);
        } else {
            tag = encodedValue.getTagList("out", Constants.NBT.TAG_COMPOUND);
        }

        boolean foundMatchingItemStack = false;
        final String[] splitTerm = searchTerm.split(" ");

        for (int i = 0; i < tag.tagCount(); i++) {
            final ItemStack parsedItemStack = new ItemStack(tag.getCompoundTagAt(i));
            if (!parsedItemStack.isEmpty()) {
                final String displayName = Platform
                        .getItemDisplayName(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createStack(parsedItemStack))
                        .toLowerCase();

                for (String term : splitTerm) {
                    if (term.length() > 1 && (term.startsWith("-") || term.startsWith("!"))) {
                        term = term.substring(1);
                        if (displayName.contains(term)) {
                            return false;
                        }
                    } else if (displayName.contains(term)) {
                        foundMatchingItemStack = true;
                    }
                }
            }
        }
        return foundMatchingItemStack;
    }

    /**
     * Tries to retrieve a cache for a with search term as keyword.
     * <p>
     * If this cache should be empty, it will populate it with an earlier cache if available or at least the cache for
     * the empty string.
     *
     * @param searchTerm the corresponding search
     * @return a Set matching a superset of the search term
     */
    private Set<Object> getCacheForSearchTerm(final String searchTerm) {
        if (!this.cachedSearches.containsKey(searchTerm)) {
            this.cachedSearches.put(searchTerm, new HashSet<>());
        }

        final Set<Object> cache = this.cachedSearches.get(searchTerm);

        if (cache.isEmpty() && searchTerm.length() > 1) {
            cache.addAll(this.getCacheForSearchTerm(searchTerm.substring(0, searchTerm.length() - 1)));
            return cache;
        }

        return cache;
    }

    private int getMaxRows() {
        return AEConfig.instance().getConfigManager().getSetting(Settings.TERMINAL_STYLE) != TerminalStyle.TALL ? 6 : Integer.MAX_VALUE;
    }

    private ClientDCInternalInv getById(final long id, final long sortBy, final String string) {
        ClientDCInternalInv o = this.byId.get(id);

        if (o == null) {
            this.byId.put(id, o = new ClientDCInternalInv(DualityInterface.NUMBER_OF_PATTERN_SLOTS, id, sortBy, string));
            this.refreshList = true;
        }

        return o;
    }
}
