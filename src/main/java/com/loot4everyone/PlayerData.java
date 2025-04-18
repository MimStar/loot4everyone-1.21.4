package com.loot4everyone;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class PlayerData {
    private HashMap<BlockPos, List<ItemStack>> inventory = new HashMap<>();

    public PlayerData(){
        inventory = new HashMap<>();
    }

    public PlayerData(String inventoryString){
        stringToInventory(inventoryString);
    }

    public HashMap<BlockPos, List<ItemStack>> getInventory() {
        return inventory;
    }

    public void addInventory(BlockPos pos, List<ItemStack> stacks){
        inventory.put(pos, stacks);
    }

    public void setInventory(HashMap<BlockPos, List<ItemStack>> inventory){
        this.inventory = inventory;
    }

    public String inventoryToString() {
        if (inventory == null){
            return "";
        }
        StringJoiner joiner = new StringJoiner(";");
        for (Map.Entry<BlockPos, List<ItemStack>> entry : inventory.entrySet()) {
            String pos = entry.getKey().getX() + "," + entry.getKey().getY() + "," + entry.getKey().getZ();
            StringJoiner itemJoiner = new StringJoiner(",");
            for (ItemStack stack : entry.getValue()) {
                itemJoiner.add(Item.getRawId(stack.getItem()) + "%" + stack.getCount());
            }
            joiner.add(pos + "=" + itemJoiner);
        }
        return joiner.toString();
    }

    public void stringToInventory(String data) {
        if (inventory != null){
            inventory.clear();
        }
        else{
            inventory = new HashMap<>();
        }
        if (data == null || data.isEmpty()) {
            return;
        }
        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] parts = entry.split("=");
            String[] posParts = parts[0].split(",");
            BlockPos pos = new BlockPos(Integer.parseInt(posParts[0]), Integer.parseInt(posParts[1]), Integer.parseInt(posParts[2]));
            List<ItemStack> stacks = new ArrayList<>();
            for (String itemData : parts[1].split(",")) {
                String[] itemParts = itemData.split("%");
                ItemStack itemStack = new ItemStack(RegistryEntry.of(Item.byRawId(Integer.parseInt(itemParts[0]))),Integer.parseInt(itemParts[1]));
                stacks.add(itemStack);
            }
            inventory.put(pos, stacks);
        }
    }

    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("inventory").forGetter(PlayerData::inventoryToString)
    ).apply(instance, PlayerData::new));


}
