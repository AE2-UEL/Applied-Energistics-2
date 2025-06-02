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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;


public class ItemRepo {

    private final IItemList<IAEItemStack> list = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class).createList();
    private List<IAEItemStack> view = new ArrayList<>();
    private final IScrollSource src;
    private final ISortSource sortSrc;

    private int rowSize = 9;

    private String searchString = "";
    private IPartitionList<IAEItemStack> myPartitionList;
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

            ItemSorters.setDirection((appeng.api.config.SortDir) sortDir);
            ItemSorters.init();

            Comparator<IAEItemStack> c = getComparator(sortBy);

            String innerSearch = searchString.toLowerCase();

            final boolean searchMod;
            if (innerSearch.startsWith("@")) {
                searchMod = true;
                innerSearch = innerSearch.substring(1);
            } else {
                searchMod = false;
            }

            final boolean terminalSearchToolTips = AEConfig.instance().getConfigManager().getSetting(Settings.SEARCH_TOOLTIPS) != YesNo.NO;

            Pattern m;
            try {
                m = Pattern.compile(innerSearch, Pattern.CASE_INSENSITIVE);
            } catch (final Throwable ignore) {
                try {
                    m = Pattern.compile(Pattern.quote(innerSearch), Pattern.CASE_INSENSITIVE);
                } catch (final Throwable __) {
                    return;
                }
            }

            String[] innerSearchTerms = innerSearch.split(" ");
            for (IAEItemStack is : this.list) {
                addIAE(is, viewMode, innerSearchTerms, searchMod, terminalSearchToolTips, m);
            }

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

    private void addIAE(IAEItemStack is, Enum viewMode, String[] terms, boolean searchMod, boolean searchTooltips, Pattern pattern) {

        final boolean needsZeroCopy = viewMode == ViewItems.CRAFTABLE;

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

        for (String term : terms) {
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

        if (searchTooltips && !foundMatchingItemStack) {
            final List<String> tooltip = Platform.getTooltip(is);
            for (final String line : tooltip) {
                if (pattern.matcher(line).find()) {
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
        return this.view.size();
    }

    public void clear() {
        this.list.resetStatus();
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
