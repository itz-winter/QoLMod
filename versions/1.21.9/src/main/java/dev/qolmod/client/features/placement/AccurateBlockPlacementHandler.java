package dev.qolmod.client.features.placement;

/**
 * Accurate Block Placement handler.
 * Always enabled, no config UI, no notifications.
 * Automatically disables fast break on servers.
 * Based on AccurateBlockPlacement-Reborn by KadTheHunter.
 *
 * The actual placement logic is injected via ClientPlayerMixin.
 */
public class AccurateBlockPlacementHandler {

    private boolean isOnServer = false;

    public void setOnServer(boolean onServer) {
        this.isOnServer = onServer;
    }

    public boolean isOnServer() {
        return isOnServer;
    }

    /**
     * Should fast break be disabled?
     * Returns true when on a server and config says to disable.
     */
    public boolean shouldDisableFastBreak() {
        return isOnServer;
    }
}
