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

package appeng.client.me;


import appeng.api.AEApi;
import appeng.api.config.*;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.client.gui.widgets.IScrollSource;
import appeng.client.gui.widgets.ISortSource;
import appeng.core.AEConfig;
import appeng.integration.Integrations;
import appeng.integration.modules.bogosorter.InventoryBogoSortModule;
import appeng.items.storage.ItemViewCell;
import appeng.util.ItemSorters;
import appeng.util.Platform;
import appeng.util.prioritylist.IPartitionList;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;


public class ItemRepo {

    private static final Comparator<IAEItemStack> PINNED_ROW_COMPARATOR = Comparator.comparing(stack -> {
        final PinnedKeys.PinInfo info = PinnedKeys.getPinInfo(stack);
        return info != null ? info.since : Instant.MAX;
    });

    private final IItemList<IAEItemStack> list = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
    private List<IAEItemStack> view = new ArrayList<>();
    private final ArrayList<IAEItemStack> pinnedRow = new ArrayList<>();
    private final IScrollSource src;
    private final ISortSource sortSrc;

    private int rowSize = 9;

    private boolean pinnedRowEnabled = false;

    private String searchString = "";
    private IPartitionList<IAEItemStack> myPartitionList;
    private String innerSearch = "";
    private boolean hasPower;

    private Enum lastView;
    private Enum lastSearchMode;
    private Enum lastSortBy;
    private Enum lastSortDir;
    private String lastSearch = "";

    private boolean resort = true;
    private boolean changed = false;


    public ItemRepo(final IScrollSource src, final ISortSource sortSrc) {
        this.src = src;
        this.sortSrc = sortSrc;
    }

    public IAEItemStack getReferenceItem(int idx) {
        if (this.pinnedRowEnabled && !this.pinnedRow.isEmpty()) {
            if (idx < this.rowSize) {
                return idx < this.pinnedRow.size() ? this.pinnedRow.get(idx) : null;
            }
            idx -= this.rowSize;
        }

        idx += this.src.getCurrentScroll() * this.rowSize;

        if (idx >= this.view.size()) {
            return null;
        }
        return this.view.get(idx);
    }

    void setSearch(final String search) {
        this.searchString = search == null ? "" : search;
    }

    public void postUpdate(final IAEItemStack is) {
        final IAEItemStack st = this.list.findPrecise(is);

        if (st != null) {
            st.reset();
            st.add(is);
        } else {
            this.list.add(is);
        }

        changed = true;
    }

    public long getItemCount(final IAEItemStack is) {
        IAEItemStack st = this.list.findPrecise(is);
        return st == null ? 0 : st.getStackSize();
    }

    public void setViewCell(final ItemStack[] list) {
        this.myPartitionList = ItemViewCell.createFilter(list);
        this.changed = true;
    }

    public void updateView() {

        final Enum viewMode = this.sortSrc.getSortDisplay();

        if (lastView != viewMode) {
            resort = true;
            lastView = viewMode;
        }

        final Enum searchMode = AEConfig.instance().getConfigManager().getSetting(Settings.SEARCH_MODE);
        if (lastSearchMode != searchMode) {
            resort = true;
            lastSearchMode = searchMode;
        }

        if (searchMode == SearchBoxMode.JEI_AUTOSEARCH || searchMode == SearchBoxMode.JEI_MANUAL_SEARCH || searchMode == SearchBoxMode.JEI_AUTOSEARCH_KEEP || searchMode == SearchBoxMode.JEI_MANUAL_SEARCH_KEEP) {
            this.updateJEI(this.searchString);
        }

        if (!lastSearch.equals(searchString)) {
            resort = true;
            lastSearch = searchString;
        }

        final Enum sortBy = this.sortSrc.getSortBy();
        final Enum sortDir = this.sortSrc.getSortDir();

        if (lastSortBy != sortBy) {
            resort = true;
            lastSortBy = sortBy;
        }

        if (lastSortDir != sortDir) {
            resort = true;
            lastSortDir = sortDir;
        }

        if (changed || resort) {
            changed = false;
            resort = false;

            view = new ArrayList<>();
            pinnedRow.clear();

            ItemSorters.setDirection((appeng.api.config.SortDir) sortDir);
            ItemSorters.init();

            Comparator<IAEItemStack> c = getComparator(sortBy);

            for (IAEItemStack is : this.list) {
                addIAE(is, viewMode);
            }

            if (this.pinnedRowEnabled && !PinnedKeys.isEmpty()) {
                for (IAEItemStack pinnedKey : PinnedKeys.getPinnedKeys()) {
                    final PinnedKeys.PinInfo info = PinnedKeys.getPinInfo(pinnedKey);
                    if (info == null || info.reason == PinnedKeys.PinReason.CRAFTING) {
                        continue;
                    }

                    boolean alreadyShown = false;
                    for (IAEItemStack shown : this.pinnedRow) {
                        if (pinnedKey.isSameType(shown)) {
                            alreadyShown = true;
                            break;
                        }
                    }

                    if (!alreadyShown) {
                        final IAEItemStack fake = pinnedKey.copy();
                        fake.reset();
                        this.pinnedRow.add(fake);
                    }
                }
            }

            pinnedRow.sort(PINNED_ROW_COMPARATOR);

            view.sort(c);
        }
    }

