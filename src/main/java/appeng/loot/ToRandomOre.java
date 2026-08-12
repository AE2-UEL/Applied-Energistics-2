package appeng.loot;

import appeng.core.AppEng;
import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Loot function to convert an item to a random one from the oredict entry, or skystone.
 * The item this function modifies is a placeholder, it will not be used!
 */
public class ToRandomOre extends LootFunction {
    private final Collection<String> oreDictNames;

    protected ToRandomOre(LootCondition[] conditionsIn, Collection<String> oreDictNames) {
        super(conditionsIn);
        this.oreDictNames = oreDictNames;
    }

    @NotNull
    @Override
    public ItemStack apply(@NotNull ItemStack placeholder, @NotNull Random rand, @NotNull LootContext context) {
        var nuggetLoot = oreDictNames.stream()
                .flatMap(name -> OreDictionary.getOres(name).stream())
                .collect(Collectors.toList());
        return nuggetLoot.get(rand.nextInt(nuggetLoot.size()));
    }

    public static class Serializer extends LootFunction.Serializer<ToRandomOre> {
        private static final String RANDOM_ORE_FUNCTION_NAME = "to_random_ore";
        private static final String ORES = "ores";

        public Serializer() {
            super(new ResourceLocation(AppEng.MOD_ID, RANDOM_ORE_FUNCTION_NAME), ToRandomOre.class);
        }

        @Override
        public void serialize(@NotNull JsonObject object,
                              @NotNull ToRandomOre functionClazz,
                              @NotNull JsonSerializationContext serializationContext) {
            JsonArray oreDictNames = new JsonArray();
            for (String s : functionClazz.oreDictNames) {
                oreDictNames.add(s);
            }
            object.add(ORES, oreDictNames);

        }

        @NotNull
        @Override
        public ToRandomOre deserialize(@NotNull JsonObject object,
                                       @NotNull JsonDeserializationContext deserializationContext,
                                       LootCondition @NotNull [] conditionsIn) {
            JsonArray array = JsonUtils.getJsonArray(object, ORES);
            ArrayList<String> oreDictNames = new ArrayList<>();
            for (JsonElement element : array) {
                oreDictNames.add(element.getAsString());
            }
            return new ToRandomOre(conditionsIn, oreDictNames);
        }
    }
}
