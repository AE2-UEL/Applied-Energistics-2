package appeng.loot;

import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A loot condition counterpart to {@link appeng.recipes.factories.conditions.Features}.
 */
public class FeatureEnabled implements LootCondition {
    private final Collection<AEFeature> features;

    public FeatureEnabled(String[] featureNames) {
        this.features = Stream.of(featureNames)
                .map(name -> AEFeature.valueOf(name.toUpperCase(Locale.ENGLISH)))
                .collect(Collectors.toList());
    }

    @Override
    public boolean testCondition(@NotNull Random rand, @NotNull LootContext context) {
        for (var feature : features) {
            if (!AEConfig.instance().isFeatureEnabled(feature)) {
                return false;
            }
        }
        return true;
    }

    public static class Serializer extends LootCondition.Serializer<FeatureEnabled> {
        private static final String FEATURE_ENABLED_NAME = "feature_enabled";
        private static final String FEATURES_KEY = "features";

        public Serializer() {
            super(new ResourceLocation(AppEng.MOD_ID, FEATURE_ENABLED_NAME), FeatureEnabled.class);
        }

        @Override
        public void serialize(@NotNull JsonObject json,
                              @NotNull FeatureEnabled value,
                              @NotNull JsonSerializationContext context) {
            JsonArray featureNames = new JsonArray();
            for (var feature : value.features) {
                featureNames.add(feature.name());
            }
            json.add(FEATURES_KEY, featureNames);
        }

        @NotNull
        @Override
        public FeatureEnabled deserialize(@NotNull JsonObject json,
                                          @NotNull JsonDeserializationContext context) {
            ArrayList<String> featureNames = new ArrayList<>();
            if (JsonUtils.isJsonArray(json, FEATURES_KEY)) {
                var array = JsonUtils.getJsonArray(json, FEATURES_KEY);
                for (JsonElement element : array) {
                    featureNames.add(element.getAsString());
                }
            } else {
                featureNames.add(JsonUtils.getString(json, FEATURES_KEY));
            }
            return new FeatureEnabled(featureNames.toArray(new String[0]));
        }
    }
}
