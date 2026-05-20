package dev.qolmod.features.teleport;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Manages /tpa, /tpahere, /tpaccept, /tpdeny, /tpcancel requests.
 */
public class TpaManager {

    private final Map<UUID, TpaRequest> outgoingRequests = new HashMap<>();
    private final Map<UUID, List<TpaRequest>> incomingRequests = new HashMap<>();

    public boolean sendRequest(ServerPlayerEntity sender, ServerPlayerEntity target, boolean here) {
        UUID senderId = sender.getUuid();
        UUID targetId = target.getUuid();

        // Cancel any existing outgoing request
        cancelOutgoing(senderId);

        TpaRequest request = new TpaRequest(senderId, targetId, here, System.currentTimeMillis());
        outgoingRequests.put(senderId, request);
        incomingRequests.computeIfAbsent(targetId, k -> new ArrayList<>()).add(request);

        return true;
    }

    public TpaRequest acceptRequest(UUID targetId, UUID specificSender) {
        List<TpaRequest> incoming = incomingRequests.get(targetId);
        if (incoming == null || incoming.isEmpty()) return null;

        TpaRequest accepted;
        if (specificSender != null) {
            accepted = incoming.stream()
                    .filter(r -> r.senderId.equals(specificSender))
                    .findFirst().orElse(null);
        } else {
            // Accept the most recent
            accepted = incoming.get(incoming.size() - 1);
        }

        if (accepted != null) {
            incoming.remove(accepted);
            outgoingRequests.remove(accepted.senderId);
            return accepted;
        }
        return null;
    }

    public TpaRequest denyRequest(UUID targetId, UUID specificSender) {
        List<TpaRequest> incoming = incomingRequests.get(targetId);
        if (incoming == null || incoming.isEmpty()) return null;

        TpaRequest denied;
        if (specificSender != null) {
            denied = incoming.stream()
                    .filter(r -> r.senderId.equals(specificSender))
                    .findFirst().orElse(null);
        } else {
            denied = incoming.get(incoming.size() - 1);
        }

        if (denied != null) {
            incoming.remove(denied);
            outgoingRequests.remove(denied.senderId);
            return denied;
        }
        return null;
    }

    public void cancelOutgoing(UUID senderId) {
        TpaRequest request = outgoingRequests.remove(senderId);
        if (request != null) {
            List<TpaRequest> incoming = incomingRequests.get(request.targetId);
            if (incoming != null) {
                incoming.remove(request);
            }
        }
    }

    public void tick() {
        long now = System.currentTimeMillis();
        long timeoutMs = 120_000; // 2 minutes

        outgoingRequests.entrySet().removeIf(entry -> {
            if (now - entry.getValue().timestamp > timeoutMs) {
                UUID targetId = entry.getValue().targetId;
                List<TpaRequest> incoming = incomingRequests.get(targetId);
                if (incoming != null) {
                    incoming.remove(entry.getValue());
                }
                return true;
            }
            return false;
        });
    }

    public void cleanup() {
        outgoingRequests.clear();
        incomingRequests.clear();
    }

    public boolean hasOutgoing(UUID senderId) {
        return outgoingRequests.containsKey(senderId);
    }

    public boolean hasIncoming(UUID targetId) {
        List<TpaRequest> incoming = incomingRequests.get(targetId);
        return incoming != null && !incoming.isEmpty();
    }

    public static class TpaRequest {
        public final UUID senderId;
        public final UUID targetId;
        public final boolean tpaHere; // true = target teleports to sender
        public final long timestamp;

        public TpaRequest(UUID senderId, UUID targetId, boolean tpaHere, long timestamp) {
            this.senderId = senderId;
            this.targetId = targetId;
            this.tpaHere = tpaHere;
            this.timestamp = timestamp;
        }
    }
}
