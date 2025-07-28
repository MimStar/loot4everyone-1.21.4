package com.loot4everyone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SettingsData {
    private boolean isLootProtectionEnabled;

    public SettingsData(){
        isLootProtectionEnabled = true;
    }

    public SettingsData(String isLootProtectionEnabled){
        this.isLootProtectionEnabled = Boolean.parseBoolean(isLootProtectionEnabled);
    }

    public void setLootProtection(boolean isLootProtectionEnabled){
        this.isLootProtectionEnabled = isLootProtectionEnabled;
    }

    public boolean getLootProtection(){
        return this.isLootProtectionEnabled;
    }

    public String toString(){
        return String.valueOf(this.isLootProtectionEnabled);
    }

    public static final Codec<SettingsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("settings").forGetter(SettingsData::toString)
    ).apply(instance, SettingsData::new));

}