    private static Comparator<IAEItemStack> getComparator(Enum sortBy) {
        Comparator<IAEItemStack> c;

        if (sortBy == SortOrder.MOD) {
            c = ItemSorters.CONFIG_BASED_SORT_BY_MOD;
        } else if (sortBy == SortOrder.AMOUNT) {
            c = ItemSorters.CONFIG_BASED_SORT_BY_SIZE;
        } else if (sortBy == SortOrder.INVTWEAKS) {
            if (InventoryBogoSortModule.isLoaded()) {
                c = InventoryBogoSortModule.COMPARATOR;
            } else {
                c = ItemSorters.CONFIG_BASED_SORT_BY_INV_TWEAKS;
            }
        } else {
            c = ItemSorters.CONFIG_BASED_SORT_BY_NAME;
        }
        return c;
    }

    private void addIAE(IAEItemStack is, Enum viewMode) {

        final boolean hasPinnedRow = this.pinnedRowEnabled && !PinnedKeys.isEmpty();

        if (hasPinnedRow && this.pinnedRow.size() < this.rowSize && PinnedKeys.isPinned(is)) {
            this.pinnedRow.add(is);
            return;
        }

        final boolean needsZeroCopy = viewMode == ViewItems.CRAFTABLE;

        final boolean terminalSearchToolTips = AEConfig.instance().getConfigManager().getSetting(Settings.SEARCH_TOOLTIPS) != YesNo.NO;

        boolean searchMod = false;

        this.innerSearch = searchString.toLowerCase();
        if (this.innerSearch.startsWith("@")) {
            searchMod = true;
            this.innerSearch = this.innerSearch.substring(1);
        }

        Pattern m = null;
        try {
            m = Pattern.compile(this.innerSearch, Pattern.CASE_INSENSITIVE);
        } catch (final Throwable ignore) {
            try {
                m = Pattern.compile(Pattern.quote(this.innerSearch), Pattern.CASE_INSENSITIVE);
            } catch (final Throwable __) {
                return;
            }
        }

        if (this.myPartitionList != null) {
            if (!this.myPartitionList.isListed(is)) {
                return;
            }
        }

        if (viewMode == ViewItems.CRAFTABLE && !is.isCraftable()) {
            return;
        }

        if (viewMode == ViewItems.STORED && is.getStackSize() == 0) {
            return;
        }

        final String dspName = (searchMod ? Platform.getModId(is) : Platform.getItemDisplayName(is)).toLowerCase();
        boolean foundMatchingItemStack = true;

        for (String term : innerSearch.split(" ")) {
            if (term.length() > 1 && (term.startsWith("-") || term.startsWith("!"))) {
                term = term.substring(1);
                if (dspName.contains(term)) {
                    foundMatchingItemStack = false;
                    break;
                }
            } else if (!dspName.contains(term)) {
                foundMatchingItemStack = false;
                break;
            }
        }

        if (terminalSearchToolTips && !foundMatchingItemStack) {
            final List<String> tooltip = Platform.getTooltip(is);
            for (final String line : tooltip) {
                if (m.matcher(line).find()) {
                    foundMatchingItemStack = true;
                    break;
                }
            }
        }

        if (foundMatchingItemStack) {
            if (needsZeroCopy) {
                is = is.copy();
                is.setStackSize(0);
            }
            this.view.add(is);
        }
    }

    private void updateJEI(String filter) {
        Integrations.jei().setSearchText(filter);
    }

    public int size() {
        return this.view.size() + this.pinnedRow.size();
    }

    public void clear() {
        this.list.resetStatus();
        this.pinnedRow.clear();
    }

    public boolean hasPinnedRow() {
        return this.pinnedRowEnabled && !this.pinnedRow.isEmpty();
    }

    public List<IAEItemStack> getPinnedEntries() {
        return Collections.unmodifiableList(this.pinnedRow);
    }

    public void setPinnedRowEnabled(final boolean pinnedRowEnabled) {
        this.pinnedRowEnabled = pinnedRowEnabled;
    }

    public boolean hasPower() {
        return this.hasPower;
    }

    public void setPower(final boolean hasPower) {
        this.hasPower = hasPower;
    }

    public int getRowSize() {
        return this.rowSize;
    }

    public void setRowSize(final int rowSize) {
        this.rowSize = rowSize;
    }

    public String getSearchString() {
        return this.searchString;
    }

    public void setSearchString(@Nonnull final String searchString) {
        this.searchString = searchString;
    }

    public IItemList<IAEItemStack> getList() {
        return list;
    }
}
