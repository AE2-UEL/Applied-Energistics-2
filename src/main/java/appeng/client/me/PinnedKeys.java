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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableSet;

import appeng.api.storage.data.IAEItemStack;

public final class PinnedKeys {
    public static final int MAX_PINNED = 9;

    private static final Comparator<Map.Entry<IAEItemStack, PinInfo>> TIME_COMPARATOR = new Comparator<Map.Entry<IAEItemStack, PinInfo>>() {
        @Override
        public int compare(Map.Entry<IAEItemStack, PinInfo> a, Map.Entry<IAEItemStack, PinInfo> b) {
            return a.getValue().since.compareTo(b.getValue().since);
        }
    };

    private static final Map<IAEItemStack, PinInfo> pinned = new HashMap<IAEItemStack, PinInfo>(MAX_PINNED);

    private static final Set<IAEItemStack> pendingJobs = new HashSet<IAEItemStack>(MAX_PINNED);

    private PinnedKeys() {
    }

    public static boolean isEmpty() {
        return pinned.isEmpty();
    }

    public static Set<IAEItemStack> getPinnedKeys() {
        return ImmutableSet.copyOf(pinned.keySet());
    }

    @Nullable
    public static PinInfo getPinInfo(IAEItemStack key) {
        return pinned.get(key);
    }

    public static void clearPinnedKeys() {
        pinned.clear();
        pendingJobs.clear();
    }

    public static void pinKey(IAEItemStack key, PinReason reason) {
        PinInfo info = pinned.get(key);
        if (info != null) {
            info.since = Instant.now();
        } else {
            pinned.put(key.copy(), new PinInfo(reason));
        }

        if (reason == PinReason.CRAFTING) {
            pendingJobs.add(key.copy());
        }

        if (pinned.size() > MAX_PINNED) {
            List<Map.Entry<IAEItemStack, PinInfo>> byAge = new ArrayList<Map.Entry<IAEItemStack, PinInfo>>(pinned.entrySet());
            Collections.sort(byAge, TIME_COMPARATOR);

            int toRemove = byAge.size() - MAX_PINNED;
            for (int i = 0; i < toRemove; i++) {
                IAEItemStack evicted = byAge.get(i).getKey();
                pinned.remove(evicted);
                pendingJobs.remove(evicted);
            }
        }
    }

    public static void unpin(IAEItemStack what) {
        pinned.remove(what);
        pendingJobs.remove(what);
    }

    public static boolean isPinned(IAEItemStack what) {
        return pinned.containsKey(what);
    }

    public static boolean hasPendingJob(IAEItemStack key) {
        return pendingJobs.contains(key);
    }

    public static void markJobDone(IAEItemStack key) {
        pendingJobs.remove(key);
    }

    public static void prune() {
        Iterator<Map.Entry<IAEItemStack, PinInfo>> it = pinned.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<IAEItemStack, PinInfo> entry = it.next();
            if (entry.getValue().canPrune) {
                pendingJobs.remove(entry.getKey());
                it.remove();
            }
        }
    }

    public static class PinInfo {
        public Instant since;
        public PinReason reason;
        public boolean canPrune;

        public PinInfo(PinReason reason) {
            this.reason = reason;
            this.since = Instant.now();
        }
    }

    public enum PinReason {
        CRAFTING
    }
}
