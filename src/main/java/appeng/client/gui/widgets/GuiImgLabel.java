package appeng.client.gui.widgets;

import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import java.util.HashMap;
import java.util.Map;

public class GuiImgLabel extends GuiLabel implements ITooltip {
    public GuiImgLabel(FontRenderer fontRendererObj, final int x, final int y, final Enum idx, final Enum val) {
        super(fontRendererObj, 0, x, y, 16, 16, 0);
        this.currentValue = val;
        this.labelSetting = idx;
        this.fontRenderer = fontRendererObj;

        if (appearances == null) {
            appearances = new HashMap<>();
            registerApp(10, Settings.UNLOCK, LockCraftingMode.NONE, GuiText.NoneLock, null, 0x00FF00);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_WHILE_LOW, GuiText.CraftingLock, GuiText.LowRedstoneLock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_WHILE_HIGH, GuiText.CraftingLock, GuiText.HighRedstoneLock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_UNTIL_PULSE, GuiText.CraftingLock, GuiText.UntilPulseUnlock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_UNTIL_RESULT, GuiText.CraftingLock, GuiText.ResultLock, 0xFF0000);
        }
    }

    private final Enum labelSetting;
    private Enum currentValue;
    private static Map<GuiImgButton.EnumPair, LabelAppearance> appearances;
    private final FontRenderer fontRenderer;

    public void setVisibility(final boolean vis) {
        this.visible = vis;
    }

    @Override
    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            final int iconIndex = this.getIconIndex();
            if (iconIndex == -1) {
                return;
            }
            mc.renderEngine.bindTexture(new ResourceLocation("appliedenergistics2", "textures/guis/states.png"));
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            final int uv_y = (int) Math.floor(iconIndex / 16);
            final int uv_x = iconIndex - uv_y * 16;


            this.drawTexturedModalRect(this.x, this.y, uv_x * 16, uv_y * 16, 16, 16);

            if (labelSetting != null && currentValue != null) {
                LabelAppearance labelAppearance = appearances.get(new GuiImgButton.EnumPair(this.labelSetting, this.currentValue));
                String translated = I18n.translateToLocal(labelAppearance.displayLabel);
                fontRenderer.drawString(translated, x + 16, y + 5, labelAppearance.color);
                width = 16 + fontRenderer.getStringWidth(translated);
            }
        }
    }

    private int getIconIndex() {
        if (this.labelSetting != null && this.currentValue != null) {
            final LabelAppearance app = appearances.get(new GuiImgButton.EnumPair(this.labelSetting, this.currentValue));
            if (app == null) {
                return -1;
            }
            return app.index;
        }
        return -1;
    }

    private void registerApp(final int iconIndex, final Settings setting, final Enum val, final GuiText label, final Object hint, int color) {
        final LabelAppearance a = new LabelAppearance();
        if (hint != null) {
            a.hiddenValue = (String) (hint instanceof String ? hint : ((GuiText) hint).getUnlocalized());
        } else {
            a.hiddenValue = null;
        }
        a.index = iconIndex;
        a.displayLabel = label.getUnlocalized();
        a.color = color;
        appearances.put(new GuiImgButton.EnumPair(setting, val), a);
    }

    @Override
    public String getMessage() {
        if (labelSetting != null && this.currentValue != null) {
            LabelAppearance labelAppearance = appearances.get(new GuiImgButton.EnumPair(this.labelSetting, this.currentValue));
            if (labelAppearance == null) {
                return "No Such Message";
            }

            if (labelAppearance.hiddenValue != null) {
                return I18n.translateToLocal(labelAppearance.hiddenValue);
            }
        }
        return null;
    }

    public void set(final Enum e) {
        if (this.currentValue != e) {
            this.currentValue = e;
        }
    }

    @Override
    public int xPos() {
        return x;
    }

    @Override
    public int yPos() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    private static class LabelAppearance {
        public int index;
        public String displayLabel;
        public String hiddenValue;
        public int color;
    }
}
