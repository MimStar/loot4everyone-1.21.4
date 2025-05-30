package com.loot4everyone.mixin;

import com.loot4everyone.*;
import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
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
        if (StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server, chest.getPos())) {
            if (StateSaverAndLoader.isChestStatePresentInPlayerState(Loot4Everyone.server, player, chest.getPos())) {
                PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server, player);
                List<ItemStack> inventory = playerData.getInventory().get(chest.getPos());
                for (int i = 0; i < inventory.size(); i++) {
                    chest.setStack(i, inventory.get(i));
                }
            } else {
                ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, chest.getPos());
                chest.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
                chest.generateLoot(player);
            }
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onChestClosed(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        if (StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server, chest.getPos())) {
            List<ItemStack> inventory = new ArrayList<>();
            for (int i = 0; i < chest.size(); i++) {
                inventory.add(chest.getStack(i));
                chest.setStack(i, ItemStack.EMPTY);
            }
            PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server, player);
            playerData.addInventory(chest.getPos(), inventory);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
        }
    }

}
