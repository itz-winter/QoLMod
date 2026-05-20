package dev.qolmod.features.homes;

import dev.qolmod.QoLMod;
import dev.qolmod.gamerule.QoLGameRules;
import dev.qolmod.teleport.TeleportManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Set;

/**
 * Utility for teleporting to a named home.
 * Respects the configured teleport countdown.
 */
public class HomeTeleportHelper {

    public static void teleport(ServerPlayerEntity player, HomeManager homeManager, String name) {
        HomeManager.HomeData home = homeManager.getHome(player.getUuid(), name);
        if (home == null) {
            player.sendMessage(Text.literal("§cHome not found: " + name));
            return;
        }

        ServerWorld world = QoLMod.getServer() != null
                ? QoLMod.getServer().getWorld(home.dimension)
                : null;
        if (world == null) {
            player.sendMessage(Text.literal("§cThe dimension for this home no longer exists."));
            return;
        }

        // Record back location
        if (QoLMod.getBackManager() != null) {
            QoLMod.getBackManager().recordTeleport(player);
        }

        int countdown = world.getGameRules().getValue(QoLGameRules.TP_COUNTDOWN_SECONDS);
        TeleportManager.requestTeleport(
                player, world,
                home.pos.getX() + 0.5, home.pos.getY(), home.pos.getZ() + 0.5,
                home.yaw, home.pitch,
                countdown,
                "§aTeleported to home: §f" + home.name
        );
    }
}
