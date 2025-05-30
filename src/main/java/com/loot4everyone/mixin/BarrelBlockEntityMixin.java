package com.loot4everyone.mixin;

import com.loot4everyone.*;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin {

    @Inject(method = "onOpen", at = @At("HEAD"))
    private void onBarrelOpened(PlayerEntity player, CallbackInfo ci){
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        if (StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,barrel.getPos())){
            BarrelViewers.addViewer(player,barrel.getPos());
            if (StateSaverAndLoader.isChestStatePresentInPlayerState(Loot4Everyone.server,player,barrel.getPos())){
                PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server,player);
                List<ItemStack> inventory = playerData.getInventory().get(barrel.getPos());
                for (int i = 0; i < inventory.size(); i++) {
                    barrel.setStack(i, inventory.get(i));
                }
                BarrelViewers.addViewer(player,barrel.getPos());
            }
            else{
                ChestData chestData = StateSaverAndLoader.getChestState(Loot4Everyone.server, barrel.getPos());
                barrel.setLootTable(chestData.getLootTable(), chestData.getLootTableSeed());
                barrel.generateLoot(player);
                BarrelViewers.addViewer(player,barrel.getPos());
            }
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void onBarrelClosed(PlayerEntity player, CallbackInfo ci){
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        if (StateSaverAndLoader.isChestStatePresent(Loot4Everyone.server,barrel.getPos())){
            List<ItemStack> inventory = new ArrayList<>();
            for (int i = 0; i < barrel.size(); i++) {
                inventory.add(barrel.getStack(i));
                barrel.setStack(i, ItemStack.EMPTY);
            }
            PlayerData playerData = StateSaverAndLoader.getPlayerState(Loot4Everyone.server,player);
            playerData.addInventory(barrel.getPos(), inventory);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
            BarrelViewers.removeViewer(player);
        }
    }
}
