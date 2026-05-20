package dev.qolmod.client.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.qolmod.config.QoLConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
            int yr = TOP_Y; // unscrolled y — used to compute total content height

            // ── Client Features ──────────────────────────────────────────────
            y += 4; yr += 4; // section gap
            addLabel(cx, y, "§e§lClient Features"); y += ENTRY_H; yr += ENTRY_H;

            addToggle(cx, y, "Fullbright  (G)",          "fullbright.enabled");        y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Tree Chopper",              "treeChopper.enabled");       y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Trade Refresh (singleplayer)", "tradeRefresh.enabled");  y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Accurate Block Placement",  "accurateBlockPlacement.enabled"); y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Recipe Viewer  (/recipe)",  "recipeViewer.enabled");     y += ENTRY_H; yr += ENTRY_H;

            // ── Entity Features ──────────────────────────────────────────────
            y += 4; yr += 4;
            addLabel(cx, y, "§e§lEntity Features"); y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Villager in a Bucket",        "villagerBucketEnabled");       y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Zombie Villager in a Bucket", "villagerBucketZombieEnabled"); y += ENTRY_H; yr += ENTRY_H;

            // ── Compatibility ────────────────────────────────────────────────
            y += 4; yr += 4;
            addLabel(cx, y, "§e§lCompatibility"); y += ENTRY_H; yr += ENTRY_H;
            addToggle(cx, y, "Override Other Mods (restart)", "overrideOtherMods"); y += ENTRY_H; yr += ENTRY_H;

            // ── Done ─────────────────────────────────────────────────────────
            y += 8; yr += 8;
            final int finalY2 = y;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal("Done"),
                    btn -> { config.save(); MinecraftClient.getInstance().setScreen(parent); }
            ).dimensions(cx - BTN_W / 2, finalY2, BTN_W, BTN_H).build());

            totalContentHeight = yr + BTN_H;
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
            super.render(ctx, mx, my, delta);
            // renderBackground is called by super.render in 1.21.11 — calling it
            // again here causes "Can only blur once per frame" IllegalStateException.
            ctx.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("§6§lQoL Mod — Settings"), this.width / 2, 12, 0xFFFFFF);
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
}

