package dev.qolmod.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/** Mod Menu integration for 1.19.4 (MatrixStack, ButtonWidget.builder). */
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
            super(Text.literal("QoL Mod - Settings"));
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

            y += 4;
            addLabel(cx, y, "§e§lClient Features"); y += ENTRY_H;
            addToggle(cx, y, "Fullbright  (G)", "fullbright.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Tree Chopper", "treeChopper.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Trade Refresh (singleplayer)", "tradeRefresh.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Accurate Block Placement", "accurateBlockPlacement.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Recipe Viewer  (/recipe)", "recipeViewer.enabled"); y += ENTRY_H;
            addToggle(cx, y, "InvMove (move in inventory)", "invMove.enabled"); y += ENTRY_H;

            y += 4;
            addLabel(cx, y, "§e§lHunger Display"); y += ENTRY_H;
            addToggle(cx, y, "Hunger Display HUD", "hungerDisplay.enabled"); y += ENTRY_H;
            final int fy1 = y;
            addDrawableChild(ButtonWidget.builder(Text.literal("Hunger Display Colours..."),
                    btn -> MinecraftClient.getInstance().setScreen(new HungerDisplaySettingsScreen(this))
            ).dimensions(cx - BTN_W / 2, fy1, BTN_W, BTN_H).build());
            y += ENTRY_H;

            y += 4;
            addLabel(cx, y, "§e§lCompatibility"); y += ENTRY_H;
            addToggle(cx, y, "Override Other Mods (restart)", "overrideOtherMods"); y += ENTRY_H;

            y += 8;
            totalContentHeight = y + BTN_H;
            final int fy2 = y;
            addDrawableChild(ButtonWidget.builder(Text.literal("Done"),
                    btn -> { config.save(); MinecraftClient.getInstance().setScreen(parent); }
            ).dimensions(cx - BTN_W / 2, fy2, BTN_W, BTN_H).build());
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
            drawCenteredTextWithShadow(matrices, this.textRenderer,
                    Text.literal("§6§lQoL Mod - Settings"), this.width / 2, 12, 0xFFFFFF);
            super.render(matrices, mx, my, delta);
        }

        @Override
        public void close() { config.save(); MinecraftClient.getInstance().setScreen(parent); }

        private void addLabel(int cx, int y, String text) {
            addDrawableChild(new TextWidget(cx - BTN_W / 2, y, BTN_W, 12,
                    Text.literal(text), this.textRenderer));
        }

        private void addToggle(int cx, int y, String label, String key) {
            boolean val = config.getBool(key);
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(label + ": " + (val ? "§aON" : "§cOFF")),
                    btn -> {
                        boolean nv = !config.getBool(key);
                        config.put(key, nv);
                        btn.setMessage(Text.literal(label + ": " + (nv ? "§aON" : "§cOFF")));
                    }
            ).dimensions(cx - BTN_W / 2, y, BTN_W, BTN_H).build());
        }
    }

    public static class HungerDisplaySettingsScreen extends Screen {
        private final Screen parent;
        private QoLConfig config;
        private TextFieldWidget satColorField, exhaustColorField, foodColorField;

        protected HungerDisplaySettingsScreen(Screen parent) {
            super(Text.literal("Hunger Display - Colours"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            config = QoLConfig.getInstance();
            int cx = this.width / 2, y = 48, fw = 120;

            addLabel(cx, y, "Saturation pip colour (hex RRGGBB)"); y += 14;
            satColorField = new TextFieldWidget(this.textRenderer, cx - fw/2, y, fw, 18, Text.literal("Sat"));
            satColorField.setMaxLength(7);
            satColorField.setText(config.getStr("hungerDisplay.saturationColor", "FFAA00").replace("#",""));
            addDrawableChild(satColorField); y += 24;

            addLabel(cx, y, "Exhaustion bar colour (hex RRGGBB)"); y += 14;
            exhaustColorField = new TextFieldWidget(this.textRenderer, cx - fw/2, y, fw, 18, Text.literal("Exh"));
            exhaustColorField.setMaxLength(7);
            exhaustColorField.setText(config.getStr("hungerDisplay.exhaustionColor", "FF4400").replace("#",""));
            addDrawableChild(exhaustColorField); y += 24;

            addLabel(cx, y, "Food preview pip colour (hex RRGGBB)"); y += 14;
            foodColorField = new TextFieldWidget(this.textRenderer, cx - fw/2, y, fw, 18, Text.literal("Food"));
            foodColorField.setMaxLength(7);
            foodColorField.setText(config.getStr("hungerDisplay.foodValueColor", "AAFFAA").replace("#",""));
            addDrawableChild(foodColorField); y += 30;

            int bw = 90;
            addDrawableChild(ButtonWidget.builder(Text.literal("Save & Back"), btn -> saveAndClose())
                    .dimensions(cx - bw - 4, y, bw, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                    btn -> MinecraftClient.getInstance().setScreen(parent))
                    .dimensions(cx + 4, y, bw, 20).build());
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
            drawCenteredTextWithShadow(matrices, this.textRenderer,
                    Text.literal("§6§lHunger Display - Colours"), this.width / 2, 16, 0xFFFFFF);
            super.render(matrices, mx, my, delta);
            renderSwatch(matrices, satColorField, satColorField.getText());
            renderSwatch(matrices, exhaustColorField, exhaustColorField.getText());
            renderSwatch(matrices, foodColorField, foodColorField.getText());
        }

        private void renderSwatch(MatrixStack matrices, TextFieldWidget field, String hex) {
            try {
                int color = 0xFF000000 | Integer.parseInt(sanitize(hex), 16);
                int sx = field.getX() + field.getWidth() + 6, sy = field.getY();
                fill(matrices, sx, sy, sx+18, sy+18, color);
                fill(matrices, sx, sy, sx+18, sy+1, 0xFFFFFFFF);
                fill(matrices, sx, sy+17, sx+18, sy+18, 0xFFFFFFFF);
                fill(matrices, sx, sy, sx+1, sy+18, 0xFFFFFFFF);
                fill(matrices, sx+17, sy, sx+18, sy+18, 0xFFFFFFFF);
            } catch (NumberFormatException ignored) {}
        }

        @Override
        public void close() { MinecraftClient.getInstance().setScreen(parent); }

        private void addLabel(int cx, int y, String text) {
            addDrawableChild(new TextWidget(cx - 130, y, 260, 12, Text.literal(text), this.textRenderer));
        }
    }
}