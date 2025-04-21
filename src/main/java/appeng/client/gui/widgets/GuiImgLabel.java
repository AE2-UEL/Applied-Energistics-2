package appeng.client.gui.widgets;

import appeng.api.config.LockCraftingMode;
import appeng.api.config.Settings;
import appeng.core.localization.GuiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiLabel;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class GuiImgLabel extends GuiLabel implements ITooltip {
    public GuiImgLabel(FontRenderer fontRendererObj, final int x, final int y, final Enum idx, final Enum val) {
        super(fontRendererObj, 0, x, y, 16, 16, 0);
        this.currentValue = val;
        this.labelSetting = idx;

        if (appearances == null) {
            appearances = new HashMap<>();
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_WHILE_LOW, GuiText.CraftingLock, GuiText.LowRedstoneLock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_WHILE_HIGH, GuiText.CraftingLock, GuiText.HighRedstoneLock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_UNTIL_PULSE, GuiText.CraftingLock, GuiText.UntilPulseUnlock, 0xFF0000);
            registerApp(9, Settings.UNLOCK, LockCraftingMode.LOCK_UNTIL_RESULT, GuiText.CraftingLock, GuiText.ResultLock, 0xFF0000);
        }
    }

    private final Enum labelSetting;
    private Enum currentValue;
    private static Map<GuiImgButton.EnumPair, LabelAppearance> appearances;

    @Override
    public void drawLabel(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        if (this.labelSetting == null || this.currentValue == null) return;

        if (!appearances.containsKey(new GuiImgButton.EnumPair(this.labelSetting, this.currentValue))) return;

        final int iconIndex = this.getIconIndex();
        if (iconIndex == -1) {
            return;
        }
        mc.renderEngine.bindTexture(new ResourceLocation("appliedenergistics2", "textures/guis/states.png"));
        GlStateManager.color(1.0f, 0.0f, 0.0f, 1.0f);
        final int uv_y = iconIndex / 16;
        final int uv_x = iconIndex - uv_y * 16;


        this.drawTexturedModalRect(this.x, this.y, uv_x * 16, uv_y * 16, 16, 16);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
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

    private void registerApp(final int iconIndex, final Settings setting, final Enum val, final GuiText title, final GuiText hint, int color) {
        final LabelAppearance a = new LabelAppearance();
        a.title = title.getUnlocalized();
        a.hiddenValue = hint.getUnlocalized();
        a.index = iconIndex;
        a.color = color;
        appearances.put(new GuiImgButton.EnumPair(setting, val), a);
    }

    @Override
    public String getMessage() {
        LabelAppearance labelAppearance = appearances.get(new GuiImgButton.EnumPair(this.labelSetting, this.currentValue));
        if (labelAppearance == null) {
            return "No Such Message";
        }

        return I18n.format(labelAppearance.title) + "\n" + I18n.format(labelAppearance.hiddenValue);
    }

    public void set(final Enum e) {
        this.currentValue = e;
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
        public String title;
        public String hiddenValue;
        public int color;
    }
}
