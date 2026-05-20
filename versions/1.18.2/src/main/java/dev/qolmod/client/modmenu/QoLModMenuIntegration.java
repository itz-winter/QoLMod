package dev.qolmod.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;

/** Mod Menu integration for 1.18.2 (LiteralText, old ButtonWidget ctor, MatrixStack). */
public class QoLModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return QoLConfigScreen::new;
    }

    public static class QoLConfigScreen extends Screen {
        private final Screen parent;
        private QoLConfig config;
        private int scrollOffset = 0;
        private int totalContentHeight = 0;
        private static final int ENTRY_H = 24, TOP_Y = 36, BTN_W = 200, BTN_H = 20;

        protected QoLConfigScreen(Screen parent) {
            super(new LiteralText("QoL Mod - Settings"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            config = QoLConfig.getInstance();
            rebuildWidgets();
        }

        private void rebuildWidgets() {
            this.clearChildren();
            int cx = this.width / 2, y = TOP_Y - scrollOffset;

            y += ENTRY_H;
            addToggle(cx, y, "Fullbright (G)", "fullbright.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Tree Chopper", "treeChopper.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Trade Refresh (singleplayer)", "tradeRefresh.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Accurate Block Placement", "accurateBlockPlacement.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Recipe Viewer (/recipe)", "recipeViewer.enabled"); y += ENTRY_H;
            addToggle(cx, y, "InvMove", "invMove.enabled"); y += ENTRY_H;

            y += ENTRY_H;
            addToggle(cx, y, "Hunger Display HUD", "hungerDisplay.enabled"); y += ENTRY_H;
            final int fy1 = y;
            addDrawableChild(new ButtonWidget(cx - BTN_W/2, fy1, BTN_W, BTN_H,
                    new LiteralText("Hunger Display Colours..."),
                    btn -> MinecraftClient.getInstance().setScreen(new HungerDisplaySettingsScreen(this))));
            y += ENTRY_H;

            y += ENTRY_H;
            addToggle(cx, y, "Override Other Mods (restart)", "overrideOtherMods"); y += ENTRY_H;

            y += 8;
            totalContentHeight = y + BTN_H;
            final int fy2 = y;
            addDrawableChild(new ButtonWidget(cx - BTN_W/2, fy2, BTN_W, BTN_H,
                    new LiteralText("Done"),
                    btn -> { config.save(); MinecraftClient.getInstance().setScreen(parent); }));
        }

        @Override
        public boolean mouseScrolled(double mx, double my, double amount) {
            int maxScroll = Math.max(0, totalContentHeight - this.height + 8);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(amount * 10)));
            rebuildWidgets();
            return true;
        }

        @Override
        public void render(MatrixStack matrices, int mx, int my, float delta) {
            this.renderBackground(matrices);
            int cx = this.width / 2, y = TOP_Y - scrollOffset;
            DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("QoL Mod - Settings").asOrderedText(), cx, 12, 0xFFAA00);
            DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("[ Client Features ]").asOrderedText(), cx, y + 6, 0xFFEE44);
            int featY = y + ENTRY_H * 7 + 4;
            DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("[ Hunger Display ]").asOrderedText(), cx, featY + 6, 0xFFEE44);
            int compatY = featY + ENTRY_H * 3 + 4;
            DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("[ Compatibility ]").asOrderedText(), cx, compatY + 6, 0xFFEE44);
            super.render(matrices, mx, my, delta);
        }

        @Override
        public void close() { config.save(); MinecraftClient.getInstance().setScreen(parent); }

        private void addToggle(int cx, int y, String label, String key) {
            boolean val = config.getBool(key);
            addDrawableChild(new ButtonWidget(cx - BTN_W/2, y, BTN_W, BTN_H,
                    new LiteralText(label + ": " + (val ? "ON" : "OFF")),
                    btn -> {
                        boolean nv = !config.getBool(key);
                        config.put(key, nv);
                        btn.setMessage(new LiteralText(label + ": " + (nv ? "ON" : "OFF")));
                    }));
        }
    }

    public static class HungerDisplaySettingsScreen extends Screen {
        private final Screen parent;
        private QoLConfig config;
        private TextFieldWidget satColorField, exhaustColorField, foodColorField;
        private int fieldX, fieldW, satFieldY, exhFieldY, foodFieldY;

        protected HungerDisplaySettingsScreen(Screen parent) {
            super(new LiteralText("Hunger Display - Colours"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            config = QoLConfig.getInstance();
            int cx = this.width / 2, y = 48, fw = 120;
            fieldX = cx-fw/2; fieldW = fw;

            satFieldY = y+14;
            satColorField = new TextFieldWidget(this.textRenderer, fieldX, satFieldY, fw, 18, new LiteralText("Sat"));
            satColorField.setMaxLength(7);
            satColorField.setText(config.getStr("hungerDisplay.saturationColor","FFAA00").replace("#",""));
            addDrawableChild(satColorField); y += 38;

            exhFieldY = y+14;
            exhaustColorField = new TextFieldWidget(this.textRenderer, fieldX, exhFieldY, fw, 18, new LiteralText("Exh"));
            exhaustColorField.setMaxLength(7);
            exhaustColorField.setText(config.getStr("hungerDisplay.exhaustionColor","FF4400").replace("#",""));
            addDrawableChild(exhaustColorField); y += 38;

            foodFieldY = y+14;
            foodColorField = new TextFieldWidget(this.textRenderer, fieldX, foodFieldY, fw, 18, new LiteralText("Food"));
            foodColorField.setMaxLength(7);
            foodColorField.setText(config.getStr("hungerDisplay.foodValueColor","AAFFAA").replace("#",""));
            addDrawableChild(foodColorField); y += 48;

            int bw = 90;
            addDrawableChild(new ButtonWidget(cx-bw-4, y, bw, 20, new LiteralText("Save & Back"), btn -> saveAndClose()));
            addDrawableChild(new ButtonWidget(cx+4, y, bw, 20, new LiteralText("Cancel"),
                    btn -> MinecraftClient.getInstance().setScreen(parent)));
        }

        private void saveAndClose() {
            config.put("hungerDisplay.saturationColor", sanitize(satColorField.getText()));
            config.put("hungerDisplay.exhaustionColor", sanitize(exhaustColorField.getText()));
            config.put("hungerDisplay.foodValueColor", sanitize(foodColorField.getText()));
            config.save();
            MinecraftClient.getInstance().setScreen(parent);
        }

        private String sanitize(String s) {
            s = s.trim().toUpperCase();
            return s.startsWith("#") ? s.substring(1) : s;
        }

        @Override
        public void render(MatrixStack matrices, int mx, int my, float delta) {
            this.renderBackground(matrices);
            DrawableHelper.drawCenteredTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("Hunger Display - Colours").asOrderedText(), this.width/2, 16, 0xFFAA00);
            DrawableHelper.drawTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("Saturation pip colour (hex RRGGBB)"), cx()-130, 48, 0xCCCCCC);
            DrawableHelper.drawTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("Exhaustion bar colour (hex RRGGBB)"), cx()-130, 86, 0xCCCCCC);
            DrawableHelper.drawTextWithShadow(matrices, this.textRenderer,
                    new LiteralText("Food preview pip colour (hex RRGGBB)"), cx()-130, 124, 0xCCCCCC);
            super.render(matrices, mx, my, delta);
            renderSwatch(matrices, satColorField.getText(), fieldX, satFieldY, fieldW);
            renderSwatch(matrices, exhaustColorField.getText(), fieldX, exhFieldY, fieldW);
            renderSwatch(matrices, foodColorField.getText(), fieldX, foodFieldY, fieldW);
        }

        private int cx() { return this.width / 2; }

        private void renderSwatch(MatrixStack matrices, String hex, int fieldX, int fieldY, int fieldW) {
            try {
                int color = 0xFF000000 | Integer.parseInt(sanitize(hex), 16);
                int sx = fieldX + fieldW + 6, sy = fieldY;
                fill(matrices, sx, sy, sx+18, sy+18, color);
                fill(matrices, sx, sy, sx+18, sy+1, 0xFFFFFFFF);
                fill(matrices, sx, sy+17, sx+18, sy+18, 0xFFFFFFFF);
                fill(matrices, sx, sy, sx+1, sy+18, 0xFFFFFFFF);
                fill(matrices, sx+17, sy, sx+18, sy+18, 0xFFFFFFFF);
            } catch (NumberFormatException ignored) {}
        }

        @Override
        public void close() { MinecraftClient.getInstance().setScreen(parent); }
    }
}
