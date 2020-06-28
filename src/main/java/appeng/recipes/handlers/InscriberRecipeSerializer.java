package appeng.recipes.handlers;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.JSONUtils;
import net.minecraftforge.registries.ForgeRegistryEntry;

import appeng.api.features.InscriberProcessType;
import appeng.core.AppEng;

public class InscriberRecipeSerializer extends ForgeRegistryEntry<RecipeSerializer<?>>
        implements RecipeSerializer<InscriberRecipe> {

    public static final InscriberRecipeSerializer INSTANCE = new InscriberRecipeSerializer();

    static {
        INSTANCE.setRegistryName(AppEng.MOD_ID, "inscriber");
    }

    private InscriberRecipeSerializer() {
    }

    private static InscriberProcessType getMode(JsonObject json) {
        String mode = JSONUtils.getString(json, "mode", "inscribe");
        switch (mode) {
            case "inscribe":
                return InscriberProcessType.INSCRIBE;
            case "press":
                return InscriberProcessType.PRESS;
            default:
                throw new IllegalStateException("Unknown mode for inscriber recipe: " + mode);
        }

    }

    @Override
    public InscriberRecipe read(Identifier recipeId, JsonObject json) {

        InscriberProcessType mode = getMode(json);

        String group = JSONUtils.getString(json, "group", "");
        ItemStack result = ShapedRecipe.deserializeItem(JSONUtils.getJsonObject(json, "result"));

        // Deserialize the three parts of the input
        JsonObject ingredients = JSONUtils.getJsonObject(json, "ingredients");
        Ingredient middle = Ingredient.deserialize(ingredients.get("middle"));
        Ingredient top = Ingredient.EMPTY;
        if (ingredients.has("top")) {
            top = Ingredient.deserialize(ingredients.get("top"));
        }
        Ingredient bottom = Ingredient.EMPTY;
        if (ingredients.has("bottom")) {
            bottom = Ingredient.deserialize(ingredients.get("bottom"));
        }

        return new InscriberRecipe(recipeId, group, middle, result, top, bottom, mode);
    }

    @Nullable
    @Override
    public InscriberRecipe read(Identifier recipeId, PacketByteBuf buffer) {
        String group = buffer.readString();
        Ingredient middle = Ingredient.read(buffer);
        ItemStack result = buffer.readItemStack();
        Ingredient top = Ingredient.read(buffer);
        Ingredient bottom = Ingredient.read(buffer);
        InscriberProcessType mode = buffer.readEnumValue(InscriberProcessType.class);

        return new InscriberRecipe(recipeId, group, middle, result, top, bottom, mode);
    }

    @Override
    public void write(PacketByteBuf buffer, InscriberRecipe recipe) {
        buffer.writeString(recipe.getGroup());
        recipe.getMiddleInput().write(buffer);
        buffer.writeItemStack(recipe.getRecipeOutput());
        recipe.getTopOptional().write(buffer);
        recipe.getBottomOptional().write(buffer);
        buffer.writeEnumValue(recipe.getProcessType());
    }

}
