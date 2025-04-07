package com.loot4everyone.mixin;

import com.loot4everyone.PlayerData;
import com.loot4everyone.StateSaverAndLoader;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
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
    @Inject(method = "onClose", at = @At("HEAD"))
    private void onChestClosed(PlayerEntity player, CallbackInfo ci) {
        ChestBlockEntity chest = (ChestBlockEntity) (Object) this;
        if (StateSaverAndLoader.isChestStatePresent(player,chest.getPos())){
            List<ItemStack> inventory = new ArrayList<>();
            for (int i = 0; i < chest.size(); i++) {
                inventory.add(chest.getStack(i));
                chest.setStack(i, ItemStack.EMPTY);
            }
            PlayerData playerData = StateSaverAndLoader.getPlayerState(player);
            playerData.addInventory(chest.getPos(), inventory);
            StateSaverAndLoader.saveState(Objects.requireNonNull(player.getServer()));
        }
    }
}
