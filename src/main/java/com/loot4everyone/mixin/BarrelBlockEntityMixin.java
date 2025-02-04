package com.loot4everyone.mixin;

import com.loot4everyone.BarrelViewers;
import com.loot4everyone.PlayerData;
import com.loot4everyone.StateSaverAndLoader;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Mixin(BarrelBlockEntity.class)
public abstract class BarrelBlockEntityMixin {
    @Inject(method = "onClose", at = @At("HEAD"))
    private void onBarrelClosed(PlayerEntity player, CallbackInfo ci){
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        if (StateSaverAndLoader.isChestStatePresent(player,barrel.getPos())){
            List<ItemStack> inventory = new ArrayList<>();
            for (int i = 0; i < barrel.size(); i++) {
                inventory.add(barrel.getStack(i));
                barrel.setStack(i, ItemStack.EMPTY);
            }
            PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
            playerData.addInventory(barrel.getPos(), inventory);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
            BarrelViewers.removeViewer(player);
        }
    }
}
