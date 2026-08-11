/*
 * Copyright (c) 2018, 2020 Adrian Siekierka
 *
 * This file is part of StackUp.
 *
 * StackUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * StackUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with StackUp.  If not, see <http://www.gnu.org/licenses/>.
 */

package appeng.core;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.fml.relauncher.FMLLaunchHandler;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Map;

@IFMLLoadingPlugin.Name("AE2ELCore")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1002) // after stackup
@IFMLLoadingPlugin.TransformerExclusions("appeng.core.transformer")
public class AE2ELCore implements IFMLLoadingPlugin {
    public static final Logger LOGGER = LogManager.getLogger("appliedenergistics2");
    public static final boolean isDeobf = FMLLaunchHandler.isDeobfuscatedEnvironment();
    public static boolean stackUpLoaded = false;

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{
                "appeng.core.transformer.AE2ELTransformer"
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            String name = transformer.getClass().getName();
            if (name.endsWith("pl.asie.stackup.core.StackUpTransformer")) {
                stackUpLoaded = true;
                break;
            }
        }
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
