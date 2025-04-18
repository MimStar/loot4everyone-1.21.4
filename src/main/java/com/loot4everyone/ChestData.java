package com.loot4everyone;

import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public class ChestData {
    private long lootTableSeed;
    private RegistryKey<LootTable> lootTable;

    public long getLootTableSeed(){
        return lootTableSeed;
    }

    public RegistryKey<LootTable> getLootTable(){
        return lootTable;
    }

    public void setLootTableSeed(long seed){
        lootTableSeed = seed;
    }

    public void setLootTable(RegistryKey<LootTable> table){
        lootTable = table;
    }

    public String chestDataToString() {
        if (lootTable == null) {
            return "";
        }
        return lootTableSeed + "=" + lootTable.getValue().toString();
    }

    public void stringToChestData(String data) {
        if (data == null || data.isEmpty()) return;
        String[] parts = data.split("=");
        if (parts.length == 2) {
            lootTableSeed = Long.parseLong(parts[0]);
            lootTable = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(parts[1]));
        }
    }


}
