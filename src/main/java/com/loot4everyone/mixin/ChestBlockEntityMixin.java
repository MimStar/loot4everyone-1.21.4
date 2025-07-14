package com.loot4everyone.mixin;

import com.loot4everyone.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin {

    @Inject(method = "onOpen", at = @At("HEAD"))
    private void onChestOpened(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        BlockPos blockPos = StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server, chest);
        BlockState blockState = chest.getCachedState();
        if (blockPos != null) {
            if (blockState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                if (StateSaverAndLoader.isBarrelStatePresentInPlayerState(Loot4Everyone.server, player, blockPos)) {
                    PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server, player);
                    List<ItemStack> inventory = playerData.getInventory().get(blockPos);
                    for (int i = 0; i < inventory.size(); i++) {
                        chest.setStack(i, inventory.get(i));
                    }
                } else {
                    ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, blockPos);
                    chest.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
                    chest.generateLoot(player);
                }
            }
            else if (blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) {
                BlockPos rightPos = blockPos.offset(blockState.get(ChestBlock.FACING).rotateYClockwise());

                if (StateSaverAndLoader.isBarrelStatePresentInPlayerState(Loot4Everyone.server, player, blockPos)) {
                    // Load both chests from player state
                    PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server, player);
                    List<ItemStack> inventory = playerData.getInventory().get(blockPos);

                    // Left chest
                    for (int i = 0; i < 27; i++) { // First half for left chest
                        chest.setStack(i, inventory.get(i));
                    }

                    // Right chest
                    BlockEntity rightEntity = player.getWorld().getBlockEntity(rightPos);
                    if (rightEntity instanceof ChestBlockEntity rightChest) {
                        for (int i = 27; i < 54; i++) { // Second half for right chest
                            rightChest.setStack(i - 27, inventory.get(i));
                        }
                    }
                } else {
                    // Generate loot only for left chest
                    ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, blockPos);
                    chest.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
                    chest.generateLoot(player);
                }
            }
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onChestClosed(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        BlockPos blockPos = StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server, chest);
        BlockState blockState = chest.getCachedState();

        if (blockPos != null) {
            PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server, player);
            List<ItemStack> inventory = new ArrayList<>();

            if (blockState.get(ChestBlock.CHEST_TYPE) == ChestType.SINGLE) {
                // Single chest - just save 27 slots
                for (int i = 0; i < chest.size(); i++) {
                    inventory.add(chest.getStack(i));
                    chest.setStack(i, ItemStack.EMPTY);
                }
            }
            else if (blockState.get(ChestBlock.CHEST_TYPE) == ChestType.LEFT) {
                // Double chest - save both left and right (54 slots total)
                BlockPos rightPos = blockPos.offset(blockState.get(ChestBlock.FACING).rotateYClockwise());
                BlockEntity rightEntity = player.getWorld().getBlockEntity(rightPos);

                // Save left chest (first 27 slots)
                for (int i = 0; i < chest.size(); i++) {
                    inventory.add(chest.getStack(i));
                    chest.setStack(i, ItemStack.EMPTY);
                }

                // Save right chest (next 27 slots) if exists
                if (rightEntity instanceof ChestBlockEntity rightChest) {
                    for (int i = 0; i < rightChest.size(); i++) {
                        inventory.add(rightChest.getStack(i));
                        rightChest.setStack(i, ItemStack.EMPTY);
                    }
                }
            }

            playerData.addInventory(blockPos, inventory);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
        }
    }


}