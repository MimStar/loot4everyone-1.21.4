package com.loot4everyone;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemFrameData {
    private boolean playerPlaced = false;
    private List<UUID> playersUsed = new ArrayList<>();

    public List<UUID> getPlayersUsed() {
        return playersUsed;
    }

    public boolean isPlayerPlaced() {
        return playerPlaced;
    }

    public void setPlayerPlaced(boolean playerPlaced) {
        this.playerPlaced = playerPlaced;
    }

    public String serializeToString() {
        StringBuilder sb = new StringBuilder();
        // Add playerPlaced
        sb.append(playerPlaced).append(";");
        // Add playersUsed UUIDs
        if (playersUsed == null){
            return sb.toString();
        }
        for (UUID uuid : playersUsed) {
            sb.append(uuid.toString()).append(",");
        }
        // Remove the trailing comma
        if (!playersUsed.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public static ItemFrameData deserializeFromString(String data) {
        if (data == null || data.isEmpty()) return new ItemFrameData();
        ItemFrameData itemFrameData = new ItemFrameData();
        String[] parts = data.split(";");
        if (parts.length >= 1) {
            // Parse playerPlaced
            itemFrameData.setPlayerPlaced(Boolean.parseBoolean(parts[0]));
        }
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            // Parse playersUsed
            String[] uuids = parts[1].split(",");
            for (String uuidStr : uuids) {
                itemFrameData.getPlayersUsed().add(UUID.fromString(uuidStr));
            }
        }
        return itemFrameData;
    }
}
