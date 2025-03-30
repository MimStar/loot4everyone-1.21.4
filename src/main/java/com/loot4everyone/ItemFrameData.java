package com.loot4everyone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemFrameData {
    private boolean playerPlaced = false;
    private List<UUID> playersUsed;

    public ItemFrameData(){
        playersUsed = new ArrayList<>();
    }

    public ItemFrameData(String data){
        deserializeFromString(data);
    }

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
        for (UUID uuid : playersUsed) {
            sb.append(uuid.toString()).append(",");
        }
        // Remove the trailing comma
        if (!playersUsed.isEmpty()) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public void deserializeFromString(String data) {
        String[] parts = data.split(";");
        if (parts.length >= 1) {
            // Parse playerPlaced
            this.setPlayerPlaced(Boolean.parseBoolean(parts[0]));
        }
        if (parts.length >= 2 && !parts[1].isEmpty()) {
            // Parse playersUsed
            String[] uuids = parts[1].split(",");
            for (String uuidStr : uuids) {
                this.getPlayersUsed().add(UUID.fromString(uuidStr));
            }
        }
    }

    public static final Codec<ItemFrameData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("itemframedata").forGetter(ItemFrameData::serializeToString)
    ).apply(instance, ItemFrameData::new));
}
