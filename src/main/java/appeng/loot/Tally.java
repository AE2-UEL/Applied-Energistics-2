package appeng.loot;

import appeng.core.AppEng;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

/**
 * Tally loot that has been rolled in this LootContext. Used to roll unique meteorite presses.
 */
public class Tally extends LootFunction {
    @NotNull
    private final String name;
    private final int contextId;

    protected Tally(LootCondition[] conditionsIn, @NotNull String name, int contextId) {
        super(conditionsIn);
        this.name = name;
        this.contextId = contextId;
    }

    @NotNull
    @Override
    public ItemStack apply(@NotNull ItemStack stack, @NotNull Random rand, @NotNull LootContext context) {
        ILootTallyer tallyer = TallyingLootContext.functionHasContext(context, Serializer.TALLY_NAME);
        if (tallyer != null) {
            var registryName = stack.getItem().getRegistryName();
            String itemName = this.name;
            if (itemName.isEmpty() && registryName != null) {
                // Append metadata
                itemName = registryName + ":" + stack.getItemDamage();
            }
            tallyer.tally(itemName, contextId);
        }
        return stack;
    }

    public static class Serializer extends LootFunction.Serializer<Tally> {
        private static final String TALLY_NAME = "tally";
        private static final String ITEM_NAME_KEY = "id";
        // Optional
        private static final String CONTEXT_ID_KEY = "context_id";

        public Serializer() {
            super(new ResourceLocation(AppEng.MOD_ID, TALLY_NAME), Tally.class);
        }

        @Override
        public void serialize(@NotNull JsonObject object,
                              @NotNull Tally functionClazz,
                              @NotNull JsonSerializationContext serializationContext) {
            String name = functionClazz.name;
            int contextId = functionClazz.contextId;
            object.addProperty(ITEM_NAME_KEY, name);
            object.addProperty(CONTEXT_ID_KEY, contextId);
        }

        @NotNull
        @Override
        public Tally deserialize(@NotNull JsonObject object,
                                 @NotNull JsonDeserializationContext deserializationContext,
                                 LootCondition @NotNull [] conditionsIn) {
            String name = JsonUtils.getString(object, ITEM_NAME_KEY, "");
            int contextId = JsonUtils.getInt(object, CONTEXT_ID_KEY, 0);
            return new Tally(conditionsIn, name, contextId);
        }
    }
}
