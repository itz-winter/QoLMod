package dev.qolmod.client.features.invmove;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * InvMove handler — allows player movement while inventory screens are open.
 * Toggleable with keybind, default enabled.
 * Based on InvMove by PieKing1215.
 *
 * The actual movement injection is via KeyboardInputMixin.
 */
public class InvMoveHandler {

    private boolean enabled;

    public InvMoveHandler(boolean defaultState) {
        this.enabled = defaultState;
    }

    public void toggle() {
        enabled = !enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns true if movement keys should be processed while the given screen is open.
     */
    public boolean shouldAllowMovement(Screen screen) {
        if (!enabled || screen == null) return false;

        // Allow movement in common inventory/container screens
        return screen instanceof InventoryScreen
                || screen instanceof CraftingScreen
                || screen instanceof ContainerScreen;
    }
}
