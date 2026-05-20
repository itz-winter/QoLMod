package dev.qolmod.mixin.client.accessor;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes Screen.addDrawableChild (protected) so version-specific mixins
 * can add widgets without shadowing the method — whose intermediary name
 * changed in 1.21.11.
 */
@Mixin(Screen.class)
public interface ScreenInvoker {

    @Invoker("addDrawableChild")
    <T extends Element & Drawable & Selectable> T invokeAddDrawableChild(T element);
}
