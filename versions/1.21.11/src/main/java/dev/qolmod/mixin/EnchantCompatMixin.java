package dev.qolmod.mixin;

import dev.qolmod.QoLMod;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides enchantment conflict logic to allow certain combinations that are
 * blocked by vanilla but are considered reasonable quality-of-life improvements.
 *
 * <p><b>Allowed (previously blocked):</b>
 * <ul>
 *   <li>Infinity + Mending (bows / crossbows)</li>
 *   <li>Multishot + Piercing (crossbows)</li>
 *   <li>Sharpness + Smite / Bane of Arthropods combos (swords / axes)</li>
 *   <li>Any other conflicting enchant pair <em>not</em> in the block list below</li>
 * </ul>
 *
 * <p><b>Kept blocked (vanilla intention preserved):</b>
 * <ul>
 *   <li>Fortune + Silk Touch</li>
 * </ul>
 *
 * <p>This injects into {@link Enchantment#canBeCombined} at RETURN so that it
 * only overrides cases where vanilla already returned {@code false}, which means
 * all vanilla-compatible pairs are untouched.
 */
@Mixin(Enchantment.class)
public class EnchantCompatMixin {

    @Inject(
            method = "canBeCombined",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void qolmod$allowCustomCombinations(
            RegistryEntry<Enchantment> first,
            RegistryEntry<Enchantment> second,
            CallbackInfoReturnable<Boolean> cir
    ) {
        // If vanilla already allows this pair, nothing to do
        if (Boolean.TRUE.equals(cir.getReturnValue())) return;

        // Check master config switch
        var config = QoLMod.getConfig();
        if (config == null || !config.enchantConflictOverrideEnabled) return;

        // Keep Fortune + Silk Touch blocked — this is an intentional vanilla restriction
        boolean firstIsFortune   = first.matchesKey(Enchantments.FORTUNE);
        boolean firstIsSilkTouch = first.matchesKey(Enchantments.SILK_TOUCH);
        boolean secondIsFortune   = second.matchesKey(Enchantments.FORTUNE);
        boolean secondIsSilkTouch = second.matchesKey(Enchantments.SILK_TOUCH);

        if ((firstIsFortune && secondIsSilkTouch) || (firstIsSilkTouch && secondIsFortune)) {
            return; // Remain blocked
        }

        // Allow all other conflicting pairs
        cir.setReturnValue(true);
    }
}
