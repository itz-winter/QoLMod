package dev.qolmod.features.channel;

import java.util.*;

/**
 * Simple chat channel system.
 * Players can switch between channels; messages are only seen by players in the same channel.
 */
public class ChannelManager {

    public static final String DEFAULT_CHANNEL = "global";

    private final Map<UUID, String> playerChannels = new HashMap<>();
    private final Set<String> availableChannels = new LinkedHashSet<>(Arrays.asList(
            "global", "local", "admin"
    ));

    public String getChannel(UUID playerId) {
        return playerChannels.getOrDefault(playerId, DEFAULT_CHANNEL);
    }

    public void setChannel(UUID playerId, String channel) {
        playerChannels.put(playerId, channel.toLowerCase(Locale.ROOT));
    }

    public Set<String> getAvailableChannels() {
        return Collections.unmodifiableSet(availableChannels);
    }

    public boolean channelExists(String channel) {
        return availableChannels.contains(channel.toLowerCase(Locale.ROOT));
    }

    public void addChannel(String channel) {
        availableChannels.add(channel.toLowerCase(Locale.ROOT));
    }

    public boolean removeChannel(String channel) {
        String lower = channel.toLowerCase(Locale.ROOT);
        if (lower.equals(DEFAULT_CHANNEL)) return false; // Can't remove default
        return availableChannels.remove(lower);
    }

    public void clearPlayer(UUID playerId) {
        playerChannels.remove(playerId);
    }
}
