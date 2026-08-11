package appeng.tile.crafting;

import appeng.api.AEApi;
import net.minecraft.item.ItemStack;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

public enum CraftingStorageType {
    STORAGE_1K(1, 1024, () -> AEApi.instance().definitions().blocks().craftingStorage1k().maybeStack(1)),
    STORAGE_4K(4, 4096, () -> AEApi.instance().definitions().blocks().craftingStorage4k().maybeStack(1)),
    STORAGE_16K(16, 16384, () -> AEApi.instance().definitions().blocks().craftingStorage16k().maybeStack(1)),
    STORAGE_64K(64, 65536, () -> AEApi.instance().definitions().blocks().craftingStorage64k().maybeStack(1));

    private final int kiloBytes;
    private final int storageBytes;
    private final Supplier<Optional<ItemStack>> itemStackSupplier;

    CraftingStorageType(int kiloBytes, int storageBytes, Supplier<Optional<ItemStack>> itemStackSupplier) {
        this.kiloBytes = kiloBytes;
        this.storageBytes = storageBytes;
        this.itemStackSupplier = itemStackSupplier;
    }

    public Optional<ItemStack> getItemStack() {
        return itemStackSupplier.get();
    }

    public int getStorageBytes() {
        return storageBytes;
    }

    public static Optional<CraftingStorageType> fromKiloBytes(int kiloBytes) {
        return Arrays.stream(values())
                .filter(type -> type.kiloBytes == kiloBytes)
                .findFirst();
    }
}