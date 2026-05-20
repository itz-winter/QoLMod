package dev.qolmod.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Mod Menu integration — provides a full in-game config screen.
 * All settings are only editable through this menu; no manual file editing needed.
 */
public class QoLModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return QoLConfigScreen::new;
    }

    // -------------------------------------------------------------------------
    // Main config screen
    // -------------------------------------------------------------------------
    public static class QoLConfigScreen extends Screen {

        private final Screen parent;
        private QoLConfig config;

        // scroll support
        private int scrollOffset = 0;
        private int totalContentHeight = 0;
        private static final int ENTRY_H = 24;
        private static final int TOP_Y   = 36;
        private static final int BTN_W   = 200;
        private static final int BTN_H   = 20;

        protected QoLConfigScreen(Screen parent) {
            super(Text.literal("QoL Mod — Settings"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            config = QoLConfig.getInstance();
            rebuildWidgets();
        }

        private void rebuildWidgets() {
            this.clearChildren();
            int cx = this.width / 2;
            int y  = TOP_Y - scrollOffset;

            // ── Client Features ──────────────────────────────────────────────
            y += 4; // section gap
            addLabel(cx, y, "§e§lClient Features"); y += ENTRY_H;

            addToggle(cx, y, "Fullbright  (G)",          "fullbright.enabled");        y += ENTRY_H;
            addToggle(cx, y, "Tree Chopper",              "treeChopper.enabled");       y += ENTRY_H;
            addToggle(cx, y, "Trade Refresh (singleplayer)", "tradeRefresh.enabled");  y += ENTRY_H;
            addToggle(cx, y, "Accurate Block Placement",  "accurateBlockPlacement.enabled"); y += ENTRY_H;
            addToggle(cx, y, "Recipe Viewer  (/recipe)",  "recipeViewer.enabled");     y += ENTRY_H;
            addToggle(cx, y, "InvMove (move in inventory)","invMove.enabled");         y += ENTRY_H;

            // ── Hunger Display ───────────────────────────────────────────────
            y += 4;
            addLabel(cx, y, "§e§lHunger Display"); y += ENTRY_H;
            addToggle(cx, y, "Hunger Display HUD", "hungerDisplay.enabled"); y += ENTRY_H;

            final int finalY1 = y;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Hunger Display Colours..."),
                    btn -> MinecraftClient.getInstance().setScreen(
                            new HungerDisplaySettingsScreen(this))
            ).dimensions(cx - BTN_W / 2, finalY1, BTN_W, BTN_H).build());
            y += ENTRY_H;

            // ── Compatibility ────────────────────────────────────────────────
            y += 4;
            addLabel(cx, y, "§e§lCompatibility"); y += ENTRY_H;
            addToggle(cx, y, "Override Other Mods (restart)", "overrideOtherMods"); y += ENTRY_H;

            // ── Done ─────────────────────────────────────────────────────────
            y += 8;
            totalContentHeight = y + BTN_H;
            final int finalY2 = y;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Done"),
                    btn -> { config.save(); MinecraftClient.getInstance().setScreen(parent); }
            ).dimensions(cx - BTN_W / 2, finalY2, BTN_W, BTN_H).build());
        }

        // Scroll via mouse wheel
        @Override
        public boolean mouseScrolled(double mx, double my, double hx, double vy) {
            int maxScroll = Math.max(0, totalContentHeight - this.height + 8);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(vy * 10)));
            rebuildWidgets();
            return true;
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            this.renderBackground(ctx, mx, my, delta);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§6§lQoL Mod — Settings"), this.width / 2, 12, 0xFFFFFF);
            super.render(ctx, mx, my, delta);
        }

        @Override
        public void close() {
            config.save();
            MinecraftClient.getInstance().setScreen(parent);
        }

        // ── helpers ──────────────────────────────────────────────────────────

        private void addLabel(int cx, int y, String text) {
            addDrawableChild(new net.minecraft.client.gui.widget.TextWidget(
                    cx - BTN_W / 2, y, BTN_W, 12,
                    Text.literal(text), this.textRenderer));
        }

        private void addToggle(int cx, int y, String label, String key) {
            boolean val = config.getBool(key);
            String state = val ? "§aON" : "§cOFF";
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(label + ": " + state),
                    btn -> {
                        boolean nv = !config.getBool(key);
                        config.put(key, nv);
                        btn.setMessage(Text.literal(label + ": " + (nv ? "§aON" : "§cOFF")));
                    }
            ).dimensions(cx - BTN_W / 2, y, BTN_W, BTN_H).build());
        }
    }

    // -------------------------------------------------------------------------
    // Hunger display colour sub-screen
    // -------------------------------------------------------------------------
    public static class HungerDisplaySettingsScreen extends Screen {

        private final Screen parent;
        private QoLConfig config;

        private TextFieldWidget satColorField;
        private TextFieldWidget exhaustColorField;
        private TextFieldWidget foodColorField;

        protected HungerDisplaySettingsScreen(Screen parent) {
            super(Text.literal("Hunger Display — Colours"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            config = QoLConfig.getInstance();
            int cx = this.width / 2;
            int y  = 48;
            int fw = 120; // field width

            // Saturation colour
            addLabel(cx, y, "Saturation pip colour (hex RRGGBB)"); y += 14;
            satColorField = new TextFieldWidget(this.textRenderer, cx - fw / 2, y, fw, 18,
                    Text.literal("Saturation Colour"));
            satColorField.setMaxLength(7);
            satColorField.setText(config.getStr("hungerDisplay.saturationColor", "FF8800"));
            addDrawableChild(satColorField);
            y += 24;

            // Exhaustion colour
            addLabel(cx, y, "Exhaustion bar colour (hex RRGGBB)"); y += 14;
            exhaustColorField = new TextFieldWidget(this.textRenderer, cx - fw / 2, y, fw, 18,
                    Text.literal("Exhaustion Colour"));
            exhaustColorField.setMaxLength(7);
            exhaustColorField.setText(config.getStr("hungerDisplay.exhaustionColor", "FF4400"));
            addDrawableChild(exhaustColorField);
            y += 24;

            // Food-value colour
            addLabel(cx, y, "Food preview pip colour (hex RRGGBB)"); y += 14;
            foodColorField = new TextFieldWidget(this.textRenderer, cx - fw / 2, y, fw, 18,
                    Text.literal("Food Colour"));
            foodColorField.setMaxLength(7);
            foodColorField.setText(config.getStr("hungerDisplay.foodValueColor", "AAFFAA"));
            addDrawableChild(foodColorField);
            y += 30;

            // Live colour previews are shown in render()

            // Buttons
            int bw = 90;
            addDrawableChild(ButtonWidget.builder(Text.literal("Save & Back"),
                    btn -> saveAndClose()
            ).dimensions(cx - bw - 4, y, bw, 20).build());

            addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"),
                    btn -> MinecraftClient.getInstance().setScreen(parent)
            ).dimensions(cx + 4, y, bw, 20).build());
        }

        private void saveAndClose() {
            config.put("hungerDisplay.saturationColor", sanitize(satColorField.getText()));
            config.put("hungerDisplay.exhaustionColor",  sanitize(exhaustColorField.getText()));
            config.put("hungerDisplay.foodValueColor",   sanitize(foodColorField.getText()));
            config.save();
            MinecraftClient.getInstance().setScreen(parent);
        }

        /** Remove leading # if present, uppercase. */
        private String sanitize(String s) {
            s = s.trim().toUpperCase();
            if (s.startsWith("#")) s = s.substring(1);
            return s;
        }

        @Override
        public void render(DrawContext ctx, int mx, int my, float delta) {
            this.renderBackground(ctx, mx, my, delta);
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§6§lHunger Display — Colours"), this.width / 2, 16, 0xFFFFFF);
            super.render(ctx, mx, my, delta);

            // Colour swatches next to each field
            renderSwatch(ctx, satColorField,    satColorField.getText());
            renderSwatch(ctx, exhaustColorField, exhaustColorField.getText());
            renderSwatch(ctx, foodColorField,    foodColorField.getText());
        }

        private void renderSwatch(DrawContext ctx, TextFieldWidget field, String hex) {
            try {
                int color = 0xFF000000 | Integer.parseInt(sanitize(hex), 16);
                int sx = field.getX() + field.getWidth() + 6;
                int sy = field.getY();
                ctx.fill(sx, sy, sx + 18, sy + 18, color);
                ctx.drawBorder(sx, sy, 18, 18, 0xFFFFFFFF);
            } catch (NumberFormatException ignored) { /* bad hex — no swatch */ }
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }

        private void addLabel(int cx, int y, String text) {
            addDrawableChild(new net.minecraft.client.gui.widget.TextWidget(
                    cx - 130, y, 260, 12,
                    Text.literal(text), this.textRenderer));
        }
    }
}

