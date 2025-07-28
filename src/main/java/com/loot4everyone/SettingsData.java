package com.loot4everyone;

public class SettingsData {
    private boolean isLootProtectionEnabled;

    public SettingsData(){
        isLootProtectionEnabled = true;
    }

    public void setLootProtection(boolean isLootProtectionEnabled){
        this.isLootProtectionEnabled = isLootProtectionEnabled;
    }

    public boolean getLootProtection(){
        return this.isLootProtectionEnabled;
    }

    public void setSettings(String settings){
        this.isLootProtectionEnabled = Boolean.parseBoolean(settings);
    }

    public String toString(){
        return String.valueOf(this.isLootProtectionEnabled);
    }

}
